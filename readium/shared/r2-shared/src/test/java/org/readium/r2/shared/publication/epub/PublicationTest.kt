/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Metadata

class PublicationTest {

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

}
