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

import com.adobe.testing.s3mock.vectors.dto.QueryVectorsResponse
import com.adobe.testing.s3mock.vectors.dto.VectorData
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal class VectorQueryControllerTest : VectorBaseControllerTest() {
  @Test
  fun `QueryVectors returns 200 with the service response and defaults topK to 10`() {
    whenever(
      vectorQueryService.queryVectors("bucket", "index", VectorData(listOf(1.0, 2.0)), 10, null, false, false),
    ).thenReturn(QueryVectorsResponse("cosine", emptyList()))

    mockMvc
      .perform(
        post("/QueryVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","indexName":"index","queryVector":{"float32":[1.0,2.0]}}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.distanceMetric").value("cosine"))
      .andExpect(jsonPath("$.vectors").isArray)
  }

  @Test
  fun `QueryVectors without indexName or indexArn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/QueryVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","queryVector":{"float32":[1.0]}}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `QueryVectors without queryVector returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/QueryVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","indexName":"index"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }
}
