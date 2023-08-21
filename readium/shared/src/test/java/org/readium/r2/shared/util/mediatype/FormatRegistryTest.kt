package org.readium.r2.shared.util.mediatype

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FormatRegistryTest {

    private fun sut() = FormatRegistry(DefaultMediaTypeSniffer())

    @Test
    fun `canonicalize media type`() = runBlocking {
        assertEquals(
            MediaType("text/html")!!,
            sut().canonicalize(MediaType("text/html;charset=utf-8")!!)
        )
        assertEquals(
            MediaType("application/atom+xml;profile=opds-catalog")!!,
            sut().canonicalize(
                MediaType("application/atom+xml;profile=opds-catalog;charset=utf-8")!!
            )
        )
        assertEquals(
            MediaType("application/unknown;charset=utf-8")!!,
            sut().canonicalize(MediaType("application/unknown;charset=utf-8")!!)
        )
    }

    @Test
    fun `get known format from canonical media type`() = runBlocking {
        assertEquals(
            Format(name = "EPUB", fileExtension = "epub"),
            sut().retrieve(MediaType("application/epub+zip")!!)
        )
    }

    @Test
    fun `get known format from non-canonical media type`() = runBlocking {
        assertEquals(
            Format(name = "EPUB", fileExtension = "epub"),
            sut().retrieve(MediaType("application/epub+zip;param=value")!!)
        )
    }

    @Test
    fun `register new format`() = runBlocking {
        val mediaType = MediaType("application/test")!!
        val format = Format(name = "Test", fileExtension = "tst")
        val sut = sut()
        sut.register(mediaType, format)

        assertEquals(format, sut.retrieve(mediaType))
    }
}
