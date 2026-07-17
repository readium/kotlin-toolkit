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
import java.io.FileInputStream
import java.security.MessageDigest
import org.readium.r2.shared.InternalReadiumApi
import timber.log.Timber

/**
 * Computes the MD5 hash of the file.
 *
 * Returns null if [File] is a directory or a file that failed to be read.
 */
@InternalReadiumApi
public fun File.md5(): String? =
    try {
        val md = MessageDigest.getInstance("MD5")
        // https://stackoverflow.com/questions/10143731/android-optimal-buffer-size
        val bufferSize = 32000
        val buffer = ByteArray(bufferSize)
        FileInputStream(this).use {
            var bytes: Int
            do {
                bytes = it.read(buffer, 0, bufferSize)
                if (bytes > 0) {
                    md.update(buffer, 0, bytes)
                }
            } while (bytes > 0)
        }

        md.digest()
            // ByteArray to hex string
            .fold("") { str, it -> str + "%02x".format(it) }
    } catch (e: Exception) {
        Timber.e(e)
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
