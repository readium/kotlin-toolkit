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
    "dcterms" to Vocabularies.Dcterms,
    "media" to Vocabularies.Media,
    "rendition" to Vocabularies.Rendition,
    "ally" to Vocabularies.Ally,
    "marc" to Vocabularies.Marc,
    "onix" to Vocabularies.Onix,
    "schema" to Vocabularies.Schema,
    "xsd" to Vocabularies.Xsd
)

internal val CONTENT_RESERVED_PREFIXES = mapOf(
    "msv" to Vocabularies.Msv,
    "prism" to Vocabularies.Prism
)

internal enum class DEFAULT_VOCAB(val iri: String) {
    META(Vocabularies.Meta),
    LINK(Vocabularies.Link),
    ITEM(Vocabularies.Item),
    ITEMREF(Vocabularies.Itemref),
    TYPE(Vocabularies.Type)
}

internal fun resolveProperty(
    property: String, prefixMap: Map<String, String>,
    defaultVocab: DEFAULT_VOCAB? = null
): String? {
    val splitted = property.split(":", limit = 2).filterNot(String::isEmpty)
    return if (splitted.size == 1) {
        if (defaultVocab == null) Timber.d("Missing prefix in property $property, no default vocabulary available")
        defaultVocab?.iri?.let { it + splitted[0] }
    } else if (splitted.size == 2) {
        val vocab = prefixMap[splitted[0]]
        if (vocab == null) Timber.d("Prefix ${splitted[0]} has not been declared and is not a reserved prefix either.")
        prefixMap[splitted[0]]?.let { it + splitted[1] }
    } else // empty string
        null
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