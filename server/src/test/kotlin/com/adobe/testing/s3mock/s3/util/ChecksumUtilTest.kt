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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.io.TempDir
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm
import software.amazon.awssdk.utils.BinaryUtils
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.writeBytes

internal class ChecksumUtilTest {
  @Test
  fun testCrc64Nvme(
    @TempDir tempDir: Path,
  ) {
    val file = tempDir.resolve("crc64nvme.bin")
    file.writeBytes("123456789".toByteArray(Charsets.UTF_8))

    // Standard CRC-64/NVME check value for "123456789", base64-encoded big-endian.
    assertThat(ChecksumUtil.checksumFor(file, DefaultChecksumAlgorithm.CRC64NVME))
      .isEqualTo("rosUhgp5mIg=")
  }

  @Test
  fun testChecksumOfMultipleFiles(testInfo: TestInfo) {
    val sha256 = MessageDigest.getInstance("SHA-256")

    // yes, this is correct - AWS calculates a Multipart digest by calculating the digest of every
    // file involved, and then calculates the digest on the result.
    // a hyphen with the part count is added as a suffix.
    val expected = "${
      BinaryUtils.toBase64(
        sha256.digest(
          sha256.digest("Part1".toByteArray()) + // testFile1
            sha256.digest("Part2".toByteArray()), // testFile2
        ),
      )
    }-2"

    // files contain the exact content seen above
    val files =
      listOf(
        TestUtil.getTestFile(testInfo, "testFile1").toPath(),
        TestUtil.getTestFile(testInfo, "testFile2").toPath(),
      )

    assertThat(ChecksumUtil.checksumMultipart(files, DefaultChecksumAlgorithm.SHA256)).isEqualTo(expected)
  }
}
