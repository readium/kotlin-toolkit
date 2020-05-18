/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import timber.log.Timber
import java.security.MessageDigest

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

