package org.readium.r2.shared.util.resource

import java.io.ByteArrayOutputStream
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.data.asInputStream
import org.readium.r2.shared.util.file.FileResource
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReadableInputStreamAdapterTest {

    private val file = Fixtures("resource").path("epub.epub").toFile()
    private val fileContent: ByteArray = file.readBytes()
    private val bufferSize = 16384 // This is the size used by NanoHTTPd for chunked responses

    @Test
    fun `stream can be read by chunks`() {
        val resource = FileResource(file)
        val resourceStream = resource.asInputStream()
        val outputStream = ByteArrayOutputStream(fileContent.size)
        resourceStream.copyTo(outputStream, bufferSize = bufferSize)
        assertTrue(fileContent.contentEquals(outputStream.toByteArray()))
    }
}
