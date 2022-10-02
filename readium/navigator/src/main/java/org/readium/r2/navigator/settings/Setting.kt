/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:Suppress("FunctionName")

package org.readium.r2.navigator.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.readium.r2.shared.ExperimentalReadiumApi
import java.text.NumberFormat

/**
 * Represents a single configurable property of a [Configurable] component and holds its current
 * [value].
 *
 * @param key Serializable unique identifier used to serialize [Preferences] to JSON.
 * @param value Current value for this setting.
 * @param validator Ensures the validity of a [V] value.
 * @param activator Ensures that the condition required for this setting to be active are met in the
 * given [Preferences] – e.g. another setting having a certain preference.
 */
@ExperimentalReadiumApi
open class Setting<V>(
    val key: Key<V>,
    val value: V,
    private val validator: SettingValidator<V>,
    private val activator: SettingActivator
) : SettingValidator<V> by validator, SettingActivator by activator, SettingCoder<V> by key.coder {

    /**
     *  @param id Unique identifier used to serialize [Preferences] to JSON.
     *  @param coder JSON serializer for the [value]
     */
    class Key<V>(
        val id: String,
        val coder: SettingCoder<V>
    ) {

        companion object {

            inline operator fun <reified V> invoke(id: String): Key<V> =
                Key(id = id, coder = SerializerSettingCoder())
        }
    }

    companion object {
        inline operator fun <reified V> invoke(
            key: Key<V>,
            value: V,
            validator: SettingValidator<V> = IdentitySettingValidator(),
            activator: SettingActivator = NullSettingActivator,
        ) : Setting<V> =
            Setting(key, value, validator, activator)
    }

    /**
     * JSON raw representation for the current value.
     */
    private val jsonValue: JsonElement = key.coder.encode(value)

    /**
     * Returns the first valid value taken from the given [Preferences] objects, in order.
     *
     * Each preference is verified using the setting [validator].
     */
    fun firstValidValue(vararg candidates: Preferences): V? =
        candidates
            .mapNotNull { candidate -> candidate[this] }
            .firstNotNullOfOrNull(::validate)

    override fun equals(other: Any?): Boolean {
        val otherSetting = (other as? Setting<*>) ?: return false
        return otherSetting.key == key && otherSetting.jsonValue == jsonValue
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + jsonValue.hashCode()
        return result
    }

    override fun toString(): String =
        "${javaClass.simpleName}($key, $value)"
}

/**
 * A boolean [Setting].
 */
@ExperimentalReadiumApi
open class ToggleSetting(
    key: Key<Boolean>,
    value: Boolean,
    validator: SettingValidator<Boolean> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : Setting<Boolean>(key, value, validator, activator)

/**
 * A [Setting] whose value is constrained to a range.
 *
 * @param range The valid range for the setting value.
 * @param suggestedSteps Value steps which can be used to decrement or increment the setting. It
 * MUST be sorted in increasing order.
 * @param suggestedIncrement Suggested value increment which can be used to decrement or increment
 * the setting.
 * @param formatValue Returns a user-facing description for the given value. This can be used to
 * format the value unit.
 */
@ExperimentalReadiumApi
open class RangeSetting<V : Comparable<V>>(
    key: Key<V>,
    value: V,
    val range: ClosedRange<V>,
    val suggestedSteps: List<V>?,
    val suggestedIncrement: V?,
    val formatValue: (V) -> String,
    validator: SettingValidator<V>,
    activator: SettingActivator
) : Setting<V>(
    key, value,
    validator = RangeSettingValidator(range) + validator,
    activator = activator
) {
    companion object {
        @ExperimentalReadiumApi
        inline operator fun <reified V : Comparable<V>> invoke(
            key: Key<V>,
            value: V,
            range: ClosedRange<V>,
            suggestedSteps: List<V>? = null,
            suggestedIncrement: V? = null,
            noinline formatValue: (V) -> String = { v ->
                when (v) {
                    is Number -> NumberFormat.getNumberInstance().run {
                        maximumFractionDigits = 5
                        format(v)
                    }
                    else -> v.toString()
                }
            },
            validator: SettingValidator<V> = IdentitySettingValidator(),
            activator: SettingActivator = NullSettingActivator,
        ) : RangeSetting<V> = RangeSetting(
            key, value, range, suggestedSteps, suggestedIncrement, formatValue, validator,
            activator
        )
    }
}

/**
 * A [RangeSetting] representing a percentage from 0.0 to 1.0.
 *
 * @param range The valid range for the setting value.
 * @param suggestedSteps Value steps which can be used to decrement or increment the setting. It
 * @param suggestedIncrement Suggested value increment which can be used to decrement or increment
 * the setting.
 * MUST be sorted in increasing order.
 */
@ExperimentalReadiumApi
open class PercentSetting(
    key: Key<Double>,
    value: Double,
    range: ClosedRange<Double> = 0.0..1.0,
    suggestedSteps: List<Double>? = null,
    suggestedIncrement: Double? = 0.1,
    formatValue: (Double) -> String = { v ->
        NumberFormat.getPercentInstance().run {
            maximumFractionDigits = 0
            format(v)
        }
    },
    validator: SettingValidator<Double> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator
) : RangeSetting<Double>(
    key, value, range, suggestedSteps, suggestedIncrement, formatValue,
    validator, activator
)

/**
 * A [Setting] whose value is a member of the enum [E].
 *
 * @param values List of valid [E] values for this setting. Not all members of the enum are
 * necessary supported.
 * @param formatValue Returns a user-facing description for the given value, when one is available.
 */
@ExperimentalReadiumApi
open class EnumSetting<E>(
    key: Key<E>,
    value: E,
    val values: List<E>?,
    val formatValue: (E) -> String?,
    validator: SettingValidator<E>,
    activator: SettingActivator
) : Setting<E>(
    key, value,AllowlistSettingValidator(values) + validator, activator
) {
    companion object {
        inline operator fun <reified E> invoke(
            key: Key<E>,
            value: E,
            values: List<E>?,
            noinline formatValue: (E) -> String? = { null },
            validator: SettingValidator<E> = IdentitySettingValidator(),
            activator: SettingActivator = NullSettingActivator,
        ) : EnumSetting<E> =
            EnumSetting(key, value, values, formatValue, validator, activator)
    }
}

/**
 * A color [Setting].
 */
@ExperimentalReadiumApi
open class ColorSetting(
    key: Key<Color>,
    value: Color,
    values: List<Color>? = null,
    formatValue: (Color) -> String? = { null },
    validator: SettingValidator<Color> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : EnumSetting<Color>(
    key, value, values, formatValue, validator, activator
)
