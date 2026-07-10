/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.preferences

import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.ProgressionStrategy

interface OptionalBooleanPreference : Preference<Boolean?> {

    val defaultValue: Boolean
}

/**
 * Toggle the preference value. A default value is taken as the initial one if
 * the preference is currently unset.
 */
fun OptionalBooleanPreference.toggle() {
    set(!(value ?: effectiveValue ?: defaultValue))
}

class OptionalBooleanPreferenceDelegate(
    private val getValue: () -> Boolean?,
    private val getEffectiveValue: () -> Boolean?,
    private val getIsEffective: () -> Boolean,
    private val updateValue: (Boolean?) -> Unit,
    override val defaultValue: Boolean,
) : OptionalBooleanPreference {

    override val value: Boolean?
        get() = getValue()

    override val effectiveValue: Boolean?
        get() = getEffectiveValue()

    override val isEffective: Boolean
        get() = getIsEffective()

    override fun set(value: Boolean?) {
        updateValue(value)
    }
}

/**
 * A [Preference] whose values must be null or in a [ClosedRange] of [T].
 */
interface OptionalRangePreference<T : Comparable<T>> : Preference<T?> {

    val defaultValue: T

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

class OptionalRangePreferenceDelegate<T : Comparable<T>>(
    private val getValue: () -> T?,
    private val getEffectiveValue: () -> T?,
    private val getIsEffective: () -> Boolean,
    private val updateValue: (T?) -> Unit,
    override val defaultValue: T,
    private val valueFormatter: (T) -> String,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>,
) : OptionalRangePreference<T> {

    override val value: T?
        get() = getValue()

    override val effectiveValue: T?
        get() = getEffectiveValue()

    override val isEffective: Boolean
        get() = getIsEffective()

    override fun set(value: T?) {
        updateValue(value?.coerceIn(supportedRange))
    }

    override fun formatValue(value: T): String =
        valueFormatter.invoke(value)

    override fun increment() {
        val currentValue = value ?: effectiveValue
        set(progressionStrategy.increment(currentValue ?: defaultValue))
    }

    override fun decrement() {
        val currentValue = value ?: effectiveValue
        set(progressionStrategy.decrement(currentValue ?: defaultValue))
    }
}
