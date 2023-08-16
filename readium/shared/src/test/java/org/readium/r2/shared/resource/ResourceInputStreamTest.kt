package org.readium.r2.shared.resource

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResourceInputStreamTest {

    private val file = File(
        assertNotNull(ResourceInputStreamTest::class.java.getResource("epub.epub")?.path)
    )
    private val fileContent: ByteArray = file.readBytes()
    private val bufferSize = 16384 // This is the size used by NanoHTTPd for chunked responses

    @Test
    fun `stream can be read by chunks`() {
        val resource = FileResource(file, mediaType = null)
        val resourceStream = ResourceInputStream(resource)
        val outputStream = ByteArrayOutputStream(fileContent.size)
        resourceStream.copyTo(outputStream, bufferSize = bufferSize)
        assertTrue(fileContent.contentEquals(outputStream.toByteArray()))
    }
}
