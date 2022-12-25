/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.preferences.Configurable.Preferences
import org.readium.r2.navigator.preferences.Configurable.Settings
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A [Configurable] is a component with a set of configurable [Settings].
 */
@ExperimentalReadiumApi
interface Configurable<S : Settings, P : Preferences<P>> {

    /**
     * Marker interface for the [Settings] properties holder.
     */
    interface Settings

    /**
     * Marker interface for the [Preferences] properties holder.
     */
    interface Preferences<P : Preferences<P>> {

        /**
         * Creates a new instance of [P] after merging the values of [other].
         *
         * In case of conflict, [other] takes precedence.
         */
        operator fun plus(other: P): P
    }

    /**
     * Current [Settings] values.
     */
    val settings: StateFlow<S>

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
interface PreferencesSerializer<P : Preferences<P>> {

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
interface PreferencesEditor<P : Preferences<P>> {

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
fun interface PreferencesFilter<P : Preferences<P>> {

    fun filter(preferences: P): P
}

@ExperimentalReadiumApi
operator fun <P : Preferences<P>> PreferencesFilter<P>.plus(other: PreferencesFilter<P>): PreferencesFilter<P> =
    CombinedPreferencesFilter(this, other)

@ExperimentalReadiumApi
private class CombinedPreferencesFilter<P : Preferences<P>>(
    private val inner: PreferencesFilter<P>,
    private val outer: PreferencesFilter<P>
) : PreferencesFilter<P> {
    override fun filter(preferences: P): P =
        outer.filter(inner.filter(preferences))
}
