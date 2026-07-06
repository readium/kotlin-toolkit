/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

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
 * Marker interface for the [Settings] properties holder.
 */
@ExperimentalReadiumApi
public interface Settings

/**
 * A controller for rendition settings that can be updated using preferences.
 */
@ExperimentalReadiumApi
public interface PreferencesController<P : Preferences<P>, S : Settings> {

    /**
     * The current preferences.
     */
    public var preferences: P

    /**
     * The current resolved settings.
     */
    public val settings: S
}
