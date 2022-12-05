/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
open class PreferenceDelegate<T>(
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

    override fun set(value: T?) =
        updateValue(value)
}

@InternalReadiumApi
class EnumPreferenceDelegate<T>(
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
class RangePreferenceDelegate<T : Comparable<T>>(
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
