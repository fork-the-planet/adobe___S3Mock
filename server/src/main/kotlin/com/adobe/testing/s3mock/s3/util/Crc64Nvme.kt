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

import software.amazon.awssdk.checksums.SdkChecksum

/**
 * Pure-JVM implementation of the CRC-64/NVME checksum used by S3 (`x-amz-checksum-crc64nvme`).
 *
 * The AWS SDK `checksums` module has no Java fallback for CRC64NVME; it delegates to the native
 * `software.amazon.awssdk.crt:aws-crt` library, which bundles ~50MB of per-platform native
 * binaries just for this one algorithm. Providing our own [SdkChecksum] here lets S3Mock drop the
 * `aws-crt` dependency entirely.
 *
 * Parameters (CRC-64/NVME, a.k.a. CRC-64/Rocksoft):
 * - width = 64, poly = `0xAD93D23594C935A9`, init = all-ones, refin = refout = true,
 *   xorout = all-ones, check("123456789") = `0xAE8B14860A799888`.
 * - Because this implementation processes reflected input (`refin = true`), the lookup table is
 *   built from the reflected form of the polynomial, `0x9A6C9329AC4BC9B5`.
 *
 * Output byte order matches the SDK/CRT implementation: the 64-bit value is emitted big-endian.
 */
internal class Crc64Nvme : SdkChecksum {
  private var crc = INIT
  private var marked = INIT

  override fun update(b: Int) {
    crc = TABLE[((crc xor b.toLong()) and 0xff).toInt()] xor (crc ushr 8)
  }

  override fun update(
    b: ByteArray,
    off: Int,
    len: Int,
  ) {
    var c = crc
    val end = off + len
    var i = off
    while (i < end) {
      c = TABLE[((c xor b[i].toLong()) and 0xff).toInt()] xor (c ushr 8)
      i++
    }
    crc = c
  }

  override fun getValue(): Long = crc.inv()

  override fun reset() {
    crc = marked
  }

  override fun mark(readLimit: Int) {
    marked = crc
  }

  override fun getChecksumBytes(): ByteArray {
    val value = getValue()
    return ByteArray(Long.SIZE_BYTES) { i -> (value ushr (56 - 8 * i)).toByte() }
  }

  companion object {
    private const val INIT = -1L
    private val POLYNOMIAL = 0x9a6c9329ac4bc9b5uL.toLong()
    private val TABLE =
      LongArray(256) { n ->
        var c = n.toLong()
        repeat(8) {
          c = if (c and 1L != 0L) (c ushr 1) xor POLYNOMIAL else c ushr 1
        }
        c
      }
  }
}
