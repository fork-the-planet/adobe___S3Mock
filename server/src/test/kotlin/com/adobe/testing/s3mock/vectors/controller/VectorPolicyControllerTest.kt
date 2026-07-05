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

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal class VectorPolicyControllerTest : VectorBaseControllerTest() {
  @Test
  fun `PutVectorBucketPolicy returns 200 and delegates to the service`() {
    mockMvc
      .perform(
        post("/PutVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket","policy":"{}"}"""),
      ).andExpect(status().isOk)

    verify(vectorBucketService).putPolicy("bucket", "{}")
  }

  @Test
  fun `PutVectorBucketPolicy without name or arn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/PutVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"policy":"{}"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `PutVectorBucketPolicy without policy returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/PutVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  @Test
  fun `GetVectorBucketPolicy returns 200 with the stored policy`() {
    whenever(vectorBucketService.getPolicy("bucket")).thenReturn("{}")

    mockMvc
      .perform(
        post("/GetVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.policy").value("{}"))
  }

  @Test
  fun `GetVectorBucketPolicy returns 404 NotFoundException when no policy is present`() {
    whenever(vectorBucketService.getPolicy("bucket")).thenReturn(null)

    mockMvc
      .perform(
        post("/GetVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isNotFound)
      .andExpect(header().string("x-amzn-errortype", "NotFoundException"))
  }

  @Test
  fun `DeleteVectorBucketPolicy returns 200 and delegates to the service`() {
    mockMvc
      .perform(
        post("/DeleteVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"vectorBucketName":"bucket"}"""),
      ).andExpect(status().isOk)

    verify(vectorBucketService).deletePolicy("bucket")
  }

  @Test
  fun `DeleteVectorBucketPolicy without name or arn returns 400 ValidationException`() {
    mockMvc
      .perform(
        post("/DeleteVectorBucketPolicy")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }
}
