/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Inflater
import timber.log.Timber

/**
 * Inflates a ZIP-compressed [ByteArray].
 *
 * @param nowrap If true then support GZIP compatible compression, see the documentation of [Inflater]
 */
fun ByteArray.inflate(nowrap: Boolean = false, bufferSize: Int = 32 * 1024 /* 32 KB */): ByteArray =
    ByteArrayOutputStream().use { output ->
        val inflater = Inflater(nowrap)
        inflater.setInput(this)

        val buffer = ByteArray(bufferSize)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            output.write(buffer, 0, count)
        }

        output.toByteArray()
    }

/** Computes the MD5 hash of the byte array. */
fun ByteArray.md5(): String? =
    try {
        MessageDigest
            .getInstance("MD5")
            .digest(this)
            .fold("") { str, it -> str + "%02x".format(it) }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
