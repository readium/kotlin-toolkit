/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */
@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.common

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Marker interface for the [Preferences] properties holder.
 */
@ExperimentalReadiumApi
public interface Preferences<P : Preferences<P>> {

    /**
     * Creates a new instance of [P] after merging the values of [other].
     *
     * In case of conflict, [other] takes precedence.
     */
    public operator fun plus(other: P): P
}

/**
 * Interactive editor of settings.
 *
 * This can be used as a helper for a user preferences screen.
 */
@ExperimentalReadiumApi
public interface PreferencesEditor<P : Preferences<P>, S : Settings> {

    /**
     * The current preferences.
     */
    public val preferences: P

    /**
     * The current computed settings.
     */
    public val settings: S

    /**
     * Unset all preferences.
     */
    public fun clear()
}

/**
 * JSON serializer of [P].
 */
@ExperimentalReadiumApi
public interface PreferencesSerializer<P : Configurable.Preferences<P>> {

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
 * A filter to keep only some preferences and filter out some others.
 */
@ExperimentalReadiumApi
public fun interface PreferencesFilter<P : Preferences<P>> {

    public fun filter(preferences: P): P
}

@ExperimentalReadiumApi
public operator fun <P : Preferences<P>> PreferencesFilter<P>.plus(other: PreferencesFilter<P>): PreferencesFilter<P> =
    CombinedPreferencesFilter(this, other)

private class CombinedPreferencesFilter<P : Preferences<P>>(
    private val inner: PreferencesFilter<P>,
    private val outer: PreferencesFilter<P>,
) : PreferencesFilter<P> {
    override fun filter(preferences: P): P =
        outer.filter(inner.filter(preferences))
}
