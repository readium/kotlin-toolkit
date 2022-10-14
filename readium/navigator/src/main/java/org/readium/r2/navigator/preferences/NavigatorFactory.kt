/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface NavigatorFactory<S: Configurable.Settings, P: Configurable.Preferences, E: PreferencesEditor<P>> {

    fun createPreferencesEditor(currentSettings: S, currentPreferences: P): E
}

@ExperimentalReadiumApi
interface PreferencesSerializer<P: Configurable.Preferences> {

    fun serialize(preferences: P): String

    fun deserialize(preferences: String): P
}

@ExperimentalReadiumApi
interface PreferencesEditor<P: Configurable.Preferences> {

    val preferences: P

    fun clear()
}

@ExperimentalReadiumApi
interface PreferencesFilter<T: Configurable.Preferences> {

    fun filterSharedPreferences(preferences: T): T

    fun filterPublicationPreferences(preferences: T): T
}
