/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.joda.time.DateTime
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.*

fun String.iso8601ToDate(): Date? =
    try {
        DateTime(this).toDate()
    } catch (e: Exception) {
        null
    }

/**
 * If this string starts with the given [prefix], returns this string.
 * Otherwise, returns a copy of this string after adding the [prefix].
 */
fun String.addPrefix(prefix: CharSequence): String {
    if (startsWith(prefix)) {
        return this
    }
    return "$prefix$this"
}

internal enum class HashAlgorithm(val key: String) {
    MD5("MD5"),
    SHA256("SHA-256")
}

internal fun String.hash(algorithm: HashAlgorithm): String =
    MessageDigest
        .getInstance(algorithm.key)
        .digest(this.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }

internal fun String.toUrlOrNull(context: URL? = null) =
    try {
        URL(context, this)
    } catch (e: Exception) {
        null
    }

internal fun String.toJsonOrNull(): JSONObject? =
    try {
        JSONObject(this)
    } catch (e: JSONException) {
        null
    }

internal fun String.queryParameters(): Map<String, String> = URLDecoder.decode(this, "UTF-8")
    .substringAfter("?") // query start
    .takeWhile { it != '#' } // anchor start
    .split("&")
    .mapNotNull { it.split("=").takeIf { it.size == 2 } }
    .associate { Pair(it[0], it[1]) }
