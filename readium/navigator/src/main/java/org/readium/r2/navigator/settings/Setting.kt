/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:Suppress("FunctionName")

package org.readium.r2.navigator.settings

import kotlinx.serialization.json.JsonElement
import org.readium.r2.shared.ExperimentalReadiumApi
import java.text.NumberFormat

/**
 * Represents a single configurable property of a [Configurable] component and holds its current
 * [value].
 *
 * @param key Unique identifier used to serialize [Preferences] to JSON.
 * @param value Current value for this setting.
 * @param coder JSON serializer for the [value]
 * @param validator Ensures the validity of a [V] value.
 * @param activator Ensures that the condition required for this setting to be active are met in the
 * given [Preferences] – e.g. another setting having a certain preference.
 */
@ExperimentalReadiumApi
open class Setting<V>(
    val key: String,
    val value: V,
    internal val coder: SettingCoder<V>,
    private val validator: SettingValidator<V>,
    private val activator: SettingActivator
) : SettingValidator<V> by validator, SettingActivator by activator, SettingCoder<V> by coder {

    companion object {
        inline operator fun <reified V> invoke(
            key: String,
            value: V,
            coder: SettingCoder<V> = SerializerSettingCoder(),
            validator: SettingValidator<V> = IdentitySettingValidator(),
            activator: SettingActivator = NullSettingActivator,
        ) : Setting<V> =
            Setting(key, value, coder, validator, activator)

        // Official setting keys.
        const val BACKGROUND_COLOR = "backgroundColor"
        const val COLUMN_COUNT = "columnCount"
        const val FIT = "fit"
        const val FONT_FAMILY = "fontFamily"
        const val FONT_SIZE = "fontSize"
        const val HYPHENS = "hyphens"
        const val IMAGE_FILTER = "imageFilter"
        const val LANGUAGE = "language"
        const val LETTER_SPACING = "letterSpacing"
        const val LIGATURES = "ligatures"
        const val LINE_HEIGHT = "lineHeight"
        const val ORIENTATION = "orientation"
        const val PAGE_MARGINS = "pageMargins"
        const val PARAGRAPH_INDENT = "paragraphIndent"
        const val PARAGRAPH_SPACING = "paragraphSpacing"
        const val PUBLISHER_STYLES = "publisherStyles"
        const val READING_PROGRESSION = "readingProgression"
        const val SCROLL = "scroll"
        const val SPREAD = "spread"
        const val TEXT_ALIGN = "textAlign"
        const val TEXT_COLOR = "textColor"
        const val TEXT_NORMALIZATION = "textNormalization"
        const val THEME = "theme"
        const val TYPE_SCALE = "typeScale"
        const val VERTICAL_TEXT = "verticalText"
        const val WORD_SPACING = "wordSpacing"

        /**
         * Keys of settings that are tied to a single publication and should not be shared between
         * several publications.
         */
        @ExperimentalReadiumApi
        val PUBLICATION_SETTINGS = listOf(
            LANGUAGE,
            READING_PROGRESSION,
            VERTICAL_TEXT
        ).toTypedArray()
    }

    /**
     * JSON raw representation for the current value.
     */
    private val jsonValue: JsonElement = coder.encode(value)

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
    key: String,
    value: Boolean,
    validator: SettingValidator<Boolean> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : Setting<Boolean>(key, value, SerializerSettingCoder(), validator, activator)

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
    key: String,
    value: V,
    coder: SettingCoder<V>,
    val range: ClosedRange<V>,
    val suggestedSteps: List<V>?,
    val suggestedIncrement: V?,
    val formatValue: (V) -> String,
    validator: SettingValidator<V>,
    activator: SettingActivator
) : Setting<V>(
    key, value,
    coder = coder,
    validator = RangeSettingValidator(range) + validator,
    activator = activator
) {
    companion object {
        @ExperimentalReadiumApi
        inline operator fun <reified V : Comparable<V>> invoke(
            key: String,
            value: V,
            coder: SettingCoder<V> = SerializerSettingCoder(),
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
            key, value, coder, range, suggestedSteps, suggestedIncrement, formatValue, validator,
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
    key: String,
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
    key, value, SerializerSettingCoder(), range, suggestedSteps, suggestedIncrement, formatValue,
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
    key: String,
    value: E,
    val values: List<E>?,
    val formatValue: (E) -> String?,
    coder: SettingCoder<E>,
    validator: SettingValidator<E>,
    activator: SettingActivator
) : Setting<E>(
    key, value, coder, AllowlistSettingValidator(values) + validator, activator
) {
    companion object {
        inline operator fun <reified E> invoke(
            key: String,
            value: E,
            values: List<E>?,
            noinline formatValue: (E) -> String? = { null },
            coder: SettingCoder<E> = SerializerSettingCoder(),
            validator: SettingValidator<E> = IdentitySettingValidator(),
            activator: SettingActivator = NullSettingActivator,
        ) : EnumSetting<E> =
            EnumSetting(key, value, values, formatValue, coder, validator, activator)
    }
}

/**
 * A color [Setting].
 */
@ExperimentalReadiumApi
open class ColorSetting(
    key: String,
    value: Color,
    values: List<Color>? = null,
    formatValue: (Color) -> String? = { null },
    coder: Color.Coder = Color.Coder(),
    validator: SettingValidator<Color> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : EnumSetting<Color>(
    key, value, values, formatValue, coder, validator, activator
)
