/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import timber.log.Timber

internal val PACKAGE_RESERVED_PREFIXES = mapOf(
    "dcterms" to Vocabularies.DCTERMS,
    "media" to Vocabularies.MEDIA,
    "rendition" to Vocabularies.RENDITION,
    "a11y" to Vocabularies.A11Y,
    "marc" to Vocabularies.MARC,
    "onix" to Vocabularies.ONIX,
    "schema" to Vocabularies.SCHEMA,
    "xsd" to Vocabularies.XSD
)

internal val CONTENT_RESERVED_PREFIXES = mapOf(
    "msv" to Vocabularies.MSV,
    "prism" to Vocabularies.PRISM
)

internal enum class DEFAULT_VOCAB(val iri: String) {
    META(Vocabularies.META),
    LINK(Vocabularies.LINK),
    ITEM(Vocabularies.ITEM),
    ITEMREF(Vocabularies.ITEMREF),
    TYPE(Vocabularies.TYPE)
}

internal fun resolveProperty(
    property: String, prefixMap: Map<String, String>,
    defaultVocab: DEFAULT_VOCAB? = null
): String {
    val splitted = property.split(":", limit = 2).filterNot(String::isEmpty)
    return if (splitted.size == 1 && defaultVocab != null) {
        defaultVocab.iri + splitted[0]
    } else if (splitted.size == 2 && prefixMap[splitted[0]] != null) {
        prefixMap[splitted[0]] + splitted[1]
    } else
        property
}

internal fun parsePrefixes(prefixes: String): Map<String, String> =
    "\\s*(\\w+):\\s*(\\S+)".toRegex().findAll(prefixes).map {
        val prefixGroup = it.groups[1]
        checkNotNull(prefixGroup)
        val iriGroup = it.groups[2]
        checkNotNull(iriGroup)
        Pair(prefixGroup.value, iriGroup.value)
    }.toMap()

internal fun parseProperties(string: String) =
    string.split("\\s+".toRegex()).filterNot(String::isEmpty)
