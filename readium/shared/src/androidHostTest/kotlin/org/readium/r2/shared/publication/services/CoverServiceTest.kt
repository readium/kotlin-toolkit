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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.toAbsoluteUrl
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoverServiceTest {

    private val coverBytes: ByteArray
    private val coverBitmap: Bitmap
    private val coverPath: AbsoluteUrl
    private val coverLink: Link
    private val publication: Publication

    init {
        val cover = CoverServiceTest::class.java.getResource("cover.jpg")
        assertNotNull(cover)
        coverBytes = cover.readBytes()
        coverBitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
        coverPath = cover.toAbsoluteUrl()!!
        coverLink = Link(
            href = Href(coverPath),
            mediaType = MediaType.JPEG,
            width = 598,
            height = 800
        )

        publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    localizedTitle = LocalizedString("title")
                ),
                resources = listOf(
                    Link(href = Href(coverPath), rels = setOf("cover"))
                )
            ),
            container = SingleResourceContainer(
                coverPath,
                FileResource(coverPath.toFile()!!)
            )
        )
    }

    @Test
    fun `helper for ServicesBuilder works fine`() {
        val factory = { _: Publication.Service.Context ->
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
