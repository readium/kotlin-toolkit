/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.preferences.Configurable.Preferences
import org.readium.r2.navigator.preferences.Configurable.Settings

/**
 * A [Configurable] is a component with a set of configurable [Settings].
 */
public interface Configurable<S : Settings, P : Preferences<P>> {

    /**
     * Marker interface for the [Settings] properties holder.
     */
    public interface Settings

    /**
     * Marker interface for the [Preferences] properties holder.
     */
    public interface Preferences<P : Preferences<P>> {

        /**
         * Creates a new instance of [P] after merging the values of [other].
         *
         * In case of conflict, [other] takes precedence.
         */
        public operator fun plus(other: P): P
    }

    /**
     * Current [Settings] values.
     */
    public val settings: StateFlow<S>

    /**
     * Submits a new set of [Preferences] to update the current [Settings].
     *
     * Note that the [Configurable] might not update its [settings] right away, or might even ignore
     * some of the provided preferences. They are only used as hints to compute the new settings.
     */
    public fun submitPreferences(preferences: P)
}

/**
 * JSON serializer of [P].
 */
public interface PreferencesSerializer<P : Preferences<P>> {

    /**
     * Serialize [P] into a JSON string.
     */
    public fun serialize(preferences: P): String

    /**
     * Deserialize [P] from a JSON string.
     */
    public fun deserialize(preferences: String): P
}

/**
 * Interactive editor of preferences.
 *
 * This can be used as a helper for a user preferences screen.
 */
public interface PreferencesEditor<P : Preferences<P>> {

    /**
     * The current preferences.
     */
    public val preferences: P

    /**
     * Unset all preferences.
     */
    public fun clear()
}

/**
 * A filter to keep only some preferences and filter out some others.
 */
public fun interface PreferencesFilter<P : Preferences<P>> {

    public fun filter(preferences: P): P
}

public operator fun <P : Preferences<P>> PreferencesFilter<P>.plus(other: PreferencesFilter<P>): PreferencesFilter<P> =
    CombinedPreferencesFilter(this, other)

private class CombinedPreferencesFilter<P : Preferences<P>>(
    private val inner: PreferencesFilter<P>,
    private val outer: PreferencesFilter<P>,
) : PreferencesFilter<P> {
    override fun filter(preferences: P): P =
        outer.filter(inner.filter(preferences))
}
