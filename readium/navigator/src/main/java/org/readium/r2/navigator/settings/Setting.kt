/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.util.IdentityValueCoder
import org.readium.r2.shared.util.ValueCoder
import java.text.NumberFormat

@ExperimentalReadiumApi
data class SettingKey<V, R>(
    val key: String,
    private val coder: ValueCoder<V?, R?>
) : ValueCoder<V?, R?> by coder {

    override fun toString(): String = key

    companion object {
        val COLUMN_COUNT = SettingKey<Int>("columnCount")
        val FIT = SettingKey("fit", Fit)
        val FONT = SettingKey("font", Font)
        val FONT_SIZE = SettingKey<Double>("fontSize")
        val ORIENTATION = SettingKey("orientation", Orientation)
        val OVERFLOW = SettingKey("overflow", Overflow)
        val PUBLISHER_STYLES = SettingKey<Boolean>("publisherStyles")
        val READING_PROGRESSION = SettingKey("readingProgression", ReadingProgression)
        val THEME = SettingKey("theme", Theme)

        operator fun <V> invoke(key: String): SettingKey<V, V> =
            SettingKey(key, IdentityValueCoder())
    }
}

@ExperimentalReadiumApi
open class Setting<T, R>(
    val key: SettingKey<T, R>,
    valueCandidates: List<T?>,
    validator: SettingValidator<T> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator
) : SettingValidator<T> by validator, SettingActivator by activator {

    val value: T = requireNotNull(
        valueCandidates.firstNotNullOfOrNull { v ->
            v?.let { validator.validate(it) }
        }
    ) { "No valid value was provided among: $valueCandidates"}

    val encodedValue: R? = key.encode(this.value)

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
open class ToggleSetting(
    key: SettingKey<Boolean, Boolean>,
    valueCandidates: List<Boolean?>,
    validator: SettingValidator<Boolean> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : Setting<Boolean, Boolean>(
    key = key, valueCandidates = valueCandidates,
    validator = validator, activator = activator
)

@OptIn(ExperimentalReadiumApi::class)
open class RangeSetting<T : Comparable<T>>(
    key: SettingKey<T, T>,
    valueCandidates: List<T?>,
    val range: ClosedRange<T>,
    val suggestedSteps: List<T>? = null,
    val label: (T) -> String = { it.toString() },
    validator: SettingValidator<T> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : Setting<T, T>(
    key = key, valueCandidates = valueCandidates,
    validator = RangeSettingValidator(range) then validator,
    activator = activator
)

@ExperimentalReadiumApi
open class PercentSetting(
    key: SettingKey<Double, Double>,
    valueCandidates: List<Double?>,
    range: ClosedRange<Double> = 0.0..1.0,
    suggestedSteps: List<Double>? = null,
    validator: SettingValidator<Double> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator
) : RangeSetting<Double>(
    key = key, valueCandidates = valueCandidates,
    range = range, suggestedSteps = suggestedSteps,
    label = { v ->
        NumberFormat.getPercentInstance().run {
            maximumFractionDigits = 0
            format(v)
        }
    }, validator, activator
)

@OptIn(ExperimentalReadiumApi::class)
open class EnumSetting<E>(
    key: SettingKey<E, String>,
    valueCandidates: List<E?>,
    val values: List<E>,
    validator: SettingValidator<E> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : Setting<E, String>(
    key = key, valueCandidates = valueCandidates,
    validator = AllowedValuesSettingValidator(values) then validator,
    activator = activator
)


@ExperimentalReadiumApi
interface SettingActivator {
    fun isActiveWithPreferences(preferences: Preferences): Boolean
    fun activateInPreferences(preferences: MutablePreferences)
}

@ExperimentalReadiumApi
object PassthroughSettingActivator : SettingActivator {
    override fun isActiveWithPreferences(preferences: Preferences): Boolean = true
    override fun activateInPreferences(preferences: MutablePreferences) {}
}

@ExperimentalReadiumApi
class DependencySettingActivator(
    val requiredValues: Preferences
) : SettingActivator {
    override fun isActiveWithPreferences(preferences: Preferences): Boolean {
        for ((key, value) in requiredValues.values) {
            if (value != preferences.values[key]) {
                return false
            }
        }
        return true
    }

    override fun activateInPreferences(preferences: MutablePreferences) {
        preferences.merge(requiredValues)
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
    override fun validate(value: T): T?  =
        value.coerceIn(range)
}

@ExperimentalReadiumApi
class AllowedValuesSettingValidator<T>(val allowedValues: List<T>) : SettingValidator<T> {
    override fun validate(value: T): T? {
        if (!allowedValues.contains(value))
            return null

        return value
    }
}
