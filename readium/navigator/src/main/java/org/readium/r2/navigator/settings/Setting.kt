/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:Suppress("FunctionName")

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.IdentityValueCoder
import org.readium.r2.shared.util.ValueCoder
import java.text.NumberFormat

@ExperimentalReadiumApi
data class Setting<T, R, E>(
    val key: String,
    val coder: ValueCoder<T?, R?>,
    val value: T,
    val extras: E,
    private val validator: SettingValidator<T> = IdentitySettingValidator(),
    private val activator: SettingActivator = PassthroughSettingActivator
) : SettingValidator<T> by validator, SettingActivator by activator, ValueCoder<T?, R?> by coder {

    companion object {
        // Well-known setting keys
        const val COLUMN_COUNT = "columnCount"
        const val FIT = "fit"
        const val FONT = "font"
        const val FONT_SIZE = "fontSize"
        const val ORIENTATION = "orientation"
        const val OVERFLOW = "overflow"
        const val PUBLISHER_STYLES = "publisherStyles"
        const val READING_PROGRESSION = "readingProgression"
        const val THEME = "theme"
        const val WORD_SPACING = "wordSpacing"
    }

    val encodedValue: R? = coder.encode(this.value)

    fun copyFirstValidValueFrom(vararg candidates: Preferences?): Setting<T, R, E> =
        copyFirstValidValueFrom(*candidates
            .filterNotNull()
            .map { Either.Left<Preferences, T>(it) }
            .toTypedArray()
        )

    // TODO: Useful?
    private fun copyFirstValidValueFrom(vararg candidates: Either<Preferences, T>?): Setting<T, R, E> =
        copy(
            value = candidates
                .filterNotNull()
                .mapNotNull { candidate ->
                    when (candidate) {
                        is Either.Left -> candidate.value[this]
                        is Either.Right -> candidate.value
                    }
                }
                .firstNotNullOfOrNull(::validate)
                ?: value
        )

    override fun equals(other: Any?): Boolean {
        val otherSetting = (other as? Setting<*, *, *>) ?: return false
        return otherSetting.key == key && otherSetting.encodedValue == encodedValue
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (encodedValue?.hashCode() ?: 0)
        return result
    }
}

@ExperimentalReadiumApi
typealias ToggleSetting = Setting<Boolean, Boolean, Unit>

@ExperimentalReadiumApi
fun ToggleSetting(
    key: String,
    value: Boolean,
    validator: SettingValidator<Boolean> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : ToggleSetting =
    Setting(
        key = key, coder = IdentityValueCoder(Boolean::class), value = value, extras = Unit,
        validator = validator, activator = activator
    )

@ExperimentalReadiumApi
typealias RangeSetting<T> = Setting<T, T, RangeExtras<T>>

@ExperimentalReadiumApi
data class RangeExtras<T : Comparable<T>>(
    val range: ClosedRange<T>,
    val suggestedSteps: List<T>?,
    val label: (T) -> String,
)

@ExperimentalReadiumApi
inline fun <reified T : Comparable<T>> RangeSetting(
    key: String,
    value: T,
    range: ClosedRange<T>,
    suggestedSteps: List<T>? = null,
    noinline label: (T) -> String = { it.toString() },
    validator: SettingValidator<T> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : RangeSetting<T> =
    Setting(
        key = key, coder = IdentityValueCoder(T::class), value = value,
        extras = RangeExtras(
            range = range,
            suggestedSteps = suggestedSteps,
            label = label
        ),
        validator = RangeSettingValidator(range) then validator,
        activator = activator
    )

@ExperimentalReadiumApi
fun <T : Comparable<T>> RangeSetting<T>.label(value: T): String =
    extras.label(value)

@ExperimentalReadiumApi
typealias PercentSetting = Setting<Double, Double, RangeExtras<Double>>

@ExperimentalReadiumApi
fun PercentSetting(
    key: String,
    value: Double,
    range: ClosedRange<Double> = 0.0..1.0,
    suggestedSteps: List<Double>? = null,
    validator: SettingValidator<Double> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator
) : PercentSetting =
    RangeSetting(
        key = key, value = value, range = range, suggestedSteps = suggestedSteps,
        label = { v ->
            NumberFormat.getPercentInstance().run {
                maximumFractionDigits = 0
                format(v)
            }
        },
        validator = validator, activator = activator
    )

@ExperimentalReadiumApi
typealias EnumSetting<E> = Setting<E, String, EnumExtras<E>>

@ExperimentalReadiumApi
data class EnumExtras<E>(
    val values: List<E>,
    val label: (E) -> String?,
)

@ExperimentalReadiumApi
fun <E> EnumSetting(
    key: String,
    coder: ValueCoder<E?, String?>,
    value: E,
    values: List<E>,
    label: (E) -> String? = { null },
    validator: SettingValidator<E> = IdentitySettingValidator(),
    activator: SettingActivator = PassthroughSettingActivator,
) : EnumSetting<E> =
    Setting(
        key = key, value = value, coder = coder,
        extras = EnumExtras(
            values = values,
            label = label
        ),
        validator = AllowedValuesSettingValidator(values) then validator,
        activator = activator
    )

@ExperimentalReadiumApi
val <E> EnumSetting<E>.values: List<E>
    get() = extras.values

@ExperimentalReadiumApi
fun <E> EnumSetting<E>.label(value: E): String? =
    extras.label(value)

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
    override fun validate(value: T): T  =
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
