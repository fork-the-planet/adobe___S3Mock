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

import com.adobe.testing.s3mock.s3.ChecksumTestUtil.prepareInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files

/**
 * Boundary/characterization tests for the read paths shared by the AWS chunked-decoding
 * input streams (implemented in [AbstractAwsInputStream]): single-byte reads, tiny buffers
 * that force short reads across chunk boundaries, interleaved reads, a zero-length request,
 * and an empty payload. The signed stream is used as a representative subclass.
 */
internal class AbstractAwsInputStreamTest {
  @Test
  fun `single-byte reads reproduce the full payload and decoded length`(testInfo: TestInfo) {
    val input = TestUtil.getFileFromClasspath(testInfo, "sampleFile_large.txt")
    val (chunked, decodedLength) = prepareInputStream(input, true, DefaultChecksumAlgorithm.SHA256)

    val iut = AwsChunkedDecodingChecksumInputStream(chunked, decodedLength)
    val decoded = readByteByByte(iut)

    assertThat(decoded).isEqualTo(input.readBytes())
    assertThat(iut.readDecodedLength).isEqualTo(decodedLength)
  }

  @Test
  fun `tiny buffer reads across chunk boundaries reproduce the full payload`(testInfo: TestInfo) {
    val input = TestUtil.getFileFromClasspath(testInfo, "sampleFile_large.txt")
    val (chunked, decodedLength) = prepareInputStream(input, true, DefaultChecksumAlgorithm.SHA256)

    val iut = AwsChunkedDecodingChecksumInputStream(chunked, decodedLength)
    val decoded = readInChunks(iut, bufferSize = 3)

    assertThat(decoded).isEqualTo(input.readBytes())
    assertThat(iut.readDecodedLength).isEqualTo(decodedLength)
  }

  @Test
  fun `interleaving single-byte and bulk reads reproduces the full payload`(testInfo: TestInfo) {
    val input = TestUtil.getFileFromClasspath(testInfo, "sampleFile_large.txt")
    val (chunked, decodedLength) = prepareInputStream(input, true, DefaultChecksumAlgorithm.SHA256)

    val iut = AwsChunkedDecodingChecksumInputStream(chunked, decodedLength)
    val out = ByteArrayOutputStream()

    // Read the first few bytes one at a time, then drain the rest in bulk.
    repeat(5) {
      val c = iut.read()
      assertThat(c).isNotNegative()
      out.write(c)
    }
    iut.copyTo(out)

    assertThat(out.toByteArray()).isEqualTo(input.readBytes())
    assertThat(iut.readDecodedLength).isEqualTo(decodedLength)
  }

  @Test
  fun `a zero-length bulk read returns zero without consuming the stream`(testInfo: TestInfo) {
    val input = TestUtil.getFileFromClasspath(testInfo, "sampleFile.txt")
    val (chunked, decodedLength) = prepareInputStream(input, true, DefaultChecksumAlgorithm.SHA256)

    val iut = AwsChunkedDecodingChecksumInputStream(chunked, decodedLength)
    assertThat(iut.read(ByteArray(4), 0, 0)).isZero()

    // The stream is still fully readable afterwards.
    assertThat(readByteByByte(iut)).isEqualTo(input.readBytes())
  }

  @Test
  fun `an empty payload decodes to no bytes`() {
    val empty = Files.createTempFile("AbstractAwsInputStreamBoundaryTest", "empty").toFile()
    empty.deleteOnExit()
    val (chunked, decodedLength) = prepareInputStream(empty, true, DefaultChecksumAlgorithm.SHA256)

    val iut = AwsChunkedDecodingChecksumInputStream(chunked, decodedLength)

    assertThat(decodedLength).isZero()
    assertThat(readByteByByte(iut)).isEmpty()
    assertThat(iut.readDecodedLength).isZero()
  }

  private fun readByteByByte(input: InputStream): ByteArray =
    ByteArrayOutputStream().use { out ->
      while (true) {
        val c = input.read()
        if (c < 0) break
        out.write(c)
      }
      out.toByteArray()
    }

  private fun readInChunks(
    input: InputStream,
    bufferSize: Int,
  ): ByteArray =
    ByteArrayOutputStream().use { out ->
      val buffer = ByteArray(bufferSize)
      while (true) {
        val read = input.read(buffer, 0, bufferSize)
        if (read < 0) break
        out.write(buffer, 0, read)
      }
      out.toByteArray()
    }
}
