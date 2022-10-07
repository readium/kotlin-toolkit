/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Ensures the validity of a setting value of type [V].
 */
@ExperimentalReadiumApi
interface SettingValidator<V> {

    /**
     * Returns a valid value for the given [value], if possible.
     *
     * For example, a range setting will coerce the value to be in the range.
     */
    fun validate(value: V): V?
}

/**
 * A default [SettingValidator] implementation for values that are always considered valid.
 */
@ExperimentalReadiumApi
class IdentitySettingValidator<V> : SettingValidator<V> {
    override fun validate(value: V): V? = value
}

/**
 * A [SettingValidator] which coerces values to be in the given [range].
 */
@ExperimentalReadiumApi
class RangeSettingValidator<V : Comparable<V>>(val range: ClosedRange<V>) : SettingValidator<V> {
    override fun validate(value: V): V  =
        value.coerceIn(range)
}

/**
 * A [SettingValidator] ensuring that the values are part of a list of [allowedValues].
 *
 * @param defaultValue Value to return for invalid values.
 */
@ExperimentalReadiumApi
class AllowlistSettingValidator<V>(
    val allowedValues: List<V>?,
    val defaultValue: V? = null
) : SettingValidator<V> {
    override fun validate(value: V): V? =
        if (allowedValues == null || allowedValues.contains(value)) value
        else defaultValue
}

/**
 * A [SettingValidator] combining two validators.
 */
@ExperimentalReadiumApi
class CombinedSettingValidator<V>(
    val outer: SettingValidator<V>,
    val inner: SettingValidator<V>
) : SettingValidator<V> {
    override fun validate(value: V): V? =
        inner.validate(value)
            ?.let { outer.validate(it) }
}

/**
 * Combines the [SettingValidator] receiver with the given [other] validator.
 */
@ExperimentalReadiumApi
operator fun <V> SettingValidator<V>.plus(other: SettingValidator<V>): SettingValidator<V> =
    CombinedSettingValidator(this, other)
