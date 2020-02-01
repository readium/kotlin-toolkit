/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

/**
 * Parse clock values as defined by
 * https://www.w3.org/TR/SMIL/smil-timing.html#q22
 */
internal object ClockValueParser {
    fun parse(rawValue: String) : Double? {
        val value = rawValue.trim()
        return if (":" in value) {
            parseClockvalue(value)
        }
        else {
            val metricStart = value.indexOfFirst(Char::isLetter)
            if (metricStart == -1) {
                parseTimecount(value.toDouble(), "")
            } else {
                val count = value.substring(0 until metricStart).toDouble()
                val metric = value.substring(metricStart until value.length)
                parseTimecount(count, metric)
            }
        }
    }

    private fun parseClockvalue(value: String) : Double {
        val parts = value.split(":").map {it.toDouble()}
        val min_sec = parts.last() + parts[parts.size-2] * 60
        return if (parts.size > 2) min_sec + parts[parts.size-3] * 3600 else min_sec
    }

    private fun parseTimecount(value: Double, metric: String) : Double? =
        when (metric) {
        "h" -> value * 3600
        "min" -> value * 60
        "s", "" -> value
        "ms" -> value / 1000
        else -> null
    }
}