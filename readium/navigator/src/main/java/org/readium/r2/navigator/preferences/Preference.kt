/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A handle to edit the value of a specific preference which is able to predict
 * which value the [Configurable] will effectively use.
 */
@ExperimentalReadiumApi
interface Preference<T> {

    /**
     * The current value of the preference.
     */
    val value: T?

    /**
     * The value that will be effectively used by the navigator
     * if preferences are submitted as they are.
     */
    val effectiveValue: T

    /**
     * If this preference will be effectively used by the navigator
     * if preferences are submitted as they are.
     */
    val isEffective: Boolean

    /**
     * Set the preference to [value]. A null value means unsetting the preference.
     */
    fun set(value: T?)
}

/**
 * Unset the preference.
 */
@ExperimentalReadiumApi
fun <T> Preference<T>.clear() =
    set(null)

/**
 * Toggle the preference value. A default value is taken as the initial one if
 * the preference is currently unset.
 */
@OptIn(ExperimentalReadiumApi::class)
fun Preference<Boolean>.toggle() {
    set(!(value ?: effectiveValue))
}

/**
 * Returns a new preference with its boolean value flipped.
 */
@OptIn(ExperimentalReadiumApi::class)
fun Preference<Boolean>.flipped(): Preference<Boolean> =
    map(from = { !it }, to = { !it })

/**
 * A [Preference] which accepts a closed set of values.
 */
@ExperimentalReadiumApi
interface EnumPreference<T> : Preference<T> {
    /**
     * List of valid values for this preference.
     */
    val supportedValues: List<T>
}

/**
 * A [Preference] whose values must be in a [ClosedRange] of [T].
 */
@ExperimentalReadiumApi
interface RangePreference<T : Comparable<T>> : Preference<T> {

    val supportedRange: ClosedRange<T>

    /**
     * Increment the preference value from its current value or a default value.
     */
    fun increment()

    /**
     * Decrement the preference value from its current value or a default value.
     */
    fun decrement()

    /**
     * Format [value] in a way suitable for display, including unit if relevant.
     */
    fun formatValue(value: T): String
}
