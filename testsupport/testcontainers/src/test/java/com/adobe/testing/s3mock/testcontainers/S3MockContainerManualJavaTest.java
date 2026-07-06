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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 * This tests / shows how to manually start and stop the S3MockContainer.
 * Tests are inherited from base class.
 *
 * <p>The container is started once in {@code setUp} and stopped in {@code tearDown}, so the single
 * instance is reused across all test methods in this class. {@link TestInstance.Lifecycle#PER_CLASS}
 * lets the {@code @BeforeAll} / {@code @AfterAll} methods be non-static and share instance state.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class S3MockContainerManualJavaTest extends S3MockContainerJavaTestBase {
  private S3MockContainer s3Mock;

  @BeforeAll
  void setUp() {
    s3Mock = withDebugLogging(new S3MockContainer(S3MOCK_VERSION)
            .withValidKmsKeys(TEST_ENC_KEYREF)
            .withInitialBuckets(String.join(",", INITIAL_BUCKET_NAMES)));
    s3Mock.start();
    // Must create S3Client after S3MockContainer is started, otherwise we can't request the random
    // locally mapped port for the endpoint
    var endpoint = s3Mock.getHttpsEndpoint();
    s3Client = createS3ClientV2(endpoint);
  }

  @AfterAll
  void tearDown() {
    s3Mock.stop();
  }
}
