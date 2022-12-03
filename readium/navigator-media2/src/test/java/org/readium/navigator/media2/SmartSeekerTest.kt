package org.readium.navigator.media2

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTime::class)
class SmartSeekerTest {

    private val playlist: List<Duration> = listOf(
        10, 20, 15, 800, 10, 230, 20, 10
    ).map { it.seconds }

    private val forwardOffset = 50.seconds

    private val backwardOffset = -50.seconds

    @Test
    fun `seek forward within current item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = 200.seconds,
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(3, 250.seconds), result)
    }

    @Test
    fun `seek backward within current item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = 200.seconds,
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(3, 150.seconds), result)
    }

    @Test
    fun `seek forward across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = 780.seconds,
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(5, 20.seconds), result)
    }

    @Test
    fun `seek backward across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = 10.seconds,
            currentIndex = 3,
            playlist
        )
        assertEquals(SmartSeeker.Result(0, 5.seconds), result)
    }

    @Test
    fun `positive offset too big within last item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = 5.seconds,
            currentIndex = 7,
            playlist
        )
        assertEquals(SmartSeeker.Result(7, 10.seconds), result)
    }

    @Test
    fun `positive offset too big across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = forwardOffset,
            currentPosition = 220.seconds,
            currentIndex = 6,
            playlist
        )
        assertEquals(SmartSeeker.Result(7, 10.seconds), result)
    }

    @Test
    fun `negative offset too small within first item`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = 5.seconds,
            currentIndex = 0,
            playlist
        )
        assertEquals(SmartSeeker.Result(0, 0.seconds), result)
    }

    @Test
    fun `negative offset too small across items`() {
        val result = SmartSeeker.dispatchSeek(
            offset = backwardOffset,
            currentPosition = 10.seconds,
            currentIndex = 2,
            playlist
        )
        assertEquals(SmartSeeker.Result(0, 0.seconds), result)
    }
}
