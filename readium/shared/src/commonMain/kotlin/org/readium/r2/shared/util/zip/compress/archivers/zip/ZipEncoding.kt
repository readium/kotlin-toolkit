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
// (Java source: `util/zip/compress/archivers/zip/ZipEncoding.java`, phase 05b).
// The `encode`/`canEncode` half of the original interface was dropped: it is only used when
// writing archives, which the read-only Readium subset never does.

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * An interface for encoders that do a pretty encoding of ZIP file names.
 *
 * ZIP file names are historically encoded with the CP437 charset; modern archives use UTF-8
 * (flagged through the general purpose bit 11 or the InfoZIP Unicode extra fields).
 */
internal interface ZipEncoding {

    /**
     * Decodes a file name or a comment from the given bytes, replacing malformed input with `?`.
     */
    fun decode(data: ByteArray): String
}
