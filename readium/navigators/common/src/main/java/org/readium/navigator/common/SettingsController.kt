/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.MutableState
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public interface SettingsController<S : Settings> {

    public val settings: MutableState<S>
}

/**
 * Marker interface for the [Settings] properties holder.
 */
@ExperimentalReadiumApi
public typealias Settings = org.readium.r2.navigator.preferences.Configurable.Settings

/**
 * Marker interface for the [Preferences] properties holder.
 */
@ExperimentalReadiumApi
public typealias Preferences<P> = org.readium.r2.navigator.preferences.Configurable.Preferences<P>

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
