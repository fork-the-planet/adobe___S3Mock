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

import com.adobe.testing.s3mock.vectors.dto.GetVectorsResponse
import com.adobe.testing.s3mock.vectors.dto.ListVectorsResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal class VectorControllerTest : VectorBaseControllerTest() {
  @Test
  fun `PutVectors returns 200 and delegates resolved arguments to the service`() {
    mockMvc
      .perform(
        post("/PutVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","indexName":"index"}"""),
      ).andExpect(status().isOk)

    verify(vectorService).putVectors("bucket", "index", emptyList())
  }

  @Test
  fun `PutVectors falls back to indexArn for the bucket when vectorBucketName is null`() {
    mockMvc
      .perform(
        post("/PutVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"indexArn":"index-arn"}"""),
      ).andExpect(status().isOk)

    verify(vectorService).putVectors("index-arn", "index-arn", emptyList())
  }

  @Test
  fun `PutVectors without indexName or indexArn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/PutVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
      .andExpect(jsonPath("$.__type").value("com.amazonaws.s3vectors#ValidationException"))
  }

  @Test
  fun `GetVectors returns 200 with the service response`() {
    whenever(vectorService.getVectors("bucket", "index", emptyList(), false, false))
      .thenReturn(GetVectorsResponse(emptyList()))

    mockMvc
      .perform(
        post("/GetVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","indexName":"index"}"""),
      ).andExpect(status().isOk)
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.vectors").isArray)
  }

  @Test
  fun `GetVectors without indexName or indexArn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/GetVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `ListVectors returns 200 with the service response`() {
    whenever(vectorService.listVectors("bucket", "index", null, null, false, false, null, null))
      .thenReturn(ListVectorsResponse(emptyList(), "next"))

    mockMvc
      .perform(
        post("/ListVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","indexName":"index"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.nextToken").value("next"))
  }

  @Test
  fun `ListVectors without indexName or indexArn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/ListVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `DeleteVectors returns 200 and delegates resolved arguments to the service`() {
    mockMvc
      .perform(
        post("/DeleteVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","indexName":"index"}"""),
      ).andExpect(status().isOk)

    verify(vectorService).deleteVectors("bucket", "index", emptyList())
  }

  @Test
  fun `DeleteVectors without indexName or indexArn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/DeleteVectors")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }
}
