package org.readium.r2.shared.util.mediatype

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MediaTypeRetrieverTest {

    private val mediaTypeRetriever = MediaTypeRetriever()

    @Test
    fun `canonicalize media type`() = runBlocking {
        assertEquals(
            MediaType.parse("text/html", fileExtension = "html")!!,
            mediaTypeRetriever.canonicalMediaType(MediaType.parse("text/html;charset=utf-8")!!)
        )
        /*assertEquals(
            MediaType.parse("application/atom+xml;profile=opds-catalog")!!,
            mediaTypeRetriever.canonicalMediaType(MediaType.parse("application/atom+xml;profile=opds-catalog;charset=utf-8")!!)
        )
        assertEquals(
            MediaType.parse("application/unknown;charset=utf-8")!!,
            mediaTypeRetriever.canonicalMediaType(MediaType.parse("application/unknown;charset=utf-8")!!.canonicalMediaType())
        )*/
    }
}
