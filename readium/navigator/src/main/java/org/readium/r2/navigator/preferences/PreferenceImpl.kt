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
sealed class Requirement(
    val isSatisfied: () -> Boolean,
)

@InternalReadiumApi
class EnforceableRequirement(
    isSatisfied: () -> Boolean,
    val enforce: () -> Unit
) : Requirement(isSatisfied) {

    operator fun plus(other: EnforceableRequirement) =
        EnforceableRequirement(
            isSatisfied = { isSatisfied() && other.isSatisfied() },
            enforce = { enforce(); other.enforce() }
        )
}

@InternalReadiumApi
class NonEnforceableRequirement(
    isSatisfied: () -> Boolean,
) : Requirement(isSatisfied) {

    operator fun plus(other: NonEnforceableRequirement) =
        NonEnforceableRequirement(
            isSatisfied = { isSatisfied() && other.isSatisfied() }
        )

    fun or(other: NonEnforceableRequirement) =
        NonEnforceableRequirement { isSatisfied() || other.isSatisfied() }
}

@InternalReadiumApi
fun interface Formatter<T> {

    fun format(value: T): String
}

@InternalReadiumApi
open class PreferenceImpl<T>(
    override var value: T?,
    override val effectiveValue: T,
    private val nonEnforceableRequirement: NonEnforceableRequirement? = null,
    private val enforceableRequirement: EnforceableRequirement? = null,
) : Preference<T> {

    override val isEffective: Boolean
        get() = (nonEnforceableRequirement?.isSatisfied?.invoke() ?: true) &&
            (enforceableRequirement?.isSatisfied?.invoke() ?: true )

    /* override val activator: Activator? =
        Activator { enforceableRequirement?.enforce?.invoke() }
            .takeIf { nonEnforceableRequirement == null } */
}

@InternalReadiumApi
class EnumPreferenceImpl<T>(
    value: T?,
    effectiveValue: T,
    override val supportedValues: List<T>,
    nonEnforceableRequirement: NonEnforceableRequirement? = null,
    enforceableRequirement: EnforceableRequirement? = null,
) : PreferenceImpl<T>(value, effectiveValue, nonEnforceableRequirement, enforceableRequirement),
    EnumPreference<T> {

    override var value: T? = value
        set(value) {
            require(value == null || value in supportedValues)
            field = value
        }
    }

@InternalReadiumApi
class SwitchPreferenceImpl(
    value: Boolean?,
    effectiveValue: Boolean,
    nonEnforceableRequirement: NonEnforceableRequirement? = null,
    enforceableRequirement: EnforceableRequirement? = null,
) : PreferenceImpl<Boolean>(value, effectiveValue, nonEnforceableRequirement, enforceableRequirement),
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
    private val valueFormatter: Formatter<T>,
    override val supportedRange: ClosedRange<T>,
    private val progressionStrategy: ProgressionStrategy<T>,
    nonEnforceableRequirement: NonEnforceableRequirement? = null,
    enforceableRequirement: EnforceableRequirement? = null,
) : PreferenceImpl<T>(value, effectiveValue, nonEnforceableRequirement, enforceableRequirement), RangePreference<T>  {

    override var value: T? = value
        set(value) {
            require(value == null || value in supportedRange)
            field = value
        }

    override fun formatValue(value: T): String =
        valueFormatter.format(value)

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
