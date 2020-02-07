/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

internal typealias LocalizedString = org.readium.r2.shared.publication.LocalizedString

internal typealias Subject = org.readium.r2.shared.publication.Subject

internal typealias Contributor = org.readium.r2.shared.publication.Contributor

internal data class Title(val value: LocalizedString, val fileAs: String? = null, val type: String? = null, val displaySeq: Int? = null)

internal data class EpubLink(val href: String, val rel: List<String>,
                             val mediaType: String?, val refines: String?, val properties: List<String> = emptyList())

internal data class MetaItem(val property: String, val value: String,
                             val scheme: String? = null, val refines: String? = null,
                             val lang: String? = null, val children: List<MetaItem> = emptyList()
)

internal data class EpubMetadata(
        val general: GeneralMetadata,
        val links: List<EpubLink>,
        val metas: Map<String?, List<MetaItem>>
)

internal data class GeneralMetadata(
        val uniqueIdentifier: String?,
        val titles: List<Title>,
        val languages: List<String>,
        val date: java.util.Date?,
        val modified: java.util.Date?,
        val description: String?,
        val rights: String?,
        val source: String?,
        val subjects: List<Subject>,
        val creators: List<Contributor>,
        val contributors: List<Contributor>,
        val publishers: List<Contributor>
)

internal data class MediaMetadata(
        val duration: Double?,
        val activeClass: String?,
        val playbackActiveClass: String?,
        val narrators: List<Contributor>
)
