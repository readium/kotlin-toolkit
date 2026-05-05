/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * An HTML Id.
 */
@ExperimentalReadiumApi
@JvmInline
public value class HtmlId(
    public val value: String,
)

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
) : Comparable<Progression> {

    override fun compareTo(other: Progression): Int {
        return value.compareTo(other.value)
    }

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
) : Comparable<Position> {

    override fun compareTo(other: Position): Int {
        return value.compareTo(other.value)
    }

    public companion object {

        public operator fun invoke(value: Int): Position? =
            value.takeIf { value >= 1 }
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

/**
 * Returns a [TextAnchor] to the beginning or the end of the text quote.
 */
@ExperimentalReadiumApi
public fun TextQuote.toTextAnchor(end: Boolean = false): TextAnchor =
    when (end) {
        false -> TextAnchor(
            textBefore = prefix,
            textAfter = text + suffix
        )
        true -> TextAnchor(
            textBefore = prefix + text,
            textAfter = suffix
        )
    }

/**
 * A [TextAnchor] is a pair of short text snippets allowing to locate in a text.
 */
@ExperimentalReadiumApi
public data class TextAnchor(
    val textBefore: String,
    val textAfter: String,
)

@ExperimentalReadiumApi
public fun Locator.Text.toTextQuote(): TextQuote? =
    highlight?.let { highlight ->
        TextQuote(
            text = highlight,
            prefix = before.orEmpty(),
            suffix = after.orEmpty()
        )
    }

@ExperimentalReadiumApi
public fun Locator.Text.toTextAnchor(end: Boolean = false): TextAnchor? =
    toTextQuote()?.toTextAnchor(end)
