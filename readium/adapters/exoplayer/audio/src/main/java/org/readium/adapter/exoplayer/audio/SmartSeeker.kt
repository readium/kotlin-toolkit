/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Computes relative seeks across playlist items.
 */
@ExperimentalTime
internal object SmartSeeker {

    data class Result(val index: Int, val position: Duration)

    fun dispatchSeek(
        offset: Duration,
        currentPosition: Duration,
        currentIndex: Int,
        playlist: List<Duration>,
    ): Result {
        val currentDuration = playlist[currentIndex]
        val dummyNewPosition = currentPosition + offset

        return when {
            offset == Duration.ZERO -> {
                Result(currentIndex, currentPosition)
            }
            currentDuration > dummyNewPosition && dummyNewPosition > Duration.ZERO -> {
                Result(currentIndex, dummyNewPosition)
            }
            offset.isPositive() && currentIndex == playlist.size - 1 -> {
                Result(currentIndex, playlist[currentIndex])
            }
            offset.isNegative() && currentIndex == 0 -> {
                Result(0, Duration.ZERO)
            }
            offset.isPositive() -> {
                var toDispatch = offset - (currentDuration - currentPosition)
                var index = currentIndex + 1
                while (toDispatch > playlist[index] && index + 1 < playlist.size) {
                    toDispatch -= playlist[index]
                    index += 1
                }
                Result(index, toDispatch.coerceAtMost(playlist[index]))
            }
            else -> {
                var toDispatch = offset + currentPosition
                var index = currentIndex - 1
                while (-toDispatch > playlist[index] && index > 0) {
                    toDispatch += playlist[index]
                    index -= 1
                }
                Result(index, (playlist[index] + toDispatch).coerceAtLeast(Duration.ZERO))
            }
        }
    }
}
