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
// (Java source: `util/zip/compress/archivers/EntryStreamOffsets.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers

/**
 * Provides information about the offsets of an archive entry's data within the archive.
 */
internal interface EntryStreamOffsets {

    /**
     * The offset of data stream within the archive file, or [OFFSET_UNKNOWN].
     */
    val dataOffset: Long

    /**
     * True if the stream is contiguous, i.e. not split among several archive parts, interspersed
     * with control blocks, etc.
     */
    val isStreamContiguous: Boolean

    companion object {
        /** Special value indicating that the offset is unknown. */
        const val OFFSET_UNKNOWN: Long = -1
    }
}
