/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.json.JSONObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*

/**
 * Holds a list of key-value pairs provided by the app to influence a [Configurable]'s setting.
 * The keys must be valid [SettingKey] keys.
 */
@ExperimentalReadiumApi
data class Preferences(
    val values: Map<String, Any?> = emptyMap()
) : JSONable {

    constructor(
        continuous: Boolean? = null,
        fit: Fit? = null,
        orientation: Orientation? = null,
        overflow: Overflow? = null,
        readingProgression: ReadingProgression? = null
    ) : this(
        buildMap {
            put(SettingKey.CONTINUOUS, continuous)
            put(SettingKey.FIT, fit)
            put(SettingKey.ORIENTATION, orientation)
            put(SettingKey.OVERFLOW, overflow)
            put(SettingKey.READING_PROGRESSION, readingProgression)
        }
    )

    inline operator fun <reified V, reified R> get(key: SettingKey<V, R>): V? =
        key.decode(values[key.key] as? R)

    inline fun <reified V, reified R> set(key: SettingKey<V, R>, value: V?): Preferences {
        val rawValue = key.encode(value)
        return Preferences(values + (key.key to rawValue))
    }

    /**
     * Returns a copy of this object after overwriting any preferences with the values from [other].
     */
    fun merge(other: Preferences): Preferences =
        Preferences(
            (other.values.entries + values.entries)
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, value) -> value.firstOrNull { it != null } }
        )

    override fun toJSON(): JSONObject =
        JSONObject(values.filterValues { it != null })

    companion object {

        fun fromJSON(json: JSONObject?): Preferences =
            Preferences(json?.toMap() ?: emptyMap())
    }
}

@ExperimentalReadiumApi
val Preferences.continuous: Boolean?
    get() = get(SettingKey.CONTINUOUS)

@ExperimentalReadiumApi
fun Preferences.continuous(value: Boolean): Preferences =
    set(SettingKey.CONTINUOUS, value)

@ExperimentalReadiumApi
val Preferences.fit: Fit?
    get() = get(SettingKey.FIT)

@ExperimentalReadiumApi
fun Preferences.fit(value: Fit): Preferences =
    set(SettingKey.FIT, value)

@ExperimentalReadiumApi
val Preferences.orientation: Orientation?
    get() = get(SettingKey.ORIENTATION)

@ExperimentalReadiumApi
fun Preferences.orientation(value: Orientation): Preferences =
    set(SettingKey.ORIENTATION, value)

@ExperimentalReadiumApi
val Preferences.overflow: Overflow?
    get() = get(SettingKey.OVERFLOW)

@ExperimentalReadiumApi
fun Preferences.overflow(value: Overflow): Preferences =
    set(SettingKey.OVERFLOW, value)

@ExperimentalReadiumApi
val Preferences.readingProgression: ReadingProgression?
    get() = get(SettingKey.READING_PROGRESSION)

@ExperimentalReadiumApi
fun Preferences.readingProgression(value: ReadingProgression): Preferences =
    set(SettingKey.READING_PROGRESSION, value)

@OptIn(ExperimentalReadiumApi::class)
private fun <V, R> MutableMap<String, Any?>.put(key: SettingKey<V, R>, value: V?) {
    put(key.key, key.encode(value))
}
