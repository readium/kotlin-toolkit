package org.readium.r2.shared.util.mediatype

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.readium.r2.shared.util.format.FileExtension
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatInfo
import org.readium.r2.shared.util.format.FormatRegistry

class FormatRegistryTest {

    private fun sut() = FormatRegistry()

    @Test
    fun `get known file extension from format`() = runBlocking {
        assertEquals(
            "epub",
            sut()[Format.EPUB]?.fileExtension?.value
        )
    }

    @Test
    fun `get known media type from format`() = runBlocking {
        assertEquals(
            "application/epub+zip",
            sut()[Format.EPUB]?.mediaType.toString()
        )
    }

    @Test
    fun `register new format`() = runBlocking {
        val mediaType = MediaType("application/test")!!
        val sut = sut()
        val format = Format("tst")
        val formatInfo = FormatInfo(
            mediaType = mediaType,
            fileExtension = FileExtension("tst")
        )
        sut.register(format, formatInfo)
        assertEquals(sut[format], formatInfo)
    }
}
