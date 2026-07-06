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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal class VectorTaggingControllerTest : VectorBaseControllerTest() {
  @Test
  fun `tagResource routes a bucket ARN to the bucket service`() {
    mockMvc
      .perform(
        post("/tags/$BUCKET_ARN")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"tags":{"k":"v"}}"""),
      ).andExpect(status().isOk)

    verify(vectorBucketService).tagBucket(BUCKET_ARN, mapOf("k" to "v"))
  }

  @Test
  fun `tagResource routes an index ARN to the index service`() {
    mockMvc
      .perform(
        post("/tags/$INDEX_ARN")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"tags":{"k":"v"}}"""),
      ).andExpect(status().isOk)

    verify(vectorIndexService).tagIndex(null, INDEX_ARN, mapOf("k" to "v"))
  }

  @Test
  fun `tagResource with null tags delegates an empty map`() {
    mockMvc
      .perform(
        post("/tags/$BUCKET_ARN")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{}"""),
      ).andExpect(status().isOk)

    verify(vectorBucketService).tagBucket(BUCKET_ARN, emptyMap())
  }

  @Test
  fun `listTagsForResource returns 200 with bucket tags`() {
    whenever(vectorBucketService.getBucketTags(BUCKET_ARN)).thenReturn(mapOf("k" to "v"))

    mockMvc
      .perform(get("/tags/$BUCKET_ARN"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.tags.k").value("v"))
  }

  @Test
  fun `listTagsForResource returns 200 with index tags`() {
    whenever(vectorIndexService.getIndexTags(null, INDEX_ARN)).thenReturn(mapOf("k" to "v"))

    mockMvc
      .perform(get("/tags/$INDEX_ARN"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.tags.k").value("v"))
  }

  @Test
  fun `untagResource routes a bucket ARN to the bucket service`() {
    mockMvc
      .perform(delete("/tags/$BUCKET_ARN").param("tagKeys", "k"))
      .andExpect(status().isOk)

    verify(vectorBucketService).removeBucketTags(BUCKET_ARN, listOf("k"))
  }

  @Test
  fun `untagResource routes an index ARN to the index service`() {
    mockMvc
      .perform(delete("/tags/$INDEX_ARN").param("tagKeys", "k"))
      .andExpect(status().isOk)

    verify(vectorIndexService).removeIndexTags(null, INDEX_ARN, listOf("k"))
  }

  @Test
  fun `extractArn rejects a URI whose suffix is not an ARN`() {
    mockMvc
      .perform(get("/tags/not-an-arn"))
      .andExpect(status().isBadRequest)
      .andExpect(header().string("x-amzn-errortype", "ValidationException"))
  }

  private companion object {
    const val BUCKET_ARN = "arn:aws:s3vectors:us-east-1:123456789012:bucket/my-bucket"
    const val INDEX_ARN = "arn:aws:s3vectors:us-east-1:123456789012:bucket/my-bucket/index/my-index"
  }
}
