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
package com.adobe.testing.s3mock.testcontainers

import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer

private val CONTAINER_LOG = LoggerFactory.getLogger("com.adobe.testing.s3mock.testcontainers.S3Mock")

/** `-Ds3mock.log=true` forwards the container output to the build log (default: off). */
private val LOG_ENABLED: Boolean = System.getProperty("s3mock.log", "false").toBoolean()

/** `-Ds3mock.debug=true` activates the server's `debug` Spring profile (default: off). */
private val DEBUG_ENABLED: Boolean = System.getProperty("s3mock.debug", "false").toBoolean()

/**
 * Applies the two independent, system-property-driven diagnostics flags to a container. Both
 * default to off. Must be called before `start()`:
 *  - `-Ds3mock.log=true` forwards the container's output to the build log.
 *  - `-Ds3mock.debug=true` activates the server's `debug` profile (debug-level logging).
 *
 * The flags are orthogonal: enable only `s3mock.log` for INFO-level output, or combine both for
 * debug-level output.
 */
internal fun S3MockContainer.withDebugLogging(): S3MockContainer =
  apply {
    if (DEBUG_ENABLED) {
      withDebug()
    }
    if (LOG_ENABLED) {
      withLogConsumer(Slf4jLogConsumer(CONTAINER_LOG))
    }
  }
