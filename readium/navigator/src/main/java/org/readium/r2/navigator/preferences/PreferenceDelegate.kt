/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
public open class PreferenceDelegate<T>(
    private val getValue: () -> T?,
    private val getEffectiveValue: () -> T,
    private val getIsEffective: () -> Boolean,
    private val updateValue: (T?) -> Unit,
) : Preference<T> {

    override val value: T?
        get() = getValue()

    override val effectiveValue: T
        get() = getEffectiveValue()

    override val isEffective: Boolean
        get() = getIsEffective()

    public override fun set(value: T?) {
        updateValue(value)
    }
}

@InternalReadiumApi
public class EnumPreferenceDelegate<T>(
    getValue: () -> T?,
    getEffectiveValue: () -> T,
    getIsEffective: () -> Boolean,
    updateValue: (T?) -> Unit,
    override val supportedValues: List<T>,
) : PreferenceDelegate<T>(getValue, getEffectiveValue, getIsEffective, updateValue),
    EnumPreference<T> {

    override fun set(value: T?) {
        require(value == null || value in supportedValues)
        super.set(value)
    }
}

@InternalReadiumApi
public class RangePreferenceDelegate<T : Comparable<T>>(
    getValue: () -> T?,
    getEffectiveValue: () -> T,
    getIsEffective: () -> Boolean,
    updateValue: (T?) -> Unit,
    private val valueFormatter: (T) -> String,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>,
) : PreferenceDelegate<T>(getValue, getEffectiveValue, getIsEffective, updateValue),
    RangePreference<T> {

    override fun set(value: T?) {
        super.set(value?.coerceIn(supportedRange))
    }

    override fun formatValue(value: T): String =
        valueFormatter.invoke(value)

    override fun increment() {
        val currentValue = value ?: effectiveValue
        set(progressionStrategy.increment(currentValue))
    }

    override fun decrement() {
        val currentValue = value ?: effectiveValue
        set(progressionStrategy.decrement(currentValue))
    }
}

@InternalReadiumApi
public class OptionalRangePreferenceDelegate<T : Comparable<T>>(
    getValue: () -> T?,
    getEffectiveValue: () -> T?,
    getIsEffective: () -> Boolean,
    updateValue: (T?) -> Unit,
    private val defaultValue: T,
    private val valueFormatter: (T) -> String,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>,
) : PreferenceDelegate<T?>(getValue, getEffectiveValue, getIsEffective, updateValue),
    OptionalRangePreference<T> {

    override fun set(value: T?) {
        super.set(value?.coerceIn(supportedRange))
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
