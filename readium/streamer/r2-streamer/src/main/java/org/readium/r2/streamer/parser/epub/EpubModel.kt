/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.ReadingProgression

internal typealias Encryption = org.readium.r2.shared.publication.encryption.Encryption

internal typealias Link = org.readium.r2.shared.publication.Link


internal data class Item(
        val href: String,
        val id: String?,
        val fallback: String?,
        val mediaOverlay: String?,
        val mediaType: String?,
        val properties: List<String>
)

internal data class Itemref(
        val idref: String,
        val linear: Boolean,
        val properties: List<String>
)

internal typealias Manifest = List<Item>

internal data class Spine(
        val itemrefs: List<Itemref>,
        val direction: ReadingProgression,
        val toc: String? = null
)

internal data class PackageDocument(
        val path: String,
        val epubVersion: Double,
        val metadata: EpubMetadata,
        val manifest: Manifest,
        val spine: Spine
)

internal typealias EncryptionData = Map<String, Encryption>

internal typealias NavigationData = Map<String, List<Link>>

internal data class Epub(
        val packageDocument: PackageDocument,
        val navigationData: NavigationData = emptyMap(),
        val encryptionData: EncryptionData = emptyMap()
)
