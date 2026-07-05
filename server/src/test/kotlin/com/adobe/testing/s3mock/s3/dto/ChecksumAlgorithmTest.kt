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
package com.adobe.testing.s3mock.s3.dto

import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_CHECKSUM_CRC32
import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_CHECKSUM_CRC32C
import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_CHECKSUM_CRC64NVME
import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_CHECKSUM_SHA1
import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_CHECKSUM_SHA256
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class ChecksumAlgorithmTest {
  @ParameterizedTest
  @EnumSource(ChecksumAlgorithm::class)
  fun `fromHeader is the inverse of headerName for every algorithm`(algorithm: ChecksumAlgorithm) {
    assertThat(ChecksumAlgorithm.fromHeader(algorithm.headerName)).isEqualTo(algorithm)
  }

  @Test
  fun `headerName matches the AWS wire header for every algorithm`() {
    assertThat(ChecksumAlgorithm.CRC32.headerName).isEqualTo(X_AMZ_CHECKSUM_CRC32)
    assertThat(ChecksumAlgorithm.CRC32C.headerName).isEqualTo(X_AMZ_CHECKSUM_CRC32C)
    assertThat(ChecksumAlgorithm.CRC64NVME.headerName).isEqualTo(X_AMZ_CHECKSUM_CRC64NVME)
    assertThat(ChecksumAlgorithm.SHA1.headerName).isEqualTo(X_AMZ_CHECKSUM_SHA1)
    assertThat(ChecksumAlgorithm.SHA256.headerName).isEqualTo(X_AMZ_CHECKSUM_SHA256)
  }

  @Test
  fun `fromHeader returns null for unknown or null header names`() {
    assertThat(ChecksumAlgorithm.fromHeader(null)).isNull()
    assertThat(ChecksumAlgorithm.fromHeader("x-amz-checksum-unknown")).isNull()
    assertThat(ChecksumAlgorithm.fromHeader("")).isNull()
  }
}
