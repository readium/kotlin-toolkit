package org.readium.navigator.media2

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SmartSeekerTest {

    private val playlist: List<Duration> = listOf(
        10, 20, 15, 800, 10, 230, 20, 10
    ).map { Duration.seconds(it) }

    private val forwardOffset = Duration.seconds(50)

    private val backwardOffset = Duration.seconds(-50)

    @Test
    fun `seek forward within current item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = Duration.seconds(200),
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(3, Duration.seconds(250)), result)
    }

    @Test
    fun `seek backward within current item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = Duration.seconds(200),
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(3, Duration.seconds(150)), result)
    }

    @Test
    fun `seek forward across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = Duration.seconds(780),
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(5, Duration.seconds(20)), result)
    }

    @Test
    fun `seek backward across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = Duration.seconds(10),
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(0, Duration.seconds(5)), result)
    }

    @Test
    fun `positive offset too big within last item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = Duration.seconds(5),
            currentIndex = 7,
            playlist
        )
        assertEquals(SmartSeeker.Result(7, Duration.seconds(10)), result)
    }

    @Test
    fun `positive offset too big across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = Duration.seconds(220),
            currentIndex = 6,
            playlist
        )
        assertEquals(SmartSeeker.Result(7, Duration.seconds(10)), result)
    }

    @Test
    fun `negative offset too small within first item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = Duration.seconds(5),
            currentIndex = 0,
            playlist
        )
        assertEquals(SmartSeeker.Result(0, Duration.seconds(0)), result)
    }

    @Test
    fun `negative offset too small across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = Duration.seconds(10),
            currentIndex = 2,
            playlist
        )
        assertEquals(SmartSeeker.Result(0, Duration.seconds(0)), result)
    }
}