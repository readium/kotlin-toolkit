/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@JvmInline
public value class CssSelector(
    public val value: String,
)

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

@ExperimentalReadiumApi
public data class TextQuote(
    val text: String,
    val prefix: String,
    val suffix: String,
)
