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
import com.adobe.testing.s3mock.vectors.dto.CreateIndexResponse
import com.adobe.testing.s3mock.vectors.dto.GetIndexResponse
import com.adobe.testing.s3mock.vectors.dto.IndexSummary
import com.adobe.testing.s3mock.vectors.dto.ListIndexesResponse
import com.adobe.testing.s3mock.vectors.dto.VectorIndex
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal class VectorIndexControllerTest : VectorBaseControllerTest() {
  @Test
  fun `CreateIndex returns 200 with index ARN`() {
    whenever(vectorIndexService.createIndex("b", "i", "float32", 128, "cosine", null, null, emptyMap()))
      .thenReturn(CreateIndexResponse("arn:aws:s3vectors:us-east-1:123:bucket/b/index/i"))

    mockMvc
      .perform(
        post("/CreateIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"i","dataType":"float32","dimension":128,"distanceMetric":"cosine","vectorBucketName":"b"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.indexArn").value("arn:aws:s3vectors:us-east-1:123:bucket/b/index/i"))
  }

  @Test
  fun `CreateIndex with missing bucket returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/CreateIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"i","dataType":"float32","dimension":4,"distanceMetric":"cosine"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `CreateIndex maps a conflict to 409 ConflictException`() {
    whenever(vectorIndexService.createIndex("b", "i", "float32", 4, "cosine", null, null, emptyMap()))
      .thenThrow(S3VectorsException.INDEX_ALREADY_EXISTS)

    mockMvc
      .perform(
        post("/CreateIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"i","dataType":"float32","dimension":4,"distanceMetric":"cosine","vectorBucketName":"b"}"""),
      ).andExpect(status().isConflict)
      .andExpect(header().string("x-amzn-errortype", "ConflictException"))
  }

  @Test
  fun `GetIndex returns 200 with index details`() {
    val index = VectorIndex("arn1", "i", "b", "float32", 128, "cosine", 1.0, null, null)
    whenever(vectorIndexService.getIndex("b", "i")).thenReturn(GetIndexResponse(index))

    mockMvc
      .perform(
        post("/GetIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"i","vectorBucketName":"b"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.index.indexName").value("i"))
  }

  @Test
  fun `GetIndex maps a missing index to 404 NotFoundException`() {
    whenever(vectorIndexService.getIndex("b", "ghost")).thenThrow(S3VectorsException.INDEX_NOT_FOUND)

    mockMvc
      .perform(
        post("/GetIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"ghost","vectorBucketName":"b"}"""),
      ).andExpect(status().isNotFound)
      .andExpect(header().string("x-amzn-errortype", "NotFoundException"))
  }

  @Test
  fun `GetIndex without indexName or indexArn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/GetIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"b"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `ListIndexes returns 200 with index summaries`() {
    val summaries =
      listOf(
        IndexSummary("arn1", "idx-a", "b", 1.0),
        IndexSummary("arn2", "idx-b", "b", 2.0),
      )
    whenever(vectorIndexService.listIndexes("b", null, null, null))
      .thenReturn(ListIndexesResponse(summaries, null))

    mockMvc
      .perform(
        post("/ListIndexes")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"b"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.indexes.length()").value(2))
  }

  @Test
  fun `ListIndexes without bucket returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/ListIndexes")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `DeleteIndex returns 200 on success`() {
    mockMvc
      .perform(
        post("/DeleteIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"i","vectorBucketName":"b"}"""),
      ).andExpect(status().isOk)

    verify(vectorIndexService).deleteIndex("b", "i")
  }

  @Test
  fun `DeleteIndex maps a missing index to 404 NotFoundException`() {
    whenever(vectorIndexService.deleteIndex("b", "ghost")).thenThrow(S3VectorsException.INDEX_NOT_FOUND)

    mockMvc
      .perform(
        post("/DeleteIndex")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexName":"ghost","vectorBucketName":"b"}"""),
      ).andExpect(status().isNotFound)
      .andExpect(header().string("x-amzn-errortype", "NotFoundException"))
  }
}
