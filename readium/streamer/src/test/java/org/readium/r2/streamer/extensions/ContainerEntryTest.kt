/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceTry
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContainerEntryTest {

    class Entry(path: String) : Container.Entry {
        override val url: Url = Url(path)!!
        override val source: AbsoluteUrl? = null
        override suspend fun mediaType(): ResourceTry<MediaType> =
            throw NotImplementedError()
        override suspend fun properties(): ResourceTry<Resource.Properties> =
            throw NotImplementedError()
        override suspend fun length(): ResourceTry<Long> =
            throw NotImplementedError()
        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            throw NotImplementedError()
        override suspend fun close() {
            throw NotImplementedError()
        }
    }

    @Test
    fun `pathCommonFirstComponent is null when files are in the root`() {
        assertNull(
            listOf(Entry("im1.jpg"), Entry("im2.jpg"), Entry("toc.xml"))
                .pathCommonFirstComponent()
        )
    }

    @Test
    fun `pathCommonFirstComponent is null when files are in different directories`() {
        assertNull(
            listOf(Entry("dir1/im1.jpg"), Entry("dir2/im2.jpg"), Entry("toc.xml"))
                .pathCommonFirstComponent()
        )
    }

    @Test
    fun `pathCommonFirstComponent is correct when there is only one file in the root`() {
        assertEquals(
            "im1.jpg",
            listOf(Entry("im1.jpg")).pathCommonFirstComponent()?.name
        )
    }

    @Test
    fun `pathCommonFirstComponent is correct when all files are in the same directory`() {
        assertEquals(
            "root",
            listOf(
                Entry("root/im1.jpg"),
                Entry("root/im2.jpg"),
                Entry("root/xml/toc.xml")
            ).pathCommonFirstComponent()?.name
        )
    }
}
