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
package com.adobe.testing.s3mock.testcontainers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3vectors.S3VectorsClient
import software.amazon.awssdk.utils.AttributeMap
import java.net.URI

/**
 * Verifies that [S3MockContainer.withVectors] activates the `vectors` Spring profile and that the
 * S3 Vectors API is reachable via the vectors endpoint accessors.
 */
@Testcontainers
internal class S3MockContainerVectorsTest {
  @Container
  private val s3Mock: S3MockContainer =
    S3MockContainer(System.getProperty("s3mock.version", "latest"))
      .withVectors()
      .withDebugLogging()

  @Test
  fun vectorsApiIsReachableWhenVectorsProfileActive() {
    createS3VectorsClient(s3Mock.vectorsHttpEndpoint).use { client ->
      val bucketName = "vector-bucket-${System.currentTimeMillis()}"
      client.createVectorBucket { it.vectorBucketName(bucketName) }

      val buckets =
        client.listVectorBuckets { }.vectorBuckets().map { it.vectorBucketName() }
      assertThat(buckets).contains(bucketName)
    }
  }

  private fun createS3VectorsClient(endpoint: String): S3VectorsClient =
    S3VectorsClient
      .builder()
      .region(Region.of("us-east-1"))
      .credentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")),
      ).endpointOverride(URI.create(endpoint))
      .httpClient(
        UrlConnectionHttpClient.builder().buildWithDefaults(
          AttributeMap
            .builder()
            .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
            .build(),
        ),
      ).build()
}
