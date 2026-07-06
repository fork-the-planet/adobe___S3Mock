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
package com.adobe.testing.s3mock

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * Architecture rules that apply to the test sources themselves.
 *
 * Kept separate from [ArchitectureTest] because that class analyses production code only
 * (`DoNotIncludeTests`), whereas these rules must analyse the test classes
 * (`OnlyIncludeTests`).
 */
@AnalyzeClasses(packages = ["com.adobe.testing.s3mock"], importOptions = [ImportOption.OnlyIncludeTests::class])
class TestArchitectureTest {
  /**
   * Spring-managed components (services, stores, controllers, filters, `@ControllerAdvice`) must be
   * tested with the Spring test harness — `@SpringBootTest` / `@WebMvcTest` plus `@MockitoBean` —
   * extending the matching base class (`ServiceTestBase`, `StoreTestBase`, `BaseControllerTest`).
   *
   * Standard Mockito unit-test style (`@ExtendWith(MockitoExtension)` + `@Mock` + `@InjectMocks`)
   * bypasses the Spring context and the project's bean wiring, so it is banned. Collaborator stubbing
   * still uses mockito-kotlin (`mock()` / `whenever(...)`), which does not rely on these annotations.
   */
  @ArchTest
  val noStandardMockitoUnitTests: ArchRule =
    noClasses()
      .should()
      .dependOnClassesThat()
      .haveFullyQualifiedName("org.mockito.junit.jupiter.MockitoExtension")
      .orShould()
      .dependOnClassesThat()
      .haveFullyQualifiedName("org.mockito.Mock")
      .orShould()
      .dependOnClassesThat()
      .haveFullyQualifiedName("org.mockito.InjectMocks")
      .because(
        "Spring-managed components must be tested with @SpringBootTest/@WebMvcTest + @MockitoBean " +
          "(extending ServiceTestBase/StoreTestBase/BaseControllerTest), not standard Mockito " +
          "unit tests using @ExtendWith(MockitoExtension) + @Mock + @InjectMocks",
      )
}
