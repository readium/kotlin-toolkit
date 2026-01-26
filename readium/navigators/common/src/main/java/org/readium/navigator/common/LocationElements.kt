/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import kotlin.time.Duration
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A CSS selector.
 */
@ExperimentalReadiumApi
@JvmInline
public value class CssSelector(
    public val value: String,
)

/**
 * A progression value, ranging from 0 to 1.
 */
@ExperimentalReadiumApi
@JvmInline
public value class Progression private constructor(
    public val value: Double,
) {

    public companion object {

        public operator fun invoke(value: Double): Progression? =
            value.takeIf { value in 0.0..1.0 }
                ?.let { Progression(it) }
    }
}

/**
 * A position in publication.
 */
@ExperimentalReadiumApi
@JvmInline
public value class Position private constructor(
    public val value: Int,
) {
    public companion object {

        public operator fun invoke(value: Double): Position? =
            value.takeIf { value >= 0 }
                ?.let { Position(it) }
    }
}

/**
 * A [TextQuote] is a short text quote allowing to target a specific range of [text]. [prefix] and
 * [suffix] are useful to give enough context to make the location less ambiguous.
 */
@ExperimentalReadiumApi
public data class TextQuote(
    val text: String,
    val prefix: String,
    val suffix: String,
)

@ExperimentalReadiumApi
public data class TextAnchor(
    val prefix: String,
    val suffix: String,
)

@JvmInline
@ExperimentalReadiumApi
public value class TimeOffset(
    public val value: Duration,
)
