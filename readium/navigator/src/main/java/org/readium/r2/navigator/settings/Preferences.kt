/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import org.readium.r2.navigator.ColumnCount
import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.navigator.settings.SettingKey.Companion.COLUMN_COUNT
import org.readium.r2.navigator.settings.SettingKey.Companion.FIT
import org.readium.r2.navigator.settings.SettingKey.Companion.FONT
import org.readium.r2.navigator.settings.SettingKey.Companion.FONT_SIZE
import org.readium.r2.navigator.settings.SettingKey.Companion.ORIENTATION
import org.readium.r2.navigator.settings.SettingKey.Companion.OVERFLOW
import org.readium.r2.navigator.settings.SettingKey.Companion.PUBLISHER_STYLES
import org.readium.r2.navigator.settings.SettingKey.Companion.READING_PROGRESSION
import org.readium.r2.navigator.settings.SettingKey.Companion.THEME
import org.readium.r2.navigator.settings.SettingKey.Companion.WORD_SPACING
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*

/**
 * Holds a list of key-value pairs provided by the app to influence a [Configurable]'s setting.
 * The keys must be valid [SettingKey] keys.
 */
@ExperimentalReadiumApi
@Parcelize
open class Preferences(
    @InternalReadiumApi open val values: @WriteWith<JSONParceler> Map<String, Any> = emptyMap()
) : JSONable, Parcelable {

    constructor(builder: MutablePreferences.() -> Unit)
        : this(MutablePreferences().apply(builder))

    constructor(mutablePreferences: MutablePreferences)
        : this(mutablePreferences.values.toMap())

    inline operator fun <reified V, reified R> get(setting: Setting<V, R>): V? =
        get(setting.key)

    inline operator fun <reified V, reified R> get(key: SettingKey<V, R>): V? =
        key.decode(values[key.key] as? R)

    fun <T, R> isActive(setting: Setting<T, R>): Boolean =
        setting.isActiveWithPreferences(this)

    fun copy(updates: MutablePreferences.() -> Unit): Preferences =
        Preferences(toMutablePreferences().apply(updates))

    fun toMutablePreferences(): MutablePreferences =
        MutablePreferences(values.toMutableMap())

    override fun toJSON(): JSONObject =
        JSONObject(values)

    override fun equals(other: Any?): Boolean =
        values == (other as? Preferences)?.values

    override fun hashCode(): Int =
        values.hashCode()

    override fun toString(): String =
        toJSON().toString()

    companion object {

        fun fromJSON(json: JSONObject?): Preferences =
            Preferences(json?.toMap() ?: emptyMap())
    }
}

@ExperimentalReadiumApi
@Parcelize
class MutablePreferences(
    @InternalReadiumApi override var values: @WriteWith<JSONParceler> MutableMap<String, Any> = mutableMapOf()
) : Preferences(values = values) {

    inline operator fun <reified V, reified R> set(setting: Setting<V, R>, value: V?) {
        set(setting, value, activate = true)
    }

    inline fun <reified V, reified R> set(setting: Setting<V, R>, value: V?, activate: Boolean = true) {
        set(setting.key, value?.let { setting.validate(it) })
        if (activate) {
            activate(setting)
        }
    }

    inline operator fun <reified V, reified R> set(key: SettingKey<V, R>, value: V?) {
        val encodedValue = key.encode(value)
        if (encodedValue != null) {
            values[key.key] = encodedValue
        } else {
            values.remove(key.key)
        }
    }

    fun clear() {
        values.clear()
    }

    fun merge(other: Preferences) {
        for ((key, value) in other.values) {
            values[key] = value
        }
    }

    inline fun <reified V, reified R> remove(key: SettingKey<V, R>) {
        values.remove(key.key)
    }

    fun toggle(setting: ToggleSetting) {
        set(setting, !(get(setting.key) ?: setting.value))
    }

    fun increment(setting: RangeSetting<Double>, step: Double = 0.1) {
        set(setting, (setting.value + step))
    }

    fun decrement(setting: RangeSetting<Double>, step: Double = 0.1) {
        set(setting, (setting.value - step))
    }

    fun <T, R> activate(setting: Setting<T, R>) {
        setting.activateInPreferences(this)
    }

    inline fun <reified E> toggle(setting: EnumSetting<E>, value: E) {
        if (get(setting.key) != value) {
            set(setting, value)
        } else {
            remove(setting.key)
        }
    }
}

@ExperimentalReadiumApi
val Preferences.columnCount: ColumnCount?
    get() = get(COLUMN_COUNT)

@ExperimentalReadiumApi
var MutablePreferences.columnCount: ColumnCount?
    get() = get(COLUMN_COUNT)
    set(value) { set(COLUMN_COUNT, value) }

@ExperimentalReadiumApi
val Preferences.fit: Fit?
    get() = get(FIT)

@ExperimentalReadiumApi
var MutablePreferences.fit: Fit?
    get() = get(FIT)
    set(value) { set(FIT, value) }

@ExperimentalReadiumApi
val Preferences.font: Font?
    get() = get(FONT)

@ExperimentalReadiumApi
var MutablePreferences.font: Font?
    get() = get(FONT)
    set(value) { set(FONT, value) }

@ExperimentalReadiumApi
val Preferences.fontSize: Double?
    get() = get(FONT_SIZE)

@ExperimentalReadiumApi
var MutablePreferences.fontSize: Double?
    get() = get(FONT_SIZE)
    set(value) { set(FONT_SIZE, value) }

@ExperimentalReadiumApi
val Preferences.publisherStyles: Boolean?
    get() = get(PUBLISHER_STYLES)

@ExperimentalReadiumApi
var MutablePreferences.publisherStyles: Boolean?
    get() = get(PUBLISHER_STYLES)
    set(value) { set(PUBLISHER_STYLES, value) }

@ExperimentalReadiumApi
val Preferences.orientation: Orientation?
    get() = get(ORIENTATION)

@ExperimentalReadiumApi
var MutablePreferences.orientation: Orientation?
    get() = get(ORIENTATION)
    set(value) { set(ORIENTATION, value) }

@ExperimentalReadiumApi
val Preferences.overflow: Overflow?
    get() = get(OVERFLOW)

@ExperimentalReadiumApi
var MutablePreferences.overflow: Overflow?
    get() = get(OVERFLOW)
    set(value) { set(OVERFLOW, value) }

@ExperimentalReadiumApi
val Preferences.readingProgression: ReadingProgression?
    get() = get(READING_PROGRESSION)

@ExperimentalReadiumApi
var MutablePreferences.readingProgression: ReadingProgression?
    get() = get(READING_PROGRESSION)
    set(value) { set(READING_PROGRESSION, value) }

@ExperimentalReadiumApi
val Preferences.theme: Theme?
    get() = get(THEME)

@ExperimentalReadiumApi
var MutablePreferences.theme: Theme?
    get() = get(THEME)
    set(value) { set(THEME, value) }

@ExperimentalReadiumApi
val Preferences.wordSpacing: Double?
    get() = get(WORD_SPACING)

@ExperimentalReadiumApi
var MutablePreferences.wordSpacing: Double?
    get() = get(WORD_SPACING)
    set(value) { set(WORD_SPACING, value) }
