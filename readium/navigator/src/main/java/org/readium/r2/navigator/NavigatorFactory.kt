/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface NavigatorFactory<S: Configurable.Settings, P: Configurable.Preferences, E: PreferencesEditor<P>> {

    fun createPreferencesEditor(currentSettings: S, currentPreferences: P): E
}
