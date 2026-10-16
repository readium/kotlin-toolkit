package org.readium.r2.shared.util.zip

import java.io.IOException
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable

class ReadableChannelAdapterTest {

    @Test
    fun `content whose length exceeds MAX_INT can be read`() {
        val readable = object : Readable {
            override suspend fun length(): Try<Long, ReadError> {
                return Try.success(Int.MAX_VALUE.toLong() + 10)
            }

            override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
                requireNotNull(range)
                val rangeLength = range.last - range.first + 1
                require(rangeLength < Int.MAX_VALUE)

                val content = ByteArray(rangeLength.toInt())
                content.fill(6)

                return Try.success(content)
            }

            override fun close() {
            }
        }

        val adapter = ReadableChannelAdapter(readable) { IOException(ErrorException(it)) }
        val array = ByteArray(8092)
        val buffer = ByteBuffer.wrap(array)
        adapter.read(buffer)

        val expected = ByteArray(8092)
        expected.fill(6)
        assertContentEquals(expected, array)
    }
}
