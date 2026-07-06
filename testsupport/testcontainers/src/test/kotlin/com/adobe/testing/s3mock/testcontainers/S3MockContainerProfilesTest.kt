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

/**
 * Verifies that Spring-profile configuration composes instead of overwriting. These tests only
 * inspect the configured env vars and never start the container, so they need no Docker.
 */
internal class S3MockContainerProfilesTest {
  @Test
  fun `withVectors and withDebug compose into a single SPRING_PROFILES_ACTIVE value`() {
    val container = S3MockContainer("latest").withVectors().withDebug()

    assertThat(container.envMap).containsEntry("SPRING_PROFILES_ACTIVE", "vectors,debug")
  }

  @Test
  fun `withSpringProfiles preserves order and ignores duplicates`() {
    val container =
      S3MockContainer("latest")
        .withVectors()
        .withSpringProfiles("debug", "vectors")

    assertThat(container.envMap).containsEntry("SPRING_PROFILES_ACTIVE", "vectors,debug")
  }

  @Test
  fun `withVectors alone activates only the vectors profile`() {
    val container = S3MockContainer("latest").withVectors()

    assertThat(container.envMap).containsEntry("SPRING_PROFILES_ACTIVE", "vectors")
  }
}
