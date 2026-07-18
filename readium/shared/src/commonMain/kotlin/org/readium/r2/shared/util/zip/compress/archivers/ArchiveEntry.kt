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
// (Java source: `util/zip/compress/archivers/ArchiveEntry.java`, phase 05b). The
// `getLastModifiedDate(): java.util.Date` accessor was dropped from the interface: the ported
// [org.readium.r2.shared.util.zip.compress.archivers.zip.ZipArchiveEntry] exposes the epoch
// milliseconds through `time` instead.

package org.readium.r2.shared.util.zip.compress.archivers

/**
 * Represents an entry of an archive.
 */
internal interface ArchiveEntry {

    /** The name of the entry in this archive. May refer to a file or directory or other item. */
    val name: String

    /** The uncompressed size of this entry. May be -1 ([SIZE_UNKNOWN]) if the size is unknown. */
    val size: Long

    /** True if this entry refers to a directory. */
    val isDirectory: Boolean

    companion object {
        /** Special value indicating that the size is unknown. */
        const val SIZE_UNKNOWN: Long = -1
    }
}
