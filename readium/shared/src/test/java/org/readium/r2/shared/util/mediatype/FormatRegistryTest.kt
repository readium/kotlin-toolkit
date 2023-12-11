package org.readium.r2.shared.util.mediatype

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FormatRegistryTest {

    private fun sut() = FormatRegistry()

    @Test
    fun `get known file extension from canonical media type`() = runBlocking {
        assertEquals(
            "epub",
            sut().fileExtension(MediaType.EPUB)
        )
    }

    @Test
    fun `register new file extensions`() = runBlocking {
        val mediaType = MediaType("application/test")!!
        val sut = sut()
        sut.register(mediaType, fileExtension = "tst", superType = null)

        assertEquals(sut.fileExtension(mediaType), "tst")
    }

    @Test
    fun `register new format with supertype`() = runBlocking {
        val mediaType = MediaType("application/test")!!
        val sut = sut()
        sut.register(mediaType, fileExtension = null, superType = MediaType.ZIP)

        assertEquals(sut.superType(mediaType), MediaType.ZIP)
    }
}
