package org.readium.r2.shared.util.resource

import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
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

    @Test
    fun `a readable with an unknown length can still be streamed`() {
        // An HTTP response streamed with chunked transfer encoding has no known length.
        val content = ByteArray(50000) { it.toByte() }
        val readable = UnknownLengthReadable(content)
        val stream = readable.asInputStream()

        // `available` cannot fail when the length is unknown; it returns 0 as permitted by the
        // InputStream contract.
        assertEquals(0, stream.available())

        val outputStream = ByteArrayOutputStream()
        stream.copyTo(outputStream, bufferSize = bufferSize)
        assertTrue(content.contentEquals(outputStream.toByteArray()))
    }

    /**
     * A forward-readable fake whose length is unknown, like an HTTP response body without a
     * Content-Length header.
     */
    private class UnknownLengthReadable(
        private val content: ByteArray,
    ) : Readable {

        override suspend fun length(): Try<Long, ReadError> =
            Try.failure(ReadError.UnsupportedOperation(DebugError("Length is unknown.")))

        override suspend fun stream(
            range: LongRange?,
            consume: (ByteArray) -> Unit,
        ): Try<Unit, ReadError> {
            if (range == null) {
                consume(content)
                return Try.success(Unit)
            }
            val start = range.first.coerceIn(0L, content.size.toLong()).toInt()
            val endExclusive = (range.last + 1).coerceIn(start.toLong(), content.size.toLong()).toInt()
            consume(content.copyOfRange(start, endExclusive))
            return Try.success(Unit)
        }

        override fun close() {}
    }
}
