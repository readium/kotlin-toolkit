/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.session

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import java.util.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class TtsTimeline(
    private val mediaItems: List<MediaItem>,
) : Timeline() {

    private val uuids = mediaItems.indices
        .map { UUID.randomUUID() }

    override fun getWindowCount(): Int {
        return mediaItems.size
    }

    override fun getWindow(
        windowIndex: Int,
        window: Window,
        defaultPositionProjectionUs: Long,
    ): Window {
        window.uid = uuids[windowIndex]
        window.firstPeriodIndex = windowIndex
        window.lastPeriodIndex = windowIndex
        window.mediaItem = mediaItems[windowIndex]
        window.isSeekable = false
        return window
    }

    override fun getPeriodCount(): Int {
        return mediaItems.size
    }

    override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
        period.windowIndex += periodIndex
        if (setIds) {
            period.uid = uuids[periodIndex]
        }
        return period
    }

    override fun getIndexOfPeriod(uid: Any): Int {
        return uuids.indexOfFirst { it == uid }
            .takeUnless { it == -1 }
            ?: C.INDEX_UNSET
    }

    override fun getUidOfPeriod(periodIndex: Int): Any {
        return uuids[periodIndex]
    }
}
