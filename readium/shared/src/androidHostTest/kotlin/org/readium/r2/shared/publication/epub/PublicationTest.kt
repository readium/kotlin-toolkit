/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PublicationTest {

    private fun createPublication(
        subcollections: Map<String, List<PublicationCollection>> = emptyMap(),
    ) = Publication(
        manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString("Title")),
            subcollections = subcollections
        )
    )

    @Test fun `get {pageList}`() {
        val links = listOf(Link(href = Href("/page1.html")!!))
        assertEquals(
            links,
            createPublication(
                subcollections = mapOf(
                    "pageList" to listOf(PublicationCollection(links = links))
                )
            ).pageList
        )
    }

    @Test fun `get {pageList} when missing`() {
        assertEquals(0, createPublication().pageList.size)
    }

    @Test fun `get {landmarks}`() {
        val links = listOf(Link(href = Href("/landmark.html")!!))
        assertEquals(
            links,
            createPublication(
                subcollections = mapOf(
                    "landmarks" to listOf(PublicationCollection(links = links))
                )
            ).landmarks
        )
    }

    @Test fun `get {landmarks} when missing`() {
        assertEquals(0, createPublication().landmarks.size)
    }

    @Test fun `get {listOfAudioClips}`() {
        val links = listOf(Link(href = Href("/audio.mp3")!!))
        assertEquals(
            links,
            createPublication(
                subcollections = mapOf(
                    "loa" to listOf(PublicationCollection(links = links))
                )
            ).listOfAudioClips
        )
    }

    @Test fun `get {listOfAudioClips} when missing`() {
        assertEquals(0, createPublication().listOfAudioClips.size)
    }

    @Test fun `get {listOfIllustrations}`() {
        val links = listOf(Link(href = Href("/image.jpg")!!))
        assertEquals(
            links,
            createPublication(
                subcollections = mapOf(
                    "loi" to listOf(PublicationCollection(links = links))
                )
            ).listOfIllustrations
        )
    }

    @Test fun `get {listOfIllustrations} when missing`() {
        assertEquals(0, createPublication().listOfIllustrations.size)
    }

    @Test fun `get {listOfTables}`() {
        val links = listOf(Link(href = Href("/table.html")!!))
        assertEquals(
            links,
            createPublication(
                subcollections = mapOf(
                    "lot" to listOf(
                        PublicationCollection(links = links)
                    )
                )
            ).listOfTables
        )
    }

    @Test fun `get {listOfTables} when missing`() {
        assertEquals(0, createPublication().listOfTables.size)
    }

    @Test fun `get {listOfVideoClips}`() {
        val links = listOf(Link(href = Href("/video.mov")!!))
        assertEquals(
            links,
            createPublication(
                subcollections = mapOf(
                    "lov" to listOf(PublicationCollection(links = links))
                )
            ).listOfVideoClips
        )
    }

    @Test fun `get {listOfVideoClips} when missing`() {
        assertEquals(0, createPublication().listOfVideoClips.size)
    }
}
