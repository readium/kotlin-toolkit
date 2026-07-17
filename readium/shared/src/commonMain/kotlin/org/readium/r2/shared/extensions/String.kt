/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import com.eygraber.uri.Uri
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import okio.ByteString.Companion.encodeUtf8
import org.readium.r2.shared.InternalReadiumApi

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

@OptIn(InternalReadiumApi::class)
@InternalReadiumApi
public fun String.toInstant(): kotlin.time.Instant? =
    tryOrNull { kotlin.time.Instant.parse(this) }
        ?: tryOrNull { LocalDateTime.parse(this).toInstant(TimeZone.UTC) }
        ?: tryOrNull { LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC) }

internal enum class HashAlgorithm {
    MD5,
    SHA256,
}

internal fun String.hash(algorithm: HashAlgorithm): String =
    encodeUtf8()
        .let {
            when (algorithm) {
                HashAlgorithm.MD5 -> it.md5()
                HashAlgorithm.SHA256 -> it.sha256()
            }
        }
        .hex()

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

/**
 * Returns whether the String receiver contains only printable ASCII characters.
 */
internal fun String.isPrintableAscii(): Boolean =
    all { it.code in 0x20..0x7F }
