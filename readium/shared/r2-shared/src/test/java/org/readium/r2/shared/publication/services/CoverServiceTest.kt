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
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.linkBlocking
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.readBlocking
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class CoverServiceTest {

    private val coverBytes: ByteArray
    private val coverBitmap: Bitmap
    private val coverPath: String
    private val coverLink: Link
    private val publication: Publication

    init {
        val cover = CoverServiceTest::class.java.getResource("cover.jpg")
        assertNotNull(cover)
        coverBytes = cover.readBytes()
        coverBitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
        coverPath = cover.path
        coverLink = Link(href = coverPath, type = "image/jpeg", width = 598, height = 800)

        publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    localizedTitle = LocalizedString("title")
                ),
                resources = listOf(
                    Link(href = coverPath, rels = setOf("cover"))
                )
            ),
            fetcher = FileFetcher(coverPath, File(coverPath))
        )
    }

    @Test
    fun `get works fine`() {
        val service = InMemoryCoverService(coverBitmap)
        val res = service.get(Link("/~readium/cover", rels = setOf("cover")))
        assertNotNull(res)
        assertEquals(
            Link(href = "/~readium/cover", type = "image/png", width = 598, height = 800, rels = setOf("cover")),
            res.linkBlocking()
        )

        val bytes = res.readBlocking().getOrNull()
        assertNotNull(bytes)

        assertTrue(BitmapFactory.decodeByteArray(bytes, 0, bytes.size).sameAs(coverBitmap))
    }

    @Test
    fun `helper for ServicesBuilder works fine`() {
        val factory = { context: Publication.Service.Context ->
            object : CoverService {
                override suspend fun cover(): Bitmap? = null
            }
        }
        assertEquals(
            factory,
            Publication.ServicesBuilder().apply { coverServiceFactory = factory }.coverServiceFactory
        )
    }

    @Test
    fun `cover helper for Publication works fine`() {
        assertTrue(coverBitmap.sameAs(runBlocking { publication.cover() }))
    }

    @Test
    fun `coverFitting helper for Publication works fine`() {
        val scaled = runBlocking { publication.coverFitting(Size(300, 400)) }
        assertNotNull(scaled)
        assertEquals(400, scaled.height)
        assertEquals(299, scaled.width)
    }
}