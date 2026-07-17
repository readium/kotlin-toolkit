/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

/**
 * Canonical IANA names for the common character sets and their aliases, mirroring the most useful
 * subset of `java.nio.charset.Charset.forName()` used on Android.
 */
private val charsetAliases: Map<String, String> = buildMap {
    fun register(canonicalName: String, vararg aliases: String) {
        put(canonicalName.lowercase(), canonicalName)
        for (alias in aliases) {
            put(alias.lowercase(), canonicalName)
        }
    }

    register("US-ASCII", "ascii", "default", "646", "iso646-us", "ansi_x3.4-1968", "cp367")
    register("UTF-8", "utf8", "unicode-1-1-utf-8")
    register("UTF-16", "utf16", "unicode", "unicodebig")
    register("UTF-16BE", "unicodebigunmarked", "x-utf-16be")
    register("UTF-16LE", "unicodelittleunmarked", "x-utf-16le")
    register("UTF-32", "utf32")
    register("UTF-32BE", "x-utf-32be")
    register("UTF-32LE", "x-utf-32le")
    register("ISO-8859-1", "iso8859-1", "iso_8859-1", "iso8859_1", "latin1", "l1", "8859_1", "cp819", "819")
    register("ISO-8859-15", "iso8859-15", "iso_8859-15", "latin9", "l9")
    register("windows-1252", "cp1252")
}

internal actual fun canonicalCharsetName(name: String): String? =
    charsetAliases[name.trim().lowercase()]
