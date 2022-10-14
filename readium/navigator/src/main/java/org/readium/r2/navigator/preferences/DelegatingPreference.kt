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
fun interface IsActive {

    fun isActive(): Boolean

    operator fun plus(other: IsActive): IsActive = IsActive {
        isActive() && other.isActive()
    }
}

@InternalReadiumApi
fun interface Activate {

    fun activate()

    operator fun plus(other: Activate) = Activate {
        activate()
        other.activate()
    }
}

@InternalReadiumApi
fun interface Formatter<T> {

    fun format(value: T): String
}

@InternalReadiumApi
open class DelegatingPreference<T>(
    override var value: T?,
    override val effectiveValue: T,
    private val isActiveImpl: IsActive,
    private val activateImpl: Activate,
) : Preference<T> {

    override val isActive: Boolean
        get() = isActiveImpl.isActive()

    fun activate() =
        activateImpl.activate()
}

@InternalReadiumApi
class DelegatingEnumPreference<T>(
    value: T?,
    effectiveValue: T,
    isActiveImpl: IsActive,
    activateImpl: Activate,
    override val supportedValues: List<T>
) : DelegatingPreference<T>(value, effectiveValue, isActiveImpl, activateImpl),
    EnumPreference<T> {

    override var value: T? = value
        set(value) {
            require(value in supportedValues)
            field = value
        }
    }

@InternalReadiumApi
class DelegatingSwitchPreference(
    value: Boolean?,
    effectiveValue: Boolean,
    isActiveImpl: IsActive,
    activateImpl: Activate,
) : DelegatingPreference<Boolean>(value, effectiveValue, isActiveImpl, activateImpl),
    SwitchPreference {

    override fun toggle() {
        val currentValue = value ?: effectiveValue
        value = !currentValue
    }
}

@InternalReadiumApi
class DelegatingRangePreference<T: Comparable<T>>(
    value: T?,
    effectiveValue: T,
    isActiveImpl: IsActive,
    activateImpl: Activate,
    private val formatValueImpl: Formatter<T>,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>
) : DelegatingPreference<T>(value, effectiveValue, isActiveImpl, activateImpl),
    RangePreference<T> {

    override var value: T? = value
        set(value) {
            require(value == null || value in supportedRange)
            field = value
        }

    override fun increment() {
        val currentValue = value ?: effectiveValue
        value = progressionStrategy.increment(currentValue)
            .coerceIn(supportedRange)
    }

    override fun decrement() {
        val currentValue = value ?: effectiveValue
        value = progressionStrategy.decrement(currentValue)
            .coerceIn(supportedRange)
    }
    override fun formatValue(value: T): String =
        formatValueImpl.format(value)
}
