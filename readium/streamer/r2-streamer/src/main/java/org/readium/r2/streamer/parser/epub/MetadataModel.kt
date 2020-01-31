/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

data class MultiString(val main: String, val alt: Map<String, String>, val fileAs: String? = null)

data class Title(val value: MultiString, val type: String? = null, val displaySeq: Int? = null)

data class Contributor(val name: MultiString, val roles: Set<String> = setOf())

data class Link(val rel: List<String>, val href: String,
                val mediaType: String?, val refines: String?, val properties: List<String> = listOf())

data class Date(val value: String, val event: String? = null)

data class Subject (val value: String, val authority: String?, val term: String?)

data class Metadata(
        val generalMetadata: GeneralMetadata,
        val mediaMetadata: MediaMetadata,
        val renditionMetadata: RenditionMetadata,
        val links: List<Link>,
        val oldMeta: Map<String, String>
)

data class GeneralMetadata(
        val uniqueIdentifier: String?,
        val titles: List<Title>,
        val languages: List<String>,
        val date: String?,
        val modified: java.util.Date?,
        val description: String?,
        val rights: String?,
        val source: String?,
        val subjects: List<Subject>,
        val creators: List<Contributor>,
        val contributors: List<Contributor>,
        val publishers: List<Contributor>)

data class MediaMetadata(
        val duration: Double? = null,
        val durationById: Map<String, Double> = mapOf(),
        val activeClass: String? = null,
        val playbackActiveClass: String? = null,
        val narrators: List<Contributor> = listOf()
)

data class RenditionMetadata(
        val flow: Flow,
        val layout: Layout,
        val orientation: Orientation,
        val spread: Spread) {

    enum class Flow(val value: String) {
        Auto("auto"),
        Paginated("paginated"),
        Continuous("scrolled-continuous"),
        Document("scrolled-doc");

        companion object : EnumCompanion<String, Flow>(
                Flow.Auto, values().associateBy(Flow::value)
        )
    }

    enum class Layout(val value: String) {
        Reflowable("reflowable"),
        PrePaginated("pre-paginated");

        companion object : EnumCompanion<String, Layout>(
                Layout.Reflowable, values().associateBy(Layout::value)
        )
    }

    enum class Orientation(val value: String) {
        Auto("auto"),
        Landscape("landscape"),
        Portait("portrait");

        companion object : EnumCompanion<String, Orientation>(
                Orientation.Auto, values().associateBy(Orientation::value)
        )
    }

    enum class Spread(val value: String) {
        Auto("auto"),
        None("none"),
        Landscape("landscape"),
        Both("both");

        companion object : EnumCompanion<String, Spread>(
                Spread.Auto, values().associateBy(Spread::value)
        )
    }
}
