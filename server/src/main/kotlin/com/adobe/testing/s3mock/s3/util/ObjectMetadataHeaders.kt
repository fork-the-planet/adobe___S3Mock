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

import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_COPY_SOURCE_VERSION_ID
import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_STORAGE_CLASS
import com.adobe.testing.s3mock.common.AwsHttpHeaders.X_AMZ_VERSION_ID
import com.adobe.testing.s3mock.s3.dto.CopySource
import com.adobe.testing.s3mock.s3.dto.StorageClass
import com.adobe.testing.s3mock.s3.model.S3ObjectMetadata
import com.adobe.testing.s3mock.s3.util.HeaderUtil.HEADER_X_AMZ_META_PREFIX
import com.adobe.testing.s3mock.s3.util.HeaderUtil.checksumHeaderFrom

/**
 * Extension functions that build the S3 *response* headers for an [S3ObjectMetadata].
 * Request-header parsing lives in [HeaderUtil].
 */
fun S3ObjectMetadata.objectMetadataHeaders(
  versioning: Boolean,
  queryParams: Map<String, String>,
  includeChecksum: Boolean = true,
): Map<String, String> =
  buildMap {
    putAll(versionHeader(versioning))
    storeHeaders?.let { putAll(it) }
    putAll(userMetadataHeaders())
    encryptionHeaders?.let { putAll(it) }
    if (includeChecksum) putAll(checksumHeader())
    putAll(storageClassHeaders())
    putAll(HeaderUtil.overrideHeadersFrom(queryParams))
  }

fun CopySource.versionHeader(versioning: Boolean): Map<String, String> =
  if (versioning && !versionId.isNullOrEmpty()) {
    mapOf(X_AMZ_COPY_SOURCE_VERSION_ID to versionId)
  } else {
    emptyMap()
  }

fun S3ObjectMetadata.versionHeader(versioning: Boolean): Map<String, String> =
  if (versioning && !versionId.isNullOrEmpty()) {
    mapOf(X_AMZ_VERSION_ID to versionId)
  } else {
    emptyMap()
  }

fun S3ObjectMetadata.checksumHeader(): Map<String, String> {
  val checksumAlgorithm = this.checksumAlgorithm
  val checksum = this.checksum
  return checksumHeaderFrom(checksum, checksumAlgorithm)
}

/**
 * Creates response headers from S3ObjectMetadata StorageClass.
 */
fun S3ObjectMetadata.storageClassHeaders(): Map<String, String> {
  val storageClass = this.storageClass ?: return emptyMap()
  if (storageClass == StorageClass.STANDARD) return emptyMap()
  return mapOf(X_AMZ_STORAGE_CLASS to storageClass.toString())
}

/**
 * Creates response headers from S3ObjectMetadata user metadata.
 */
fun S3ObjectMetadata.userMetadataHeaders(): Map<String, String> {
  val user = this.userMetadata.orEmpty()
  return buildMap {
    user.forEach { (k, v) ->
      if (k.isNotBlank() && v.isNotBlank()) {
        val key = if (k.startsWith(HEADER_X_AMZ_META_PREFIX, ignoreCase = true)) k else HEADER_X_AMZ_META_PREFIX + k
        put(key, v)
      }
    }
  }
}
