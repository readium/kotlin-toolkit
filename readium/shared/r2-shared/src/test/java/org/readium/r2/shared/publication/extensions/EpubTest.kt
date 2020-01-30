/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.extensions

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.link.Link
import org.readium.r2.shared.publication.link.Properties
import org.readium.r2.shared.publication.metadata.Metadata

class EpubTest {

    // EpubLayout

    @Test fun `parse layout`() {
        assertEquals(EpubLayout.FIXED, EpubLayout.from("fixed"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.from("reflowable"))
        assertNull(EpubLayout.from("foobar"))
        assertNull(EpubLayout.from(null))
    }

    @Test fun `parse layout from EPUB rendition property`() {
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("reflowable"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("pre-paginated"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("foobar"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("foobar", fallback = EpubLayout.FIXED))
    }

    @Test fun `get layout value`() {
        assertEquals("fixed", EpubLayout.FIXED.value)
        assertEquals("reflowable", EpubLayout.REFLOWABLE.value)
    }


    // EPUB extensions for [Publication].

    private fun createPublication(
        otherCollections: List<PublicationCollection> = emptyList()
    ) = Publication(
        metadata = Metadata(localizedTitle = LocalizedString("Title")),
        otherCollections = otherCollections
    )

    @Test fun `get {pageList}`() {
        val links = listOf(Link(href = "/page1.html"))
        assertEquals(
            links,
            createPublication(otherCollections = listOf(
                PublicationCollection(role = "page-list", links = links)
            )).pageList
        )
    }

    @Test fun `get {pageList} when missing`() {
        assertEquals(0, createPublication().pageList.size)
    }

    @Test fun `get {landmarks}`() {
        val links = listOf(Link(href = "/landmark.html"))
        assertEquals(
            links,
            createPublication(otherCollections = listOf(
                PublicationCollection(role = "landmarks", links = links)
            )).landmarks
        )
    }

    @Test fun `get {landmarks} when missing`() {
        assertEquals(0, createPublication().landmarks.size)
    }

    @Test fun `get {listOfAudioClips}`() {
        val links = listOf(Link(href = "/audio.mp3"))
        assertEquals(
            links,
            createPublication(otherCollections = listOf(
                PublicationCollection(role = "loa", links = links)
            )).listOfAudioClips
        )
    }

    @Test fun `get {listOfAudioClips} when missing`() {
        assertEquals(0, createPublication().listOfAudioClips.size)
    }

    @Test fun `get {listOfIllustrations}`() {
        val links = listOf(Link(href = "/image.jpg"))
        assertEquals(
            links,
            createPublication(otherCollections = listOf(
                PublicationCollection(role = "loi", links = links)
            )).listOfIllustrations
        )
    }

    @Test fun `get {listOfIllustrations} when missing`() {
        assertEquals(0, createPublication().listOfIllustrations.size)
    }

    @Test fun `get {listOfTables}`() {
        val links = listOf(Link(href = "/table.html"))
        assertEquals(
            links,
            createPublication(otherCollections = listOf(
                PublicationCollection(role = "lot", links = links)
            )).listOfTables
        )
    }

    @Test fun `get {listOfTables} when missing`() {
        assertEquals(0, createPublication().listOfTables.size)
    }

    @Test fun `get {listOfVideoClips}`() {
        val links = listOf(Link(href = "/video.mov"))
        assertEquals(
            links,
            createPublication(otherCollections = listOf(
                PublicationCollection(role = "lov", links = links)
            )).listOfVideoClips
        )
    }

    @Test fun `get {listOfVideoClips} when missing`() {
        assertEquals(0, createPublication().listOfVideoClips.size)
    }


    // EPUB extensions for [Metadata].

    @Test fun `get Metadata {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Metadata(
                localizedTitle = LocalizedString("Title"),
                otherMetadata = mapOf("layout" to "fixed")
            ).layout
        )
    }

    @Test fun `get Metadata {layout} when missing`() {
        assertNull(Metadata(localizedTitle = LocalizedString("Title")).layout)
    }


    // EPUB extensions for link [Properties].

    @Test fun `get Properties {contains} when available`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} removes duplicates`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} when missing`() {
        assertEquals(emptySet<String>(), Properties().contains)
    }

    @Test fun `get Properties {contains} skips duplicates`() {
        assertEquals(
            setOf("mathml"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "mathml"))).contains
        )
    }

    @Test fun `get Properties {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Properties(otherProperties = mapOf("layout" to "fixed")).layout
        )
    }

    @Test fun `get Properties {layout} when missing`() {
        assertNull(Properties().layout)
    }

    @Test fun `get Properties {mediaOverlay} when available`() {
        assertEquals(
            "http://media-overlay",
            Properties(otherProperties = mapOf("mediaOverlay" to "http://media-overlay")).mediaOverlay
        )
    }

    @Test fun `get Properties {mediaOverlay} when missing`() {
        assertNull(Properties().mediaOverlay)
    }

}