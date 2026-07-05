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

import com.adobe.testing.s3mock.s3.S3Exception
import com.adobe.testing.s3mock.s3.dto.ChecksumAlgorithm
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm
import software.amazon.awssdk.checksums.SdkChecksum
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.Base64
import java.util.zip.CheckedInputStream
import kotlin.io.path.inputStream
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm as SdkChecksumAlgorithm

/**
 * Utility class for AWS SDK checksum operations.
 * Handles checksum verification and calculation using AWS SDK algorithms.
 * For MD5/ETag digest operations, see [DigestUtil].
 */
object ChecksumUtil {
  private const val CHECKSUM_COULD_NOT_BE_CALCULATED = "Checksum could not be calculated."

  fun verifyChecksum(
    expected: String,
    actual: String?,
    checksumAlgorithm: ChecksumAlgorithm,
  ) {
    if (expected != actual) {
      when (checksumAlgorithm) {
        ChecksumAlgorithm.SHA1 -> throw S3Exception.BAD_CHECKSUM_SHA1
        ChecksumAlgorithm.SHA256 -> throw S3Exception.BAD_CHECKSUM_SHA256
        ChecksumAlgorithm.CRC32 -> throw S3Exception.BAD_CHECKSUM_CRC32
        ChecksumAlgorithm.CRC32C -> throw S3Exception.BAD_CHECKSUM_CRC32C
        ChecksumAlgorithm.CRC64NVME -> throw S3Exception.BAD_CHECKSUM_CRC64NVME
      }
    }
  }

  /**
   * Calculate a checksum for the given path and algorithm.
   *
   * @param path Path containing the bytes to generate the checksum for
   * @param algorithm algorithm to use
   * @return the checksum
   */
  fun checksumFor(
    path: Path,
    algorithm: SdkChecksumAlgorithm,
  ): String {
    try {
      path.inputStream().use {
        return checksumFor(it, algorithm)
      }
    } catch (e: IOException) {
      throw IllegalStateException(CHECKSUM_COULD_NOT_BE_CALCULATED, e)
    }
  }

  /**
   * Calculate a checksum for the given inputstream and algorithm.
   *
   * @param stream InputStream containing the bytes to generate the checksum for
   * @param algorithm algorithm to use
   * @return the checksum
   */
  private fun checksumFor(
    stream: InputStream,
    algorithm: SdkChecksumAlgorithm,
  ): String = Base64.getEncoder().encodeToString(checksum(stream, algorithm))

  /**
   * Calculate a checksum for the given inputstream and algorithm.
   *
   * @param stream InputStream containing the bytes to generate the checksum for
   * @param algorithm algorithm to use
   * @return the checksum
   */
  private fun checksum(
    stream: InputStream,
    algorithm: SdkChecksumAlgorithm,
  ): ByteArray {
    val sdkChecksum = sdkChecksumFor(algorithm)
    try {
      CheckedInputStream(stream, sdkChecksum).copyTo(OutputStream.nullOutputStream())
      return sdkChecksum.checksumBytes
    } catch (e: IOException) {
      throw IllegalStateException(CHECKSUM_COULD_NOT_BE_CALCULATED, e)
    }
  }

  private fun checksum(
    paths: List<Path>,
    algorithm: SdkChecksumAlgorithm,
  ): ByteArray {
    val sdkChecksum = sdkChecksumFor(algorithm)
    val allChecksums = ByteArrayOutputStream()
    for (path in paths) {
      try {
        path.inputStream().use {
          allChecksums.write(checksum(it, algorithm))
        }
      } catch (e: IOException) {
        throw IllegalStateException("Could not read from path $path", e)
      }
    }
    sdkChecksum.update(allChecksums.toByteArray(), 0, allChecksums.size())
    return sdkChecksum.checksumBytes
  }

  /**
   * Calculates the checksum for a list of paths.
   * For multipart uploads, AWS takes the checksum of all parts, concatenates them, and then takes
   * the checksum again. Then, they add a hyphen and the number of parts used to calculate the
   * checksum.
   * [API](https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html)
   */
  fun checksumMultipart(
    paths: List<Path>,
    algorithm: SdkChecksumAlgorithm,
  ): String = "${Base64.getEncoder().encodeToString(checksum(paths, algorithm))}-${paths.size}"

  /**
   * Returns the [SdkChecksum] for the given algorithm.
   *
   * CRC64NVME is served by our own pure-JVM [Crc64Nvme] so we don't need the native
   * `aws-crt` library; all other algorithms have a Java implementation in the SDK
   * `checksums` module and are delegated to [SdkChecksum.forAlgorithm].
   */
  private fun sdkChecksumFor(algorithm: SdkChecksumAlgorithm): SdkChecksum =
    if (algorithm.algorithmId() == DefaultChecksumAlgorithm.CRC64NVME.algorithmId()) {
      Crc64Nvme()
    } else {
      SdkChecksum.forAlgorithm(algorithm)
    }
}
