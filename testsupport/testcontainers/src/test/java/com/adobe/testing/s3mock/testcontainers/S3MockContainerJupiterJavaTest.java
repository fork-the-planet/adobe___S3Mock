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

package com.adobe.testing.s3mock.testcontainers;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * This shows how to let JUnit 5 Jupiter start and stop the S3MockContainer.
 * Tests are inherited from base class.
 *
 * <p>The container is declared as a static {@code @Container}, so Jupiter starts it once before all
 * tests in the class and stops it after the last test - the single container instance is reused
 * across all test methods. Use a non-static (instance) {@code @Container} field instead if a test
 * needs a fresh container per method.
 */
@Testcontainers
class S3MockContainerJupiterJavaTest extends S3MockContainerJavaTestBase {

  // Container is started once before all tests and stopped after the last test.
  @Container
  private static final S3MockContainer S3_MOCK =
      withDebugLogging(new S3MockContainer(S3MOCK_VERSION)
          .withValidKmsKeys(TEST_ENC_KEYREF)
          .withInitialBuckets(String.join(",", INITIAL_BUCKET_NAMES)));

  @BeforeEach
  void setUp() {
    // Must create S3Client after S3MockContainer is started, otherwise we can't request the random
    // locally mapped port for the endpoint
    var endpoint = S3_MOCK.getHttpsEndpoint();
    s3Client = createS3ClientV2(endpoint);
  }
}
