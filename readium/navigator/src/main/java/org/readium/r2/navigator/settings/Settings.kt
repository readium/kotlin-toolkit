/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.util.IdentityValueCoder
import org.readium.r2.shared.util.ValueCoder
import java.text.NumberFormat

@ExperimentalReadiumApi
interface Configurable<S : Configurable.Settings> {
    interface Settings

    val settings: StateFlow<S>

    /**
     * Submits a new set of Presentation preferences used by the Navigator to recompute its
     * Presentation Settings.
     *
     * Note that the Navigator might not update its presentation right away, or might even ignore
     * some of the provided settings. They are only used as guidelines to compute the Presentation
     * Properties.
     */
    suspend fun applyPreferences(preferences: Preferences)
}

@ExperimentalReadiumApi
data class SettingKey<V, R>(
    val key: String,
    private val coder: ValueCoder<V?, R?>
) : ValueCoder<V?, R?> by coder {

    override fun toString(): String = key

    companion object {
        // FIXME: font size, font, theme, columnCount
        val CONTINUOUS = SettingKey<Boolean>("continuous")
        val FIT = SettingKey("fit", Fit)
        val ORIENTATION = SettingKey("orientation", Orientation)
        val OVERFLOW = SettingKey("overflow", Overflow)
        val READING_PROGRESSION = SettingKey("readingProgression", ReadingProgression)

        operator fun <V> invoke(key: String): SettingKey<V, V> =
            SettingKey(key, IdentityValueCoder())
    }
}

@ExperimentalReadiumApi
open class Setting<T, R>(
    val key: SettingKey<T, R>,
    val value: T,
    validator: SettingValidator<T> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator
) : SettingValidator<T> by validator, SettingActivator by activator {

    val encodedValue: R? get() = key.encode(value)

    override fun equals(other: Any?): Boolean {
        val otherSetting = (other as? Setting<*, *>) ?: return false
        return otherSetting.key == key && otherSetting.encodedValue == encodedValue
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (encodedValue?.hashCode() ?: 0)
        return result
    }
}

@OptIn(ExperimentalReadiumApi::class)
open class RangeSetting<T : Comparable<T>, R>(
    key: SettingKey<T, R>,
    value: T,
    val range: ClosedRange<T>,
    val suggestedSteps: List<T>? = null,
    val label: (Double) -> String = { it.toString() },
    validator: SettingValidator<T> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : Setting<T, R>(
    key = key, value = value,
    validator = RangeSettingValidator(range) then validator,
    activator = activator
)

@ExperimentalReadiumApi
open class PercentSetting(
    key: SettingKey<Double, Double>,
    value: Double,
    suggestedSteps: List<Double>? = null,
    validator: SettingValidator<Double> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator
) : RangeSetting<Double, Double>(
    key = key, value = value,
    range = 0.0..1.0,
    suggestedSteps = suggestedSteps,
    label = { v ->
        NumberFormat.getPercentInstance().run {
            maximumFractionDigits = 0
            format(v)
        }
    }, validator, activator
)

@OptIn(ExperimentalReadiumApi::class)
open class EnumSetting<E : Enum<E>, R>(
    key: SettingKey<E, R>,
    value: E,
    val allowedValues: List<E>?,
    validator: SettingValidator<E> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : Setting<E, R>(
    key = key, value = value,
    validator = AllowedValuesSettingValidator(allowedValues) then validator,
    activator = activator
)


@ExperimentalReadiumApi
interface SettingActivator {
    fun isActiveWithPreferences(preferences: Preferences): Boolean
    fun activateForPreferences(preferences: Preferences): Preferences
}

@ExperimentalReadiumApi
object PassthroughSettingActivator : SettingActivator {
    override fun isActiveWithPreferences(preferences: Preferences): Boolean = true
    override fun activateForPreferences(preferences: Preferences): Preferences = preferences
}

@ExperimentalReadiumApi
class DependencySettingActivator(
    val requiredValues: Preferences
) : SettingActivator {
    override fun isActiveWithPreferences(preferences: Preferences): Boolean {
        val requiredVals = requiredValues.values.filterValues { it != null }
        for ((key, value) in requiredVals) {
            if (value != preferences.values[key]) {
                return false
            }
        }
        return true
    }

    override fun activateForPreferences(preferences: Preferences): Preferences {
        return preferences.merge(requiredValues)
    }
}


@ExperimentalReadiumApi
interface SettingValidator<T> {
    fun validate(value: T): T?
}

@ExperimentalReadiumApi
class IdentitySettingValidator<T> : SettingValidator<T> {
    override fun validate(value: T): T? = value
}

@ExperimentalReadiumApi
class CombinedSettingValidator<T>(
    val outer: SettingValidator<T>,
    val inner: SettingValidator<T>
) : SettingValidator<T> {
    override fun validate(value: T): T? =
        inner.validate(value)
            ?.let { outer.validate(it) }
}

@ExperimentalReadiumApi
infix fun <T> SettingValidator<T>.then(other: SettingValidator<T>): SettingValidator<T> =
    CombinedSettingValidator(this, other)

@ExperimentalReadiumApi
class RangeSettingValidator<T : Comparable<T>>(val range: ClosedRange<T>) : SettingValidator<T> {
    override fun validate(value: T): T? =
        value.coerceIn(range)
}

@ExperimentalReadiumApi
class AllowedValuesSettingValidator<T : Comparable<T>>(val allowedValues: List<T>?) : SettingValidator<T> {
    override fun validate(value: T): T? {
        if (allowedValues != null && !allowedValues.contains(value))
            return null

        return value
    }
}
