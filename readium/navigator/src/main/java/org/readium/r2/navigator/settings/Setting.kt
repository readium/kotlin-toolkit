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
import java.util.*

/**
 * Represents a single configurable property of a [Configurable] component and holds its current
 * [value].
 *
 * @param key Unique identifier used to serialize [Preferences] to JSON.
 * @param value Current value for this setting.
 * @param extras Holds additional metadata specific to this setting type.
 * @param coder JSON serializer for the [value]
 * @param validator Ensures the validity of a [V] value.
 * @param activator Ensures that the condition required for this setting to be active are met in the
 * given [Preferences] – e.g. another setting having a certain preference.
 */
@ExperimentalReadiumApi
class Setting<V, E>(
    val key: String,
    val value: V,
    val extras: E,
    internal val coder: SettingCoder<V>,
    private val validator: SettingValidator<V> = IdentitySettingValidator(),
    private val activator: SettingActivator = NullSettingActivator
) : SettingValidator<V> by validator, SettingActivator by activator, SettingCoder<V> by coder {

    companion object {
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
     * Creates a copy of the [Setting] receiver, after modifying some of its components.
     */
    fun copy(
        value: V? = null,
        extras: E? = null,
        coder: SettingCoder<V>? = null,
        validator: SettingValidator<V>? = null,
        activator: SettingActivator? = null
    ): Setting<V, E> =
        Setting(
            key = this.key,
            value = value ?: this.value,
            extras = extras ?: this.extras,
            coder = coder ?: this.coder,
            validator = validator ?: this.validator,
            activator = activator ?: this.activator
        )

    /**
     * Creates a copy of the [Setting] receiver, after replacing its value with the first valid
     * value taken from the given [Preferences] objects, in order.
     *
     * Each preference is verified using the setting [validator].
     */
    fun copyFirstValidValueFrom(vararg candidates: Preferences, fallback: Setting<V, E> = this): Setting<V, E> =
        copyFirstValidValueFrom(*candidates, fallback = fallback.value)

    /**
     * Creates a copy of the [Setting] receiver, after replacing its value with the first valid
     * value taken from the given [Preferences] objects, in order.
     *
     * Each preference is verified using the setting [validator].
     */
    fun copyFirstValidValueFrom(vararg candidates: Preferences, fallback: V): Setting<V, E> =
        copy(
            value = candidates
                .mapNotNull { candidate -> candidate[this] }
                .firstNotNullOfOrNull(::validate)
                ?: fallback
        )

    override fun equals(other: Any?): Boolean {
        val otherSetting = (other as? Setting<*, *>) ?: return false
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
 * An arbitrary [Setting] without constraint except its type.
 */
@ExperimentalReadiumApi
typealias ValueSetting<V> = Setting<V, Unit>

/**
 * Creates a new [ValueSetting] with the given [value].
 */
@ExperimentalReadiumApi
inline fun <reified V> ValueSetting(
    key: String,
    value: V,
    coder: SettingCoder<V> = SerializerSettingCoder(),
    validator: SettingValidator<V> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : ValueSetting<V> =
    Setting(
        key = key, value = value, extras = Unit,
        coder = coder, validator = validator, activator = activator
    )

/**
 * A boolean [Setting].
 */
@ExperimentalReadiumApi
typealias ToggleSetting = ValueSetting<Boolean>

/**
 * Creates a new [ToggleSetting] with the given [value].
 */
@ExperimentalReadiumApi
fun ToggleSetting(
    key: String,
    value: Boolean,
    validator: SettingValidator<Boolean> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : ToggleSetting =
    Setting(
        key = key, value = value, extras = Unit,
        coder = SerializerSettingCoder(),
        validator = validator, activator = activator
    )

/**
 * A [Setting] whose value is constrained to a range.
 */
@ExperimentalReadiumApi
typealias RangeSetting<V> = Setting<V, RangeExtras<V>>

/**
 * Additional metadata associated with a [RangeSetting].
 */
@ExperimentalReadiumApi
data class RangeExtras<V : Comparable<V>>(
    val range: ClosedRange<V>,
    val suggestedSteps: List<V>?,
    val suggestedIncrement: V?,
    val formatValue: (V) -> String,
)

/**
 * Creates a new [RangeSetting] with the given [value].
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
inline fun <reified V : Comparable<V>> RangeSetting(
    key: String,
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
) : RangeSetting<V> =
    Setting(
        key = key, value = value,
        extras = RangeExtras(
            range = range,
            suggestedSteps = suggestedSteps,
            suggestedIncrement = suggestedIncrement,
            formatValue = formatValue
        ),
        coder = SerializerSettingCoder(),
        validator = RangeSettingValidator(range) then validator,
        activator = activator
    )

/**
 * Returns a user-facing description for the given [RangeSetting] value.
 */
@ExperimentalReadiumApi
fun <V : Comparable<V>> RangeSetting<V>.formatValue(value: V): String =
    extras.formatValue(value)

/**
 * A [RangeSetting] representing a percentage from 0.0 to 1.0.
 */
@ExperimentalReadiumApi
typealias PercentSetting = Setting<Double, RangeExtras<Double>>

/**
 * Creates a new [PercentSetting] with the given [value].
 *
 * @param range The valid range for the setting value.
 * @param suggestedSteps Value steps which can be used to decrement or increment the setting. It
 * @param suggestedIncrement Suggested value increment which can be used to decrement or increment
 * the setting.
 * MUST be sorted in increasing order.
 */
@ExperimentalReadiumApi
fun PercentSetting(
    key: String,
    value: Double,
    range: ClosedRange<Double> = 0.0..1.0,
    suggestedSteps: List<Double>? = null,
    suggestedIncrement: Double? = 0.1,
    validator: SettingValidator<Double> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator
) : PercentSetting =
    RangeSetting(
        key = key, value = value, range = range,
        suggestedSteps = suggestedSteps, suggestedIncrement = suggestedIncrement,
        formatValue = { v ->
            NumberFormat.getPercentInstance().run {
                maximumFractionDigits = 0
                format(v)
            }
        },
        validator = validator, activator = activator
    )

/**
 * A [Setting] whose value is a member of the enum [E].
 */
@ExperimentalReadiumApi
typealias EnumSetting<E> = Setting<E, EnumExtras<E>>

/**
 * Additional metadata associated with an [EnumSetting].
 */
@ExperimentalReadiumApi
data class EnumExtras<E>(
    val values: List<E>?,
    val formatValue: (E) -> String?,
    val originalValidator: SettingValidator<E>,
)

/**
 * Creates a new [EnumSetting] with the given [value].
 *
 * @param values List of valid [E] values for this setting. Not all members of the enum are
 * necessary supported.
 * @param formatValue Returns a user-facing description for the given value, when one is available.
 */
@ExperimentalReadiumApi
inline fun <reified E> EnumSetting(
    key: String,
    value: E,
    values: List<E>?,
    noinline formatValue: (E) -> String? = { null },
    coder: SettingCoder<E> = SerializerSettingCoder(),
    validator: SettingValidator<E> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : EnumSetting<E> =
    Setting(
        key = key, value = value,
        extras = EnumExtras(
            values = values,
            formatValue = formatValue,
            originalValidator = validator
        ),
        coder = coder,
        validator = AllowlistSettingValidator(values) then validator,
        activator = activator
    )

/**
 * Creates a copy of this [EnumSetting] after replacing its [values] and [coder].
 */
@ExperimentalReadiumApi
fun <E> EnumSetting<E>.copy(
    values: List<E>? = this.values,
    coder: SettingCoder<E> = this.coder,
): EnumSetting<E> =
    copy(
        coder = coder,
        extras = extras.copy(values = values),
        validator = AllowlistSettingValidator(values) then extras.originalValidator
    )

/**
 * List of valid [E] values for this setting. Not all members of the enum are necessary supported.
 */
@ExperimentalReadiumApi
val <E> EnumSetting<E>.values: List<E>?
    get() = extras.values

/**
 * Returns a user-facing description for the given [value], when one is available.
 */
@ExperimentalReadiumApi
fun <E> EnumSetting<E>.formatValue(value: E): String? =
    extras.formatValue(value)

/**
 * A color [Setting].
 */
@ExperimentalReadiumApi
typealias ColorSetting = EnumSetting<Color>

/**
 * Creates a new [ColorSetting] with the given [value].
 */
@ExperimentalReadiumApi
fun ColorSetting(
    key: String,
    value: Color,
    values: List<Color>? = null,
    formatValue: (Color) -> String? = { null },
    coder: Color.Coder = Color.Coder(),
    validator: SettingValidator<Color> = IdentitySettingValidator(),
    activator: SettingActivator = NullSettingActivator,
) : ColorSetting =
    EnumSetting(
        key = key, value = value, values = values, formatValue = formatValue,
        coder = coder, validator = validator, activator = activator
    )
