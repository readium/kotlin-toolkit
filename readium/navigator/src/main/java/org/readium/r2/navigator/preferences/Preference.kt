/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A preference holder able to edit it and predict the navigator's behaviour
 * regarding the computation of settings.
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
fun<T> Preference<T>.clear() =
    set(null)

/**
 * A [Preference] which supports a closed set of values.
 */
@ExperimentalReadiumApi
interface EnumPreference<T> : Preference<T> {

    val supportedValues: List<T>
}

/**
 * A [Preference] whose values must be in a [ClosedRange] of [T].
 */
@ExperimentalReadiumApi
interface RangePreference<T: Comparable<T>> : Preference<T> {

    val supportedRange: ClosedRange<T>

    /**
     * Increment the preference value from its current value or the default value.
     */
    fun increment()

    /**
     * Decrement the preference value from its current value or the default value.
     */
    fun decrement()

    /**
     * Format [value] in a suitable way for displaying, including unit when relevant.
     */
    fun formatValue(value: T): String
}

/**
 * A [Boolean] preference.
 */
@ExperimentalReadiumApi
interface SwitchPreference : Preference<Boolean> {

    /**
     * Toggle the preference value. The default value is taken as the initial one if
     * the preference is currently unset.
     */
    fun toggle()
}
