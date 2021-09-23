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
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.presentation.Presentation

class PresentationTest {

    @Test
    fun `Get the layout of a reflowable resource`() {
        assertEquals(
            EpubLayout.REFLOWABLE,
            Presentation(layout = null).layoutOf(createLink(EpubLayout.REFLOWABLE))
        )
    }

    @Test
    fun `Get the layout of a fixed resource`() {
        assertEquals(
            EpubLayout.FIXED,
            Presentation(layout = null).layoutOf(createLink(EpubLayout.FIXED))
        )
    }

    @Test
    fun `The layout of a resource takes precedence over the document layout`() {
        assertEquals(
            EpubLayout.FIXED,
            Presentation(layout = EpubLayout.REFLOWABLE).layoutOf(createLink(EpubLayout.FIXED))
        )
    }

    @Test
    fun `Get the layout falls back on the document layout`() {
        assertEquals(
            EpubLayout.FIXED,
            Presentation(layout = EpubLayout.FIXED).layoutOf(createLink(null))
        )
    }

    @Test
    fun `Get the layout falls back on REFLOWABLE`() {
        assertEquals(
            EpubLayout.REFLOWABLE,
            Presentation(layout = null).layoutOf(createLink(null))
        )
    }

    private fun createLink(layout: EpubLayout?) = Link(
        href = "res",
        properties = Properties(
            otherProperties = layout?.let { mapOf("layout" to layout.value) }
                ?: emptyMap()
        )
    )

}
