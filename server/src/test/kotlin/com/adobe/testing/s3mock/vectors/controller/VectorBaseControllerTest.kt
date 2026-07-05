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

import com.adobe.testing.s3mock.vectors.service.VectorBucketService
import com.adobe.testing.s3mock.vectors.service.VectorIndexService
import com.adobe.testing.s3mock.vectors.service.VectorQueryService
import com.adobe.testing.s3mock.vectors.service.VectorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Base class for S3 Vectors controller slice tests.
 *
 * The vectors web layer is not part of the default MVC slice:
 * - the controllers are registered as `@Bean`s inside [VectorsControllerConfiguration]
 *   (the application excludes `@RestController` from component scan),
 * - the JSON [org.springframework.http.converter.HttpMessageConverter], content negotiation
 *   and the `S3VectorsException`/`IllegalStateException` handlers all live in that configuration,
 * - all of it is gated behind the `vectors` Spring profile.
 *
 * Extending this class activates the profile, imports the configuration and mocks the four
 * vector services so that a concrete test only needs to autowire [mockMvc].
 */
@WebMvcTest
@ActiveProfiles("vectors")
@Import(VectorsControllerConfiguration::class)
internal abstract class VectorBaseControllerTest {
  @MockitoBean
  protected lateinit var vectorBucketService: VectorBucketService

  @MockitoBean
  protected lateinit var vectorIndexService: VectorIndexService

  @MockitoBean
  protected lateinit var vectorService: VectorService

  @MockitoBean
  protected lateinit var vectorQueryService: VectorQueryService

  @Autowired
  protected lateinit var mockMvc: MockMvc

  companion object {
    val MAPPER: JsonMapper =
      JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .findAndAddModules()
        .build()
  }
}
