package org.readium.r2.shared.resource

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

class ResourceInputStreamTest {

    private val fileContent: ByteArray
    private val fetcher: Fetcher
    private val bufferSize = 16384 // This is the size used by NanoHTTPd for chunked responses

    init {
        val resource = ResourceInputStreamTest::class.java.getResource("epub.epub")
        assertNotNull(resource)
        fileContent = resource.openStream().readBytes()
        val fileFetcher = runBlocking { FileFetcher("/epub.epub", File(resource.path), MediaTypeRetriever()) }
        assertNotNull(fileFetcher)
        fetcher = fileFetcher
    }

    @Test
    fun `stream can be read by chunks`() {
        val resourceStream = ResourceInputStream(fetcher.get("/epub.epub"))
        val outputStream = ByteArrayOutputStream(fileContent.size)
        resourceStream.copyTo(outputStream, bufferSize = bufferSize)
        assertTrue(fileContent.contentEquals(outputStream.toByteArray()))
    }
}
