/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ported to Kotlin Multiplatform for Readium from the vendored Commons Compress subset
// (Java source: `util/zip/compress/archivers/zip/UnsupportedZipFeatureException.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Exception thrown when attempting to read or write data for a zip entry that uses ZIP features
 * not supported by this library.
 */
internal class UnsupportedZipFeatureException : ZipException {

    /** The feature that is not supported. */
    val feature: Feature

    /** The entry using the unsupported feature. */
    val entry: ZipArchiveEntry?

    /**
     * Creates an exception when whether a specific feature is supported depends on the entry.
     */
    constructor(reason: Feature, entry: ZipArchiveEntry) : super(
        "Unsupported feature $reason used in entry ${entry.name}"
    ) {
        this.feature = reason
        this.entry = entry
    }

    /**
     * Creates an exception for an unsupported compression method.
     */
    constructor(method: ZipMethod?, entry: ZipArchiveEntry) : super(
        "Unsupported compression method ${entry.method} (${method?.name ?: "UNKNOWN"}) " +
            "used in entry ${entry.name}"
    ) {
        this.feature = Feature.METHOD
        this.entry = entry
    }

    /**
     * ZIP features that may or may not be supported.
     */
    class Feature private constructor(private val name: String) {

        override fun toString(): String = name

        companion object {
            /** The entry is encrypted. */
            val ENCRYPTION: Feature = Feature("encryption")

            /** The entry used an unsupported compression method. */
            val METHOD: Feature = Feature("compression method")

            /** The entry uses a data descriptor. */
            val DATA_DESCRIPTOR: Feature = Feature("data descriptor")

            /** The archive uses splitting or spanning. */
            val SPLITTING: Feature = Feature("splitting")

            /** The archive contains entries with unknown compressed size. */
            val UNKNOWN_COMPRESSED_SIZE: Feature = Feature("unknown compressed size")
        }
    }
}
