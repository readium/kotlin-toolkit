/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@OptIn(DelicateReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
class LegacyLocatorMigrationTest {

    private val href = Url("document.pdf")!!

    // A 3-page PDF, as `PdfPositionsService` reports it.
    private val positions = (1..3).map { position ->
        Locator(
            href = href,
            mediaType = MediaType.PDF,
            locations = Locator.Locations(
                fragments = listOf("page=$position"),
                progression = (position - 1) / 3.0,
                position = position,
                totalProgression = (position - 1) / 3.0
            )
        )
    }

    private val publication = Publication(
        manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString("Title")),
            readingOrder = listOf(Link(href = Href(href.toString())!!, mediaType = MediaType.PDF))
        ),
        servicesBuilder = Publication.ServicesBuilder().apply {
            positionsServiceFactory = {
                object : PositionsService {
                    override suspend fun positionsByReadingOrder(): List<List<Locator>> =
                        listOf(positions)
                }
            }
        }
    )

    // A locator persisted by the legacy navigator, which stored the whole `positions()`
    // element it was given — but for the page after the visible one.
    private fun storedLocator(
        position: Int?,
        text: Locator.Text = Locator.Text(),
        otherLocations: Map<String, Any> = emptyMap(),
    ) = Locator(
        href = href,
        mediaType = MediaType.PDF,
        title = "Bookmark",
        text = text,
        locations = Locator.Locations(
            fragments = position?.let { listOf("page=$it") } ?: emptyList(),
            progression = position?.let { (it - 1) / 3.0 },
            position = position,
            totalProgression = position?.let { (it - 1) / 3.0 },
            otherLocations = otherLocations
        )
    )

    @Test
    fun `shifts the position back by one`() = runTest {
        // Page 2 was stored as position 3.
        val migrated = publication.migrateLegacyPdfiumLocator(storedLocator(position = 3))

        assertEquals(2, migrated.locations.position)
        assertEquals(1 / 3.0, migrated.locations.progression)
        assertEquals(1 / 3.0, migrated.locations.totalProgression)
    }

    @Test
    fun `shifts the page fragment back by one`() = runTest {
        // The navigator resolves `page` before `position`, so a stale fragment would
        // make the migration a no-op.
        val migrated = publication.migrateLegacyPdfiumLocator(storedLocator(position = 3))

        assertEquals(listOf("page=2"), migrated.locations.fragments)
    }

    @Test
    fun `preserves the title, text and other locations`() = runTest {
        val text = Locator.Text(highlight = "Highlight")
        val migrated = publication.migrateLegacyPdfiumLocator(
            storedLocator(position = 3, text = text, otherLocations = mapOf("custom" to "value"))
        )

        assertEquals("Bookmark", migrated.title)
        assertEquals(text, migrated.text)
        assertEquals(mapOf("custom" to "value"), migrated.locations.otherLocations)
    }

    @Test
    fun `keeps a locator without a position unchanged`() = runTest {
        val locator = storedLocator(position = null)
        assertEquals(locator, publication.migrateLegacyPdfiumLocator(locator))
    }

    @Test
    fun `keeps position 1 unchanged`() = runTest {
        // Position 1 is only ever the initial value, never a page reported by the
        // legacy navigator.
        val locator = storedLocator(position = 1)
        assertEquals(locator, publication.migrateLegacyPdfiumLocator(locator))
    }

    @Test
    fun `keeps an out of range position unchanged`() = runTest {
        val locator = storedLocator(position = 42)
        assertEquals(locator, publication.migrateLegacyPdfiumLocator(locator))
    }
}
