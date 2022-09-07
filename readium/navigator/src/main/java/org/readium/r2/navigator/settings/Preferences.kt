/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.json.JSONObject
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.logging.WarningLogger

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
 *         set(settings.scroll, false)
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
     * Creates a copy of this [Preferences] receiver, keeping only the preferences for the given
     * settings.
     */
    fun filter(vararg settings: Setting<*>): Preferences =
        filter(*settings.map { it.key }.toTypedArray())

    /**
     * Creates a copy of this [Preferences] receiver, keeping only the preferences for the given
     * settings.
     */
    fun filterNot(vararg settings: Setting<*>): Preferences =
        filterNot(*settings.map { it.key }.toTypedArray())

    /**
     * Creates a copy of this [Preferences] receiver, keeping only the preferences for the given
     * setting [keys].
     */
    fun filter(vararg keys: String): Preferences =
        Preferences(values.filterKeys { it in keys })

    /**
     * Creates a copy of this [Preferences] receiver, keeping only the preferences for the given
     * setting [keys].
     */
    fun filterNot(vararg keys: String): Preferences =
        Preferences(values.filterKeys { it !in keys })

    /**
     * Creates a copy of this [Preferences] receiver after modifying it with the given
     * [updates] builder.
     */
    fun copy(updates: MutablePreferences.() -> Unit): Preferences =
        Preferences(toMutablePreferences().apply(updates))

    /**
     * Creates a new [Preferences] object by replacing or adding values to the receiver from [other].
     */
    operator fun plus(other: Preferences): Preferences =
        copy { merge(other) }

    /**
     * Creates a mutable copy of this [Preferences] receiver.
     */
    fun toMutablePreferences(): MutablePreferences =
        MutablePreferences(values.toMutableMap())

    /**
     * Gets the preference for the given [setting], if set.
     */
    operator fun <V> get(setting: Setting<V>): V? =
        get(setting.key, coder = setting)

    /**
     * Gets the preference for the given [setting] key, if set.
     *
     * The value will be decoded using [coder].
     */
    operator fun <V> get(key: String, coder: SettingCoder<V>): V? =
        values[key]?.let { coder.decode(it) }

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

    companion object {
        /**
         * Creates a [Preferences] object from its JSON representation.
         */
        fun fromJson(json: JsonObject?): Preferences? {
            json ?: return null
            return Preferences(json.toMap())
        }

        /**
         * Creates a [Preferences] object from its JSON representation.
         */
        fun fromJson(jsonString: String): Preferences? {
            val json = tryOrNull { Json.parseToJsonElement(jsonString) as? JsonObject }
            return fromJson(json)
        }
    }
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
    operator fun <V> set(setting: Setting<V>, preference: V?) {
        set(setting, preference, activate = true)
    }

    /**
     * Sets the preference for the given setting [key].
     *
     * @param coder Coder used to encode the value.
     */
    operator fun <V> set(key: String, coder: SettingCoder<V>, preference: V?) {
        if (preference == null) {
            values.remove(key)
        } else {
            values[key] = coder.encode(preference)
        }
    }

    /**
     * Sets the preference for the given [setting].
     *
     * @param activate Indicates whether the setting will be force activated if needed.
     */
    fun <V> set(setting: Setting<V>, preference: V?, activate: Boolean = true) {
        val value = preference?.let { setting.validate(it) }

        set(key = setting.key, coder = setting.coder, preference = value)

        if (value != null && activate) {
            activate(setting)
        }
    }

    /**
     * Removes the preference for the given [setting].
     */
    fun <V> remove(setting: Setting<V>?) {
        setting ?: return
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
     * Merges the preferences of [other], overwriting the ones from the receiver in case of conflict.
     */
    operator fun plusAssign(other: Preferences) {
        merge(other)
    }

    /**
     * Returns the preference for the [Setting] receiver, or its current value when missing.
     */
    internal val <V> Setting<V>.prefOrValue: V get() =
        get(this) ?: value
}

/**
 * Returns whether the given [setting] is active in these preferences.
 *
 * An inactive setting is ignored by the [Configurable] until its activation conditions are met
 * (e.g. another setting has a certain preference).
 *
 * Use [MutablePreferences.activate] to activate it.
 */
@ExperimentalReadiumApi
fun <T> Preferences.isActive(setting: Setting<T>): Boolean =
    setting.isActiveWithPreferences(this)

/**
 * Activates the given [setting] in the preferences, if needed.
 */
@ExperimentalReadiumApi
fun <T> MutablePreferences.activate(setting: Setting<T>) {
    if (!isActive(setting)) {
        setting.activateInPreferences(this)
    }
}

/**
 * Sets the preference for the given [setting] after transforming the current value.
 *
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun <T> MutablePreferences.update(setting: Setting<T>, activate: Boolean = true, transform: (T) -> T) {
    set(setting, transform(setting.prefOrValue), activate = activate)
}

/**
 * Toggles the preference for the given [setting].
 *
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.toggle(setting: ToggleSetting, activate: Boolean = true) {
    set(setting, !setting.prefOrValue, activate = activate)
}

/**
 * Toggles the preference for the enum [setting] to the given [preference].
 *
 * If the preference was already set to the same value, it is removed.
 *
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun <E> MutablePreferences.toggle(setting: EnumSetting<E>, preference: E, activate: Boolean = true) {
    if (get(setting) != preference) {
        set(setting, preference, activate = activate)
    } else {
        remove(setting)
    }
}

/**
 * Increments the preference for the given [setting] to the next step.
 *
 * If the [setting] doesn't have any suggested steps, the [next] function will be used instead
 * to determine the next step.
 *
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun <V : Comparable<V>> MutablePreferences.increment(setting: RangeSetting<V>, activate: Boolean = true, next: (V) -> V) {
    val steps = setting.suggestedSteps
    if (steps == null) {
        update(setting, activate, next)
    } else {
        val index = steps.indexOfLast { it <= setting.prefOrValue }.takeIf { it != -1 } ?: return
        val nextValue = steps.getOrNull(index + 1) ?: return
        set(setting, nextValue, activate)
    }
}

/**
 * Decrements the preference for the given [setting] to the previous step.
 *
 * If the [setting] doesn't have any suggested steps, the [previous] function will be used instead
 * to determine the previous step.
 *
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun <V : Comparable<V>> MutablePreferences.decrement(setting: RangeSetting<V>, activate: Boolean = true, previous: (V) -> V) {
    val steps = setting.suggestedSteps
    if (steps == null) {
        update(setting, activate, previous)
    } else {
        val index = steps.indexOfFirst { it >= setting.prefOrValue }.takeIf { it != -1 } ?: return
        val previousValue = steps.getOrNull(index - 1) ?: return
        set(setting, previousValue, activate)
    }
}

/**
 * Increments the preference for the given [setting] to the next step.
 *
 * @param amount Amount to increment, when the [setting] doesn't have any suggested steps or
 * increment.
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.increment(setting: RangeSetting<Int>, amount: Int = setting.suggestedIncrement ?: 1, activate: Boolean = true) {
    increment(setting, activate) { it + amount }
}

/**
 * Decrements the preference for the given [setting] to the previous step.
 *
 * @param amount Amount to decrement, when the [setting] doesn't have any suggested steps or
 * increment.
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.decrement(setting: RangeSetting<Int>, amount: Int = setting.suggestedIncrement ?: 1, activate: Boolean = true) {
    decrement(setting, activate) { it - amount }
}

/**
 * Adjusts the preference for the given [setting] by the [amount].
 *
 * @param amount Amount to add to the current preference value.
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.adjustBy(setting: RangeSetting<Int>, amount: Int, activate: Boolean = true) {
    update(setting, activate) { it + amount }
}

/**
 * Increments the preference for the given [setting] to the next step.
 *
 * @param amount Amount to increment, when the [setting] doesn't have any suggested steps or
 * increment.
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.increment(setting: RangeSetting<Double>, amount: Double = setting.suggestedIncrement ?: 0.1, activate: Boolean = true) {
    increment(setting, activate) { it + amount }
}

/**
 * Decrements the preference for the given [setting] to the previous step.
 *
 * @param amount Amount to decrement, when the [setting] doesn't have any suggested steps or
 * increment.
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.decrement(setting: RangeSetting<Double>, amount: Double = setting.suggestedIncrement ?: 0.1, activate: Boolean = true) {
    decrement(setting, activate) { it - amount }
}

/**
 * Adjusts the preference for the given [setting] by the [amount].
 *
 * @param amount Amount to add to the current preference value.
 * @param activate Indicates whether the setting will be force activated if needed.
 */
@ExperimentalReadiumApi
fun MutablePreferences.adjustBy(setting: RangeSetting<Double>, amount: Double, activate: Boolean = true) {
    update(setting, activate) { it + amount }
}
