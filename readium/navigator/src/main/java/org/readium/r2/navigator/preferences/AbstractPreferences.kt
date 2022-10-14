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
abstract class AbstractSwitchPreference(
    override val effectiveValue: Boolean
) : SwitchPreference {

    override fun toggle() {
        val currentValue = value ?: effectiveValue
        value = !currentValue
    }
}

@InternalReadiumApi
abstract class AbstractEnumPreference<T>(
    override val effectiveValue: T,
    override val supportedValues: List<T>,
) : EnumPreference<T> {

    abstract fun get(): T?

    abstract fun set(value: T?)

    override var value: T?
        get() = get()
        set(value) {
            require(value == null || value in supportedValues)
            set(value)
        }
}

@InternalReadiumApi
abstract class AbstractRangePreference<T: Comparable<T>>(
    override val effectiveValue: T,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>
) : RangePreference<T> {

    abstract fun get(): T?

    abstract fun set(value: T?)

    override var value: T?
        get() = get()
        set(value) {
            require(value == null || value in supportedRange)
            set(value)
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
}
