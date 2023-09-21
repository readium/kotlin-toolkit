/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import android.net.Uri
import java.net.URL
import java.security.MessageDigest
import java.util.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
public fun String.iso8601ToDate(): Date? =
    try {
        // We assume that a date without a time zone component is in UTC. To handle this properly,
        // we need to set the default time zone of Joda to UTC, since by default it uses the local
        // time zone. This ensures that apps see exactly the same dates (e.g. published) no matter
        // where they are located.
        // For the same reason, the output Date will be in UTC. Apps should convert it to the local
        // time zone for display purposes, or keep it as UTC for storage.
        val defaultTZ = DateTimeZone.getDefault()
        DateTimeZone.setDefault(DateTimeZone.UTC)
        val date = DateTime(this).toDateTime(DateTimeZone.UTC).toDate()
        DateTimeZone.setDefault(defaultTZ)
        date
    } catch (e: Exception) {
        null
    }

/**
 * If this string starts with the given [prefix], returns this string.
 * Otherwise, returns a copy of this string after adding the [prefix].
 */
@InternalReadiumApi
public fun String.addPrefix(prefix: CharSequence): String {
    if (startsWith(prefix)) {
        return this
    }
    return prefix.toString() + this
}

/**
 * If this string ends with the given [suffix], returns this string.
 * Otherwise, returns a copy of this string after adding the [suffix].
 */
@InternalReadiumApi
public fun String.addSuffix(suffix: CharSequence): String {
    if (endsWith(suffix)) {
        return this
    }
    return this + suffix
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

/**
 * Percent-encodes an URL path section.
 *
 * Equivalent to Swift's `string.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed)`
 */
internal fun String.percentEncodedPath(): String =
    Uri.encode(this, "$&+,/:=@")

/**
 * Percent-encodes an URL query key or value.
 *
 * Equivalent to Swift's `string.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)`
 */
internal fun String.percentEncodedQuery(): String =
    Uri.encode(this, "$+,/?:=@")
