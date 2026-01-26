/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Temporal dimension parser for
 * [Media Fragment specification](https://www.w3.org/TR/media-frags/#naming-time).
 *
 * Supports only Normal Play Time specified as seconds at the moment.
 */
public object TemporalFragmentParser {

    public fun parse(value: String): TimeInterval? {
        if (!value.startsWith("t=")) {
            return null
        }

        val nptValue = value.removePrefix("t=").removePrefix("npt:")
        return parseNormalPlayTime(nptValue)
    }

    private fun parseNormalPlayTime(value: String): TimeInterval? {
        val components = value.split(",", limit = 2)

        val startOffset = components.getOrNull(0)
            ?.toDoubleOrNull()
            ?.seconds

        val endOffset = components.getOrNull(1)
            ?.toDoubleOrNull()
            ?.seconds

        return TimeInterval(startOffset, endOffset)
    }
}

public data class TimeInterval(
    val start: Duration?,
    val end: Duration?,
)
