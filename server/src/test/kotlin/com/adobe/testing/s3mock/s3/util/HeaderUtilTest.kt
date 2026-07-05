/*
 *  Copyright 2017-2026 Adobe.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.testing.s3mock.s3.util

import com.adobe.testing.s3mock.s3.dto.ChecksumAlgorithm
import com.adobe.testing.s3mock.s3.dto.ChecksumType
import com.adobe.testing.s3mock.s3.dto.Owner
import com.adobe.testing.s3mock.s3.dto.StorageClass
import com.adobe.testing.s3mock.s3.model.S3ObjectMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpHeaders
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

internal class HeaderUtilTest {
  @ParameterizedTest
  @EnumSource(ChecksumAlgorithm::class)
  fun `checksumAlgorithmFromHeader detects the per-algorithm checksum header`(algorithm: ChecksumAlgorithm) {
    val httpHeaders = HttpHeaders().apply { add(algorithm.headerName, "checksum-value") }

    assertThat(HeaderUtil.checksumAlgorithmFromHeader(httpHeaders)).isEqualTo(algorithm)
  }

  @ParameterizedTest
  @EnumSource(ChecksumAlgorithm::class)
  fun `checksumFrom returns the value of the present per-algorithm checksum header`(algorithm: ChecksumAlgorithm) {
    val httpHeaders = HttpHeaders().apply { add(algorithm.headerName, "checksum-value") }

    assertThat(HeaderUtil.checksumFrom(httpHeaders)).isEqualTo("checksum-value")
  }

  @Test
  fun `checksumAlgorithmFromHeader falls back to the algorithm header`() {
    val httpHeaders = HttpHeaders().apply { add("x-amz-checksum-algorithm", "SHA256") }

    assertThat(HeaderUtil.checksumAlgorithmFromHeader(httpHeaders)).isEqualTo(ChecksumAlgorithm.SHA256)
  }

  @Test
  fun `checksumAlgorithmFromHeader and checksumFrom return null when no checksum header is present`() {
    val httpHeaders = HttpHeaders()

    assertThat(HeaderUtil.checksumAlgorithmFromHeader(httpHeaders)).isNull()
    assertThat(HeaderUtil.checksumFrom(httpHeaders)).isNull()
  }

  @ParameterizedTest
  @EnumSource(ChecksumAlgorithm::class)
  fun `checksumHeaderFrom maps every algorithm back to its header`(algorithm: ChecksumAlgorithm) {
    val headers = HeaderUtil.checksumHeaderFrom("checksum-value", algorithm)

    assertThat(headers).containsExactly(entry(algorithm.headerName, "checksum-value"))
  }

  @Test
  fun testGetUserMetadata_canonical() {
    val httpHeaders = HttpHeaders().apply { add(X_AMZ_CANONICAL_HEADER, TEST_VALUE) }

    val userMetadata = HeaderUtil.userMetadataFrom(httpHeaders)
    assertThat(userMetadata).containsEntry(X_AMZ_CANONICAL_HEADER, TEST_VALUE)
  }

  @Test
  fun testGetUserMetadata_javaSdk() {
    val httpHeaders = HttpHeaders().apply { add(X_AMZ_LOWERCASE_HEADER, TEST_VALUE) }

    val userMetadata = HeaderUtil.userMetadataFrom(httpHeaders)
    assertThat(userMetadata).containsEntry(X_AMZ_LOWERCASE_HEADER, TEST_VALUE)
  }

  @Test
  fun testCreateUserMetadata_canonical() {
    val userMetadata = mapOf(X_AMZ_CANONICAL_HEADER to TEST_VALUE)
    val s3ObjectMetadata = s3ObjectMetadata(userMetadata = userMetadata)

    val userMetadataHeaders = s3ObjectMetadata.userMetadataHeaders()
    assertThat(userMetadataHeaders).containsEntry(X_AMZ_CANONICAL_HEADER, TEST_VALUE)
  }

  @Test
  fun testCreateUserMetadata_javaSdk() {
    val userMetadata = mapOf(X_AMZ_LOWERCASE_HEADER to TEST_VALUE)
    val s3ObjectMetadata = s3ObjectMetadata(userMetadata = userMetadata)

    val userMetadataHeaders = s3ObjectMetadata.userMetadataHeaders()
    assertThat(userMetadataHeaders).containsEntry(X_AMZ_LOWERCASE_HEADER, TEST_VALUE)
  }

  private fun s3ObjectMetadata(
    id: UUID = UUID.randomUUID(),
    key: String = "key",
    userMetadata: Map<String, String>? = null,
  ): S3ObjectMetadata =
    S3ObjectMetadata(
      id,
      key,
      "size",
      "lastModified",
      "\"etag\"",
      null,
      Instant.now().toEpochMilli(),
      Path.of("test"),
      userMetadata,
      null,
      null,
      null,
      Owner(0L.toString()),
      null,
      null,
      ChecksumAlgorithm.SHA256,
      null,
      StorageClass.STANDARD,
      null,
      null,
      false,
      ChecksumType.FULL_OBJECT,
    )

  companion object {
    private const val X_AMZ_CANONICAL_HEADER = "X-Amz-Meta-Some-header"
    private const val X_AMZ_LOWERCASE_HEADER = "x-amz-meta-Some-header"
    private const val TEST_VALUE = "test-value"
  }
}
