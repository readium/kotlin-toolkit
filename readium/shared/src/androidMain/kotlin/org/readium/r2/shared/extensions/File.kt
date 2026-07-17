/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import java.io.File
import okio.HashingSource
import okio.blackholeSink
import okio.buffer
import okio.source
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.logging.ReadiumLog

/**
 * Computes the MD5 hash of the file.
 *
 * Returns null if [File] is a directory or a file that failed to be read.
 */
@InternalReadiumApi
public fun File.md5(): String? =
    try {
        HashingSource.md5(source()).use { hashingSource ->
            hashingSource.buffer().readAll(blackholeSink())
            hashingSource.hash.hex()
        }
    } catch (e: Exception) {
        ReadiumLog.e(e)
        null
    }

/**
 * Returns whether the `other` is a descendant of this file.
 */
@InternalReadiumApi
public fun File.isParentOf(other: File): Boolean {
    val canonicalThis = canonicalFile
    var parent = other.canonicalFile.parentFile
    while (parent != null) {
        if (parent == canonicalThis) {
            return true
        }
        parent = parent.parentFile
    }
    return false
}
