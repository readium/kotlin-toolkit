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
import org.readium.r2.shared.util.Url

@InternalReadiumApi
public data class Clip(
    val audioResource: String? = null,
    val fragmentId: String? = null,
    val start: Double? = null,
    val end: Double? = null,
)

@InternalReadiumApi
public data class MediaOverlayNode(
    val text: Url, // an URI possibly finishing by a fragment (textFile#id)
    val audio: Url?, // an URI possibly finishing by a simple timer (audioFile#t=start,end)
    val children: List<MediaOverlayNode> = listOf(),
    val role: List<String> = listOf(),
) : Serializable {

    val audioFile: String?
        get() = audio?.removeFragment()?.path!!
    val audioTime: String?
        get() = audio?.fragment
    val textFile: String
        get() = text.removeFragment().path!!
    val fragmentId: String?
        get() = text.fragment
    val clip: Clip
        get() {
            val audio = audio ?: throw Exception("audio")
            val audioFile = audio.removeFragment().path
            val times = audio.fragment ?: ""
            val (start, end) = parseTimer(times)
            return Clip(audioFile, fragmentId, start, end)
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
