/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.serialization.json.JsonElement
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
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
    private val validator: SettingValidator<V> = IdentitySettingValidator(),
    private val activator: SettingActivator = NullSettingActivator
) : SettingValidator<V> by validator, SettingActivator by activator {

    class Key<V> @InternalReadiumApi constructor(
        val id: String,
        private val coder: SettingCoder<V>
    ) : SettingCoder<V> by coder {

        companion object {

            /**
             *  @param id Unique identifier used to serialize [Preferences] to JSON.
             */
            inline operator fun <reified V> invoke(id: String): Key<V> =
                Key(id = id, coder = SerializerSettingCoder())
        }
    }

    /**
     * JSON raw representation for the current value.
     */
    private val jsonValue: JsonElement = key.encode(value)

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
typealias ToggleSetting = Setting<Boolean>

/**
 * A [Setting] whose value is constrained to a range.
 *
 * @param range The valid range for the setting value.
 * @param suggestedProgression Suggested progression strategy.
 * @param formatValue Returns a user-facing description for the given value. This can be used to
 * format the value unit.
 */
@ExperimentalReadiumApi
open class RangeSetting<V : Comparable<V>>(
    key: Key<V>,
    value: V,
    val range: ClosedRange<V>,
    val suggestedProgression: ProgressionStrategy<V>? = null,
    val formatValue: (V) -> String = { v ->
        when (v) {
            is Number -> NumberFormat.getNumberInstance().run {
                maximumFractionDigits = 5
                format(v)
            }
            else -> v.toString()
        }
    },
    activator: SettingActivator = NullSettingActivator
) : Setting<V>(
    key, value,
    validator = RangeSettingValidator(range),
    activator = activator
)

/**
 * A [RangeSetting] representing a percentage from 0.0 to 1.0.
 *
 * @param range The valid range for the setting value.
 * @param suggestedProgression Suggested progression strategy.
 */
@ExperimentalReadiumApi
open class PercentSetting(
    key: Key<Double>,
    value: Double,
    range: ClosedRange<Double> = 0.0..1.0,
    suggestedProgression: ProgressionStrategy<Double> = DoubleIncrement(0.1),
    formatValue: (Double) -> String = { v ->
        NumberFormat.getPercentInstance().run {
            maximumFractionDigits = 0
            format(v)
        }
    },
    activator: SettingActivator = NullSettingActivator
) : RangeSetting<Double>(
    key, value, range,
    suggestedProgression, formatValue, activator
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
    val formatValue: (E) -> String? = { null },
    activator: SettingActivator = NullSettingActivator
) : Setting<E>(
    key, value,
    validator = AllowlistSettingValidator(values),
    activator = activator
)
