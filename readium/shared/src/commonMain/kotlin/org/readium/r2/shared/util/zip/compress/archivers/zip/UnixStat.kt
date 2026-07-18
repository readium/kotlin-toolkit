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
// (Java source: `util/zip/compress/archivers/zip/UnixStat.java`, phase 05b). Kotlin has no octal
// literals: the original octal values are kept as comments.

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Constants from stat.h on Unix systems.
 */
internal object UnixStat {

    /** Bits used for permissions (and sticky bit). 07777 */
    const val PERM_MASK: Int = 0xFFF

    /** Bits used to indicate the file system object type. 0170000 */
    const val FILE_TYPE_FLAG: Int = 0xF000

    /** Indicates symbolic links. 0120000 */
    const val LINK_FLAG: Int = 0xA000

    /** Indicates plain files. 0100000 */
    const val FILE_FLAG: Int = 0x8000

    /** Indicates directories. 040000 */
    const val DIR_FLAG: Int = 0x4000

    /** Default permissions for symbolic links. 0777 */
    const val DEFAULT_LINK_PERM: Int = 0x1FF

    /** Default permissions for directories. 0755 */
    const val DEFAULT_DIR_PERM: Int = 0x1ED

    /** Default permissions for plain files. 0644 */
    const val DEFAULT_FILE_PERM: Int = 0x1A4
}
