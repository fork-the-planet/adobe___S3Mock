# Testing Strategy — S3Mock

## Testing Philosophy

S3Mock's test suite verifies two things: that the internal logic is correct (unit tests) and that the HTTP/XML contract matches the real AWS S3 API (integration tests). A passing unit test suite does not guarantee a correct S3 implementation — always verify with integration tests before claiming a feature works.

## Critical Paths

These paths have the highest risk of regressions. Always cover them when modifying the relevant code:

| Path | Why it matters |
|---|---|
| PUT / GET / DELETE object round-trip | Core storage correctness |
| ETag computation (single-part and multipart) | Clients depend on ETags for caching and integrity checks |
| Versioning: PUT, GET, DELETE with `?versionId=` | Version ID mismatches cause silent data loss |
| Multipart: initiate → upload parts → complete | Part ETag format must match S3's `{md5}-{n}` pattern |
| KMS header validation | `InvalidArgument` must be returned for unknown ARNs |
| Error responses: `NoSuchBucket`, `NoSuchKey`, 409 on existing bucket | Error code and HTTP status must match AWS exactly |

## Mocking & Fixtures

- In **unit tests** (`*Test.kt`): mock store and service dependencies with `@MockitoBean`; never mock the class under test
- In **integration tests** (`*IT.kt`): never mock AWS SDK clients — use the real `s3Client` from `S3TestBase` against a live container
- Use `givenBucket(testInfo)` for bucket names — it derives a unique name from the test method, preventing cross-test pollution
- For test objects, prefer `RequestBody.fromString("content")` over files for unit tests; use real byte arrays for checksum or content-length tests

## Test Types

| Type | Location | Suffix | Purpose |
|------|----------|--------|---------|
| Unit tests | `server/src/test/kotlin/` | `*Test.kt` | Service, store, and controller logic in isolation |
| Integration tests | `integration-tests/src/test/kotlin/.../its/` | `*IT.kt` | Real AWS SDK v2 against a live Docker container |

## Unit Tests

Use `@SpringBootTest` with `@MockitoBean` for mocking. Extend the appropriate base class:

| Base Class | Use For |
|---|---|
| `ServiceTestBase` | Service-layer tests |
| `StoreTestBase` | Store-layer tests |
| `BaseControllerTest` | Controller slice tests (`@WebMvcTest`) |

**Which harness to use — decide by whether the class is a Spring bean:**

| Class under test | Harness | Base class |
|---|---|---|
| Service, store, filter, `@ControllerAdvice` (Spring bean with injected collaborators) | `@SpringBootTest` + `@MockitoBean` | `ServiceTestBase` / `StoreTestBase` |
| Controller (`@RestController`) | `@WebMvcTest` + `@MockitoBean` | `BaseControllerTest` |
| Plain class with no injected collaborators (most `dto`/`model`/`util`) | plain unit test | none |

Standard Mockito unit-test style (`@ExtendWith(MockitoExtension)` + `@Mock` + `@InjectMocks`) is **banned for Spring-managed components** — it bypasses the Spring context and bean wiring. This is enforced by the `noStandardMockitoUnitTests` ArchUnit rule (`make test`); see [INVARIANTS.md](../INVARIANTS.md). Collaborator stubbing still uses mockito-kotlin (`mock()` / `whenever(...)`), which does not rely on those annotations.

Name the class under test **`iut`** (implementation under test); inject with `@Autowired`:

```kotlin
@SpringBootTest(classes = [ServiceConfiguration::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockitoBean(types = [BucketService::class, MultipartService::class, MultipartStore::class])
internal class ObjectServiceTest : ServiceTestBase() {
  @Autowired
  private lateinit var iut: ObjectService

  @Test
  fun `should get object`() {
    whenever(bucketStore.getBucketMetadata("bucket")).thenReturn(bucket)
    whenever(objectStore.getObject(bucket, "key")).thenReturn(s3Object)
    assertThat(iut.getObject("bucket", "key")).isEqualTo(s3Object)
  }
}
```

## Integration Tests

Extend `S3TestBase` for a pre-configured `s3Client` (AWS SDK v2). Accept `TestInfo` as a method parameter and use `givenBucket(testInfo)` for unique bucket names:

```kotlin
internal class MyFeatureIT : S3TestBase() {
  @Test
  fun `should perform operation`(testInfo: TestInfo) {
    // Arrange
    val bucketName = givenBucket(testInfo)

    // Act
    s3Client.putObject(
      PutObjectRequest.builder().bucket(bucketName).key("key").build(),
      RequestBody.fromString("content")
    )

    // Assert
    val response = s3Client.getObject(
      GetObjectRequest.builder().bucket(bucketName).key("key").build()
    )
    assertThat(response.readAllBytes().decodeToString()).isEqualTo("content")
  }
}
```

Access `serviceEndpoint`, `serviceEndpointHttp`, and `serviceEndpointHttps` from `S3TestBase` when needed.

## Conventions

See **[docs/KOTLIN.md](KOTLIN.md)** for Kotlin naming conventions (backtick test names, `internal` visibility, naming patterns).

- **Pattern**: Arrange-Act-Assert
- **Assertions**: AssertJ (`assertThat(...)`) — use specific matchers, not just `isNotNull()`
- **Error cases**: Use AssertJ, not JUnit `assertThrows`:
  ```kotlin
  assertThatThrownBy { s3Client.deleteBucket { it.bucket(bucketName) } }
    .isInstanceOf(AwsServiceException::class.java)
    .hasMessageContaining("Status Code: 409")
  ```
- **Independence**: Each test creates its own resources — no shared state, UUID-based bucket names

## Running Tests

```bash
make test                                                                    # Unit tests only
make integration-tests                                                       # All integration tests
make integration-test-class CLASS=BucketIT                                  # Specific class
make integration-test-class CLASS=BucketIT#shouldCreateBucket               # Specific method
```

> Integration tests require Docker to be running.

Each test class runs against its own S3Mock container (started by Testcontainers). By default the
container logs are not forwarded, keeping the build log readable. Two independent flags control
container diagnostics (both default to off):

- `-Ds3mock.log=true` — forward each container's output to the build log (INFO level).
- `-Ds3mock.debug=true` — activate the server's `debug` Spring profile (debug-level logging).

Combine them for debug-level output in the build log:

```bash
./mvnw -B verify -pl integration-tests -Ds3mock.log=true -Ds3mock.debug=true
```

The same flags apply to the `testsupport/testcontainers` module tests.

### Running against a real S3 endpoint

To run the integration tests against an external endpoint (e.g. the real AWS S3 API) directly from
the IDE instead of a Testcontainer, set `it.s3mock.endpoint`. When it is set, S3 traffic uses the
given endpoint directly, so **no Testcontainer is started for S3 operations**. Vector operations are
fully external only when `it.s3mock.vectors.endpoint` is also set; otherwise vector helpers fall
back to the container-backed `s3Mock.vectors*Endpoint` accessors and may still start the
Testcontainer. Use the following overrides:

| System property | Purpose | Default |
|---|---|---|
| `it.s3mock.endpoint` | S3 endpoint; also switches on real-backend mode | — (uses container) |
| `it.s3mock.vectors.endpoint` | S3 Vectors endpoint | — (uses container) |
| `it.s3mock.access.key.id` | AWS access key id | `foo` |
| `it.s3mock.secret.access.key` | AWS secret access key | `bar` |
| `it.s3mock.region` | AWS region | `us-east-1` |

Tests annotated `@S3VerifiedFailure` are automatically disabled in this mode (see
`RealS3BackendUsedCondition`). Example:

```bash
./mvnw -B verify -pl integration-tests \
  -Dit.s3mock.endpoint=https://s3.eu-west-1.amazonaws.com \
  -Dit.s3mock.region=eu-west-1 \
  -Dit.s3mock.access.key.id=AKIA... -Dit.s3mock.secret.access.key=...
```

## Troubleshooting

- **Docker not running**: Run `docker info` — if it fails, Docker is not running; escalate to the human rather than debugging the test failure
- **Port conflict**: Check `lsof -i :9090`
- **Flaky test**: Look for shared state or ordering dependencies
- **Compilation error**: Run `make typecheck` first

## Checklist

- [ ] Verify tests pass locally
- [ ] Cover both success and failure cases
- [ ] Keep tests independent (no shared state, UUID bucket names)
- [ ] Use specific assertions
- [ ] Run `make format`
