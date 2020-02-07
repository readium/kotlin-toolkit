/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

internal typealias ReadingProgression = org.readium.r2.shared.publication.ReadingProgression

internal typealias Encryption = org.readium.r2.shared.publication.encryption.Encryption

internal typealias Link = org.readium.r2.shared.publication.Link

internal typealias LocalizedString = org.readium.r2.shared.publication.LocalizedString

internal typealias Subject = org.readium.r2.shared.publication.Subject

internal typealias Contributor = org.readium.r2.shared.publication.Contributor

internal data class Title(
        val value: LocalizedString,
        val fileAs: String? = null,
        val type: String? = null,
        val displaySeq: Int? = null
)

internal data class EpubLink(
        val href: String,
        val rel: List<String>,
        val mediaType: String?, val refines: String?,
        val properties: List<String> = emptyList()
)

internal data class MetaItem(
        val property: String,
        val value: String, val lang: String,
        val scheme: String? = null,
        val refines: String? = null,
        val id: String?,
        val children: List<MetaItem> = emptyList()
)

internal data class EpubMetadata(
        val uniqueIdentifierId: String?,
        val globalItems: List<MetaItem>,
        val refineItems: Map<String, List<MetaItem>>,
        val links: List<EpubLink>
)

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
