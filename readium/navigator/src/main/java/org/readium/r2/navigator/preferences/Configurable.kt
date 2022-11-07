/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.preferences.Configurable.Settings
import org.readium.r2.navigator.preferences.Configurable.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A [Configurable] is a component with a set of configurable [Settings].
 */
@ExperimentalReadiumApi
interface Configurable<T : Settings, P: Preferences> {

    /**
     * Marker interface for the [Settings] properties holder.
     */
    interface Settings

    /**
     * Marker interface for the [Preferences] properties holder.
     */
    interface Preferences

    /**
     * Current [Settings] values.
     */
    val settings: StateFlow<T>

    /**
     * Submits a new set of [Preferences] to update the current [Settings].
     *
     * Note that the [Configurable] might not update its [settings] right away, or might even ignore
     * some of the provided preferences. They are only used as hints to compute the new settings.
     */

    fun submitPreferences(preferences: P)
}

/**
 * JSON serializer of [P].
 */
@ExperimentalReadiumApi
interface PreferencesSerializer<P: Preferences> {

    /**
     * Serialize [P] into a JSON string.
     */
    fun serialize(preferences: P): String

    /**
     * Deserialize [P] from a JSON string.
     */
    fun deserialize(preferences: String): P
}

/**
 * Interactive editor of preferences.
 *
 * This can be used as a helper for a user preferences screen.
 */
@ExperimentalReadiumApi
interface PreferencesEditor<P: Preferences> {

    /**
     * The current preferences.
     */
    val preferences: P

    /**
     * Unset all preferences.
     */
    fun clear()
}

/**
 * A filter to keep only some preferences and filter out some others.
 */
@ExperimentalReadiumApi
fun interface PreferencesFilter<T: Preferences> {

    fun filter(preferences: T): T
}

