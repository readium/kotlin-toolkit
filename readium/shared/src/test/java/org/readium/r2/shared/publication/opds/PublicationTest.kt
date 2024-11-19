/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.opds

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PublicationTest {

    private fun createPublication(
        subCollections: Map<String, List<PublicationCollection>> = emptyMap(),
    ) = Publication(
        Manifest(
            metadata = Metadata(localizedTitle = LocalizedString("Title")),
            subcollections = subCollections
        )
    )

    @Test fun `get {images}`() {
        val links = listOf(Link(href = Href("/image.png")!!))
        assertEquals(
            links,
            createPublication(
                subCollections = mapOf(
                    "images" to listOf(PublicationCollection(links = links))
                )
            ).images
        )
    }

    @Test fun `get {images} when missing`() {
        assertEquals(0, createPublication().images.size)
    }
}
