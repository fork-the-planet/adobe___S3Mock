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
package com.adobe.testing.s3mock.s3.controller

import com.adobe.testing.s3mock.s3.dto.ChecksumMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class ChecksumModeHeaderConverterTest {
  private val iut = ChecksumModeHeaderConverter()

  @ParameterizedTest
  @EnumSource(ChecksumMode::class)
  fun `converts the wire value of every mode back to its enum`(mode: ChecksumMode) {
    assertThat(iut.convert(mode.toString())).isEqualTo(mode)
  }

  @Test
  fun `converts case-insensitively`() {
    assertThat(iut.convert("enabled")).isEqualTo(ChecksumMode.ENABLED)
  }

  @Test
  fun `returns null for an unknown value`() {
    assertThat(iut.convert("NotAMode")).isNull()
  }
}
