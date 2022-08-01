/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.extensions.tryOrLog
import timber.log.Timber

/**
 * Set of preferences used to update a [Configurable]'s settings.
 *
 * To modify the preferences, use one of the builder methods based on [MutablePreferences]. It
 * offers convenient helpers to update different type of [Setting] objects.
 *
 * [Preferences] can be serialized to JSON, which is useful to persist user preferences.
 *
 * Usage example:
 *
 *     // Get the currently available settings for the configurable.
 *     val settings = configurable.settings.value
 *
 *     // Build a new set of Preferences, using the Setting objects as keys.
 *     val prefs = Preferences {
 *         set(settings.overflow, Overflow.PAGINATED)
 *         increment(settings.fontSize)
 *     }
 *
 *     // Apply the preferences to the Configurable, which will automatically update its settings
 *     // accordingly.
 *     configurable.applyPreferences(prefs)
 *
 * @param values Direct access to the JSON values. Prefer using the safe [Setting]-based accessors
 * instead.
 */
@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
open class Preferences(
    @DelicateReadiumApi open val values: Map<String, JsonElement> = emptyMap()
)  {

    /**
     * Creates a [Preferences] object using a mutable builder.
     */
    constructor(builder: MutablePreferences.() -> Unit)
        : this(MutablePreferences().apply(builder))

    /**
     * Creates a read-only [Preferences] object from a mutable one.
     */
    constructor(preferences: MutablePreferences)
        : this(preferences.values.toMap())

    /**
     * Creates a [Preferences] object from its JSON representation.
     */
    constructor(jsonString: String?)
        : this(jsonString?.also { Timber.e(it) }?.let { Json.parseToJsonElement(it) as? JsonObject })

    /**
     * Creates a [Preferences] object from its JSON representation.
     */
    constructor(json: JsonObject?)
        : this(json?.toMap() ?: emptyMap())

    /**
     * Creates a copy of this [Preferences] receiver after modifying it with the given
     * [updates] builder.
     */
    fun copy(updates: MutablePreferences.() -> Unit): Preferences =
        Preferences(toMutablePreferences().apply(updates))

    /**
     * Creates a mutable copy of this [Preferences] receiver.
     */
    fun toMutablePreferences(): MutablePreferences =
        MutablePreferences(values.toMutableMap())

    /**
     * Gets the preference for the given [setting], if set.
     */
    operator fun <V, R> get(setting: Setting<V, R, *>): V? =
        values[setting.key]?.let { setting.decode(it) }

    /**
     * Returns whether the given [setting] is active in these preferences.
     *
     * An inactive setting is ignored by the [Configurable] until its activation conditions are met
     * (e.g. another setting has a certain preference).
     *
     * Use [MutablePreferences.activate] to activate it.
     */
    fun <T, R> isActive(setting: Setting<T, R, *>): Boolean =
        setting.isActiveWithPreferences(this)

    /**
     * Serializes this [Preferences] to a JSON object.
     */
    fun toJson(): JsonObject =
        JsonObject(values)

    /**
     * Serializes this [Preferences] to a JSON object.
     */
    fun toJsonString(): String =
        toJson().toString()

    override fun equals(other: Any?): Boolean =
        values == (other as? Preferences)?.values

    override fun hashCode(): Int =
        values.hashCode()

    override fun toString(): String =
        toJsonString()
}

/**
 * Mutable set of preferences used to update a [Configurable]'s settings.
 *
 * @param values Direct access to the JSON values. Prefer using the safe [Setting]-based accessors
 * instead.
 */
@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
class MutablePreferences(
    @DelicateReadiumApi override var values: MutableMap<String, JsonElement> = mutableMapOf()
) : Preferences(values) {

    /**
     * Sets the preference for the given [setting].
     */
    operator fun <V, R> set(setting: Setting<V, R, *>, preference: V?) {
        set(setting, preference, activate = true)
    }

    /**
     * Sets the preference for the given [setting].
     *
     * @param activate Indicates whether the setting will be force activated if needed.
     */
    fun <V, R> set(setting: Setting<V, R, *>, preference: V?, activate: Boolean = true) {
        val encodedValue = preference
            ?.let { setting.validate(it) }
            ?.let { setting.encode(it) }
        if (encodedValue == null) {
            values.remove(setting.key)
        } else {
            values[setting.key] = encodedValue
            if (activate) {
                activate(setting)
            }
        }
    }

    /**
     * Removes the preference for the given [setting].
     */
    fun <V, R> remove(setting: Setting<V, R, *>) {
        values.remove(setting.key)
    }

    /**
     * Clears all preferences.
     */
    fun clear() {
        values.clear()
    }

    /**
     * Merges the preferences of [other], overwriting the ones from the receiver in case of conflict.
     */
    fun merge(other: Preferences) {
        for ((key, value) in other.values) {
            values[key] = value
        }
    }

    /**
     * Activates the given [setting] in the preferences, if needed.
     */
    fun <T, R> activate(setting: Setting<T, R, *>) {
        if (!isActive(setting)) {
            setting.activateInPreferences(this)
        }
    }

    /**
     * Toggles the preference for the given [setting].
     *
     * @param activate Indicates whether the setting will be force activated if needed.
     */
    fun toggle(setting: ToggleSetting, activate: Boolean = true) {
        set(setting, !setting.prefOrValue, activate = activate)
    }

    /**
     * Toggles the preference for the enum [setting] to the given [preference].
     *
     * If the preference was already set to the same value, it is removed.
     *
     * @param activate Indicates whether the setting will be force activated if needed.
     */
    fun <E> toggle(setting: EnumSetting<E>, preference: E, activate: Boolean = true) {
        if (setting.prefOrValue != preference) {
            set(setting, preference, activate = activate)
        } else {
            remove(setting)
        }
    }

    /**
     * Increments the preference for the given [setting] to the next step.
     *
     * @param step Amount to increment, when the [setting] doesn't have any suggested steps.
     * @param activate Indicates whether the setting will be force activated if needed.
     */
    fun increment(setting: RangeSetting<Double>, step: Double = 0.1, activate: Boolean = true) {
        set(setting, setting.prefOrValue + step, activate = activate)
    }

    /**
     * Decrements the preference for the given [setting] to the previous step.
     *
     * @param step Amount to decrement, when the [setting] doesn't have any suggested steps.
     * @param activate Indicates whether the setting will be force activated if needed.
     */
    fun decrement(setting: RangeSetting<Double>, step: Double = 0.1, activate: Boolean = true) {
        set(setting, setting.prefOrValue - step, activate = activate)
    }

    /**
     * Returns the preference for the [Setting] receiver, or its current value when missing.
     */
    private val <V, R> Setting<V, R, *>.prefOrValue: V get() =
        get(this) ?: value
}
