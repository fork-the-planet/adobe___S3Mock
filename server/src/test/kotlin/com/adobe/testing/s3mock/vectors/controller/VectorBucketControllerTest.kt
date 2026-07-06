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
package com.adobe.testing.s3mock.vectors.controller

import com.adobe.testing.s3mock.vectors.S3VectorsException
import com.adobe.testing.s3mock.vectors.dto.CreateVectorBucketResponse
import com.adobe.testing.s3mock.vectors.dto.GetVectorBucketResponse
import com.adobe.testing.s3mock.vectors.dto.ListVectorBucketsResponse
import com.adobe.testing.s3mock.vectors.dto.VectorBucket
import com.adobe.testing.s3mock.vectors.dto.VectorBucketSummary
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal class VectorBucketControllerTest : VectorBaseControllerTest() {
  @Test
  fun `CreateVectorBucket returns 200 with ARN`() {
    whenever(vectorBucketService.createVectorBucket("my-bucket", null, emptyMap()))
      .thenReturn(CreateVectorBucketResponse("arn:aws:s3vectors:us-east-1:123:bucket/my-bucket"))

    mockMvc
      .perform(
        post("/CreateVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"my-bucket"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.vectorBucketArn").value("arn:aws:s3vectors:us-east-1:123:bucket/my-bucket"))
  }

  @Test
  fun `CreateVectorBucket with missing name returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/CreateVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `CreateVectorBucket maps a conflict to 409 ConflictException`() {
    whenever(vectorBucketService.createVectorBucket("existing", null, emptyMap()))
      .thenThrow(S3VectorsException.VECTOR_BUCKET_ALREADY_EXISTS)

    mockMvc
      .perform(
        post("/CreateVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"existing"}"""),
      ).andExpect(status().isConflict)
      .andExpect(header().string("x-amzn-errortype", "ConflictException"))
  }

  @Test
  fun `GetVectorBucket returns 200 with bucket details`() {
    val bucket = VectorBucket("arn:aws:s3vectors:us-east-1:123:bucket/b", "b", 1.0, null)
    whenever(vectorBucketService.getVectorBucket("b")).thenReturn(GetVectorBucketResponse(bucket))

    mockMvc
      .perform(
        post("/GetVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"b"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.vectorBucket.vectorBucketName").value("b"))
  }

  @Test
  fun `GetVectorBucket maps a missing bucket to 404 NotFoundException`() {
    whenever(vectorBucketService.getVectorBucket("missing"))
      .thenThrow(S3VectorsException.VECTOR_BUCKET_NOT_FOUND)

    mockMvc
      .perform(
        post("/GetVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"missing"}"""),
      ).andExpect(status().isNotFound)
      .andExpect(header().string("x-amzn-errortype", "NotFoundException"))
  }

  @Test
  fun `ListVectorBuckets returns 200 with summaries`() {
    val summaries =
      listOf(
        VectorBucketSummary("arn1", "b1", 1.0),
        VectorBucketSummary("arn2", "b2", 2.0),
      )
    whenever(vectorBucketService.listVectorBuckets(null, null, null))
      .thenReturn(ListVectorBucketsResponse(summaries, null))

    mockMvc
      .perform(
        post("/ListVectorBuckets")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.vectorBuckets.length()").value(2))
  }

  @Test
  fun `DeleteVectorBucket returns 200 on success`() {
    mockMvc
      .perform(
        post("/DeleteVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"b"}"""),
      ).andExpect(status().isOk)

    verify(vectorBucketService).deleteVectorBucket("b")
  }

  @Test
  fun `DeleteVectorBucket maps a non-empty bucket to 409 ConflictException`() {
    whenever(vectorBucketService.deleteVectorBucket("full"))
      .thenThrow(S3VectorsException.VECTOR_BUCKET_NOT_EMPTY)

    mockMvc
      .perform(
        post("/DeleteVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"full"}"""),
      ).andExpect(status().isConflict)
      .andExpect(header().string("x-amzn-errortype", "ConflictException"))
  }

  @Test
  fun `DeleteVectorBucket with missing name returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/DeleteVectorBucket")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }
}
