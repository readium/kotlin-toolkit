/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CoverServiceTest {

    private val coverBytes: ByteArray
    private val coverBitmap: Bitmap
    private val coverPath: String
    private val coverLink: Link

    init {
        val cover = CoverServiceTest::class.java.getResource("cover.jpg")
        assertNotNull(cover)
        coverBytes = cover.readBytes()
        coverBitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
        coverPath = cover.path
        coverLink = Link(href = coverPath, type = "image/jpeg", width = 598, height = 800)
    }

    @Test
    fun `get works fine`() {
        val service = InMemoryCoverService(coverBitmap)
        val res = service.get(Link("/~readium/cover"))
        assertNotNull(res)
        assertEquals(
            Link(href = "/~readium/cover", type = "image/png", width = 598, height = 800, rels = setOf("cover")),
            res.link
        )

        val bytes = res.read().successOrNull()
        assertNotNull(bytes)

        assertTrue(BitmapFactory.decodeByteArray(bytes, 0, bytes.size).sameAs(coverBitmap))
    }

    @Test
    fun `helper for ServicesBuilder works fine`() {
        val factory = { context: Publication.Service.Context ->
            object : CoverService {
                override val cover: Bitmap? = null
            }
        }
        assertEquals(
            factory,
            Publication.ServicesBuilder().apply { coverServiceFactory = factory }.coverServiceFactory
        )
    }

    @Test
    fun `DefaultCoverService works fine`() {
        val fetcher = FileFetcher(coverPath, coverPath)
        val service = DefaultCoverService(listOf(coverLink), fetcher)
        assertTrue(coverBitmap.sameAs(service.cover))

    }
}