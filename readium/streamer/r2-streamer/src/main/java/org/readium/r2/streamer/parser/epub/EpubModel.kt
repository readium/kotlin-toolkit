/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Link
import java.lang.IllegalArgumentException

internal open class EnumCompanion<T, V>(val default: V, private val map: Map<T, V>) {
    val names: Set<T>
        get() = map.keys

    fun get(name: T) = map[name] ?: throw IllegalArgumentException("Invalid name $name")
    fun getOrNull(name: T) = map[name]
    fun getOrDefault(name: T) = map[name] ?: default
}

internal data class Item(
        val id: String,
        val href: String,
        val fallback: String? = null,
        val mediaOverlay: String? = null,
        val mediaType: String? = null,
        val properties: List<String> = listOf()
)

internal typealias Manifest = List<Item>

internal data class Itemref(
        val idref: String,
        val linear: Boolean,
        val properties: List<String> = listOf()
)

typealias ReadingProgression = org.readium.r2.shared.publication.ReadingProgression

internal data class Spine(
        val itemrefs: List<Itemref>,
        val direction: ReadingProgression = ReadingProgression.AUTO,
        val toc: String? = null
)

internal data class PackageDocument(
        val path: String,
        val epubVersion: Double,
        val metadata: Metadata,
        val manifest: Manifest,
        val spine: Spine
)

internal typealias Encryption = org.readium.r2.shared.publication.encryption.Encryption

internal typealias EncryptionData = Map<String, Encryption>

internal data class NavigationData(
        val toc: List<Link>,
        val pageList: List<Link>,
        val landmarks: List<Link> = emptyList(),
        val loi: List<Link> = emptyList(),
        val lot: List<Link> = emptyList(),
        val loa: List<Link> = emptyList(),
        val lov: List<Link> = emptyList()
)

internal data class Epub(
        val packageDocument: PackageDocument,
        val navigationData: NavigationData? = null,
        val encryptionData: EncryptionData? = null
)
