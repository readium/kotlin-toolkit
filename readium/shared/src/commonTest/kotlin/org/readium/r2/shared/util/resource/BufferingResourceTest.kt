package org.readium.r2.shared.util.resource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.fromFilePath

class BufferingResourceTest {

    @Test
    fun `get file`() {
        assertEquals(fileUrl, sut().sourceUrl)
    }

    @Test
    fun `get properties`() = runTest {
        assertEquals(resource.properties().checkSuccess(), sut().properties().checkSuccess())
    }

    @Test
    fun `get length`() = runTest {
        assertEquals(161291, sut().length().getOrNull())
    }

    @Test
    fun `read fully`() = runTest {
        testRead(sut())
    }

    @Test
    fun `read fully by chunks smaller than buffer`() = runTest {
        val sut = sut(1024)
        for (start in 0..652L) {
            testRead(sut, (start * 247) until ((start + 1) * 247))
        }
    }

    @Test
    fun `read fully by chunks equal to buffer`() = runTest {
        val sut = sut(247)
        for (start in 0..652L) {
            testRead(sut, (start * 247) until ((start + 1) * 247))
        }
    }

    @Test
    fun `read fully by chunks larger to buffer`() = runTest {
        val sut = sut(100)
        for (start in 0..652L) {
            testRead(sut, (start * 247) until ((start + 1) * 247))
        }
    }

    @Test
    fun `read unbuffered ranges`() = runTest {
        testRead(sut(), 0 until 850L)
        testRead(sut(), 1000 until 2048L)
        testRead(sut(), 160291 until 161291L)
    }

    @Test
    fun `read unbuffered ranges consecutively`() = runTest {
        val sut = sut(1024)
        testRead(sut, 0 until 850L)
        testRead(sut, 1000 until 2048L)
        testRead(sut, 160291 until 161291L)
    }

    @Test
    fun `read buffered ranges`() = runTest {
        val sut = sut(1024)
        testRead(sut, 0 until 850L)
        testRead(sut, 400 until 850L)
        testRead(sut, 500 until 1000L)
        testRead(sut, 1000 until 2048L)
        testRead(sut, 2048 until 4096L)
        testRead(sut, 2048 until 3072L)
        testRead(sut, 1024 until 1079L)
        testRead(sut, 160291 until 161291L)
        testRead(sut, 160300 until 161270L)
    }

    @Test
    fun `read ranges overlapping buffer`() = runTest {
        val sut = sut(1024)

        // Overlapping start
        testRead(sut, 512 until 1000L)
        testRead(sut, 0 until 750L)
        testRead(sut, 1024 until 2048L)
        testRead(sut, 512 until 1500L)

        // Overlapping end
        testRead(sut, 512 until 1000L)
        testRead(sut, 750 until 4096L)
        testRead(sut, 1024 until 2048L)
        testRead(sut, 1500 until 4096L)
    }

    @Test
    fun `read bigger than buffer`() = runTest {
        val sut = sut(1024)
        testRead(sut, 512 until 1024L)
        testRead(sut, 200 until 4096L)
    }

    @Test
    fun `read random ranges`() = runTest(timeout = 5.minutes) {
        val sut = sut(8489)
        for (i in 0..1000) {
            val lowerBound = (0 until 161291L).random()
            val upperBound = (lowerBound until 161291L).random()
            testRead(sut, lowerBound until upperBound)
        }
    }

    private val fixtures = Fixtures("resource")
    private val fileUrl = assertNotNull(
        AbsoluteUrl.fromFilePath(fixtures.path("epub.epub").toString())
    )
    private val data = fixtures.read("epub.epub").toByteArray()
    private val resource = FileResource(fileUrl)

    private fun sut(bufferSize: Int = 1024): BufferingResource =
        BufferingResource(resource, bufferSize = bufferSize)

    private suspend fun testRead(sut: BufferingResource, range: LongRange? = null) {
        val res = sut.read(range)
        val readData = res.getOrNull()
        assertNotNull(readData)
        if (range != null) {
            val expected = data.copyOfRange(range.first.toInt(), range.last.toInt() + 1)
            if (!readData.contentEquals(expected)) {
                fail("data not equal for range $range")
            }
        } else {
            if (!readData.contentEquals(data)) {
                fail("data not equal")
            }
        }
    }
}
