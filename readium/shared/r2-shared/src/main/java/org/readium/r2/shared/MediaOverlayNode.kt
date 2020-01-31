/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import java.io.Serializable

data class Clip (
        val audioResource: String? = null,
        val fragmentId: String? = null,
        val start: Double? = null,
        val end: Double? = null
)

data class MediaOverlayNode (
    val text : String, // an URI possibly finishing by a fragment (textFile#id)
    val audio : String?, // an URI possibly finishing by a simple timer (audioFile#t=start,end)
    val children: List<MediaOverlayNode> = listOf(),
    val role: List<String> = listOf()) : Serializable {

    val audioFile: String?
        get() = audio?.split("#")?.first()
    val audioTime: String?
        get() = if (audio != null && '#' in audio) audio.split("#", limit=2).last() else null
    val textFile: String
        get() = text.split("#").first()
    val fragmentId: String?
        get () = if ('#' in text) text.split('#', limit=2).last() else null
    val clip: Clip
        get() {
            val audioString = this.audio ?: throw Exception("audio")
            val audioFileString = audioString.split('#').first()
            val times = audioString.split('#').last()
            val (start, end) = parseTimer(times)
            return Clip(audioFileString, fragmentId, start, end)
        }

    private fun parseTimer(times: String): Pair<Double?, Double?> {
        //  Remove "t=" prefix
        val netTimes = times.removeRange(0, 2)
        val start = netTimes.split(',').first()
        val end = try {
            netTimes.split(',').last()
        } catch (e: Exception) {
            null
        }
        val startTimer = start.toDoubleOrNull()
        val endTimer = end?.toDoubleOrNull()
        return Pair(startTimer, endTimer)
    }

}
