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
import com.adobe.testing.s3mock.common.S3Verified
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm

/**
 * Returns [checksum] if this algorithm matches [target], otherwise null.
 * Use this to route a single checksum value to the correct per-algorithm field in DTOs.
 */
fun ChecksumAlgorithm?.ifAlgorithm(
  target: ChecksumAlgorithm,
  checksum: String?,
): String? = if (this == target) checksum else null

@S3Verified(year = 2025)
enum class ChecksumAlgorithm(
  val headerName: String,
) {
  CRC32(X_AMZ_CHECKSUM_CRC32),
  CRC32C(X_AMZ_CHECKSUM_CRC32C),
  CRC64NVME(X_AMZ_CHECKSUM_CRC64NVME),
  SHA1(X_AMZ_CHECKSUM_SHA1),
  SHA256(X_AMZ_CHECKSUM_SHA256),
  ;

  fun toChecksumAlgorithm(): software.amazon.awssdk.checksums.spi.ChecksumAlgorithm =
    when (this) {
      CRC32 -> DefaultChecksumAlgorithm.CRC32
      CRC32C -> DefaultChecksumAlgorithm.CRC32C
      CRC64NVME -> DefaultChecksumAlgorithm.CRC64NVME
      SHA1 -> DefaultChecksumAlgorithm.SHA1
      SHA256 -> DefaultChecksumAlgorithm.SHA256
    }

  @JsonValue
  override fun toString(): String = name

  companion object {
    @JsonCreator
    fun fromString(value: String?): ChecksumAlgorithm? = enumFromName<ChecksumAlgorithm>(value)

    fun fromHeader(value: String?): ChecksumAlgorithm? = entries.firstOrNull { it.headerName == value }
  }
}
