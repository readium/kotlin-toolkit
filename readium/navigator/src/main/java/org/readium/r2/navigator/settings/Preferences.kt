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
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.extensions.toMap

/**
 * Holds a list of key-value pairs provided by the app to influence a [Configurable]'s setting.
 */
@ExperimentalReadiumApi
@Parcelize
open class Preferences(
    @InternalReadiumApi open val values: @WriteWith<JSONParceler> Map<String, Any> = emptyMap()
) : JSONable, Parcelable {

    constructor(jsonString: String?)
        : this(jsonString?.let { JSONObject(it) })

    constructor(json: JSONObject?)
        : this(json?.toMap() ?: emptyMap())

    constructor(builder: MutablePreferences.() -> Unit)
        : this(MutablePreferences().apply(builder))

    constructor(mutablePreferences: MutablePreferences)
        : this(mutablePreferences.values.toMap())

    operator fun <V, R> get(setting: Setting<V, R, *>): V? =
        values[setting.key]?.let { setting.decode(it) }

    fun <T, R> isActive(setting: Setting<T, R, *>): Boolean =
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
}

@ExperimentalReadiumApi
@Parcelize
class MutablePreferences(
    @InternalReadiumApi override var values: @WriteWith<JSONParceler> MutableMap<String, Any> = mutableMapOf()
) : Preferences(values = values) {

    operator fun <V, R> set(setting: Setting<V, R, *>, value: V?) {
        set(setting, value, activate = true)
    }

    fun <V, R> set(setting: Setting<V, R, *>, value: V?, activate: Boolean = true) {
        val encodedValue = value?.let { setting.encode(setting.validate(it)) }
        if (encodedValue != null) {
            values[setting.key] = encodedValue
        } else {
            values.remove(setting.key)
            if (activate) {
                activate(setting)
            }
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

    fun <V, R> remove(setting: Setting<V, R, *>) {
        values.remove(setting.key)
    }

    fun toggle(setting: ToggleSetting) {
        set(setting, !setting.prefOrValue)
    }

    fun increment(setting: RangeSetting<Double>, step: Double = 0.1) {
        set(setting, setting.prefOrValue + step)
    }

    fun decrement(setting: RangeSetting<Double>, step: Double = 0.1) {
        set(setting, setting.prefOrValue - step)
    }

    fun <T, R> activate(setting: Setting<T, R, *>) {
        setting.activateInPreferences(this)
    }

    fun <E> toggle(setting: EnumSetting<E>, value: E) {
        if (setting.prefOrValue != value) {
            set(setting, value)
        } else {
            remove(setting)
        }
    }

    val <V, R> Setting<V, R, *>.prefOrValue: V get() =
        get(this) ?: value
}
