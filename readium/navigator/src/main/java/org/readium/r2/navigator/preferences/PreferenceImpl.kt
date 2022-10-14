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
open class PreferenceImpl<T>(
    override var value: T?,
    override val effectiveValue: T,
    private val isActiveDelegate: () -> Boolean,
    private val activateDelegate: () -> Unit,
) : Preference<T> {

    override val isActive: Boolean
        get() = isActiveDelegate()

    fun activate() =
        activateDelegate()
}

@InternalReadiumApi
class EnumPreferenceImpl<T>(
    value: T?,
    effectiveValue: T,
    isActiveDelegate: () -> Boolean,
    activateDelegate: () -> Unit,
    override val supportedValues: List<T>
) : PreferenceImpl<T>(value, effectiveValue, isActiveDelegate, activateDelegate),
    EnumPreference<T> {

    override var value: T? = value
        set(value) {
            require(value in supportedValues)
            field = value
        }
    }

@InternalReadiumApi
class SwitchPreferenceImpl(
    value: Boolean?,
    effectiveValue: Boolean,
    isActiveDelegate: () -> Boolean,
    activateDelegate: () -> Unit,
) : PreferenceImpl<Boolean>(value, effectiveValue, isActiveDelegate, activateDelegate),
    SwitchPreference {

    override fun toggle() {
        val currentValue = value ?: effectiveValue
        value = !currentValue
    }
}

@InternalReadiumApi
class RangePreferenceImpl<T: Comparable<T>>(
    value: T?,
    effectiveValue: T,
    isActiveDelegate: () -> Boolean,
    activateDelegate: () -> Unit,
    private val formatValueDelegate: (T) -> String,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>
) : PreferenceImpl<T>(value, effectiveValue, isActiveDelegate, activateDelegate),
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
        formatValueDelegate(value)
}