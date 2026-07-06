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
import software.amazon.awssdk.utils.BinaryUtils

internal class Crc64NvmeTest {
  @Test
  fun testReferenceCheckValue() {
    // Standard CRC-64/NVME check value for the ASCII string "123456789".
    val crc = Crc64Nvme()
    val data = "123456789".toByteArray(Charsets.UTF_8)
    crc.update(data, 0, data.size)

    assertThat(crc.value).isEqualTo(-0x5174eb79f5866778L) // 0xAE8B14860A799888
    assertThat(BinaryUtils.toBase64(crc.checksumBytes)).isEqualTo("rosUhgp5mIg=")
    assertThat(crc.checksumBytes).hasSize(8)
  }

  @Test
  fun testEmptyInput() {
    assertThat(Crc64Nvme().value).isZero()
  }

  @Test
  fun testUpdateSingleBytesMatchesBulk() {
    val data = "The quick brown fox jumps over the lazy dog".toByteArray(Charsets.UTF_8)

    val bulk = Crc64Nvme().apply { update(data, 0, data.size) }

    val byByte =
      Crc64Nvme().apply {
        for (b in data) {
          update(b.toInt())
        }
      }

    assertThat(byByte.value).isEqualTo(bulk.value)
  }

  @Test
  fun testResetReturnsToInitialState() {
    val crc = Crc64Nvme()
    val data = "payload".toByteArray(Charsets.UTF_8)
    crc.update(data, 0, data.size)
    crc.reset()

    assertThat(crc.value).isZero()
  }

  @Test
  fun testMarkAndResetRestoreMarkedState() {
    val prefix = "abc".toByteArray(Charsets.UTF_8)
    val suffix = "def".toByteArray(Charsets.UTF_8)

    val marked =
      Crc64Nvme().apply {
        update(prefix, 0, prefix.size)
        mark(0)
        update(suffix, 0, suffix.size)
        reset()
      }

    val prefixOnly = Crc64Nvme().apply { update(prefix, 0, prefix.size) }

    assertThat(marked.value).isEqualTo(prefixOnly.value)
  }
}
