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
        sut.register(mediaType, fileExtension = "tst")

        assertEquals(sut.fileExtension(mediaType), "tst")
    }
}
