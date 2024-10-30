/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.preferences.Configurable

/**
 * Trivial user preferences manager. You can add persistence.
 */
class PreferencesManager<P : Configurable.Preferences<P>>(
    initialPreferences: P
) {
    private val preferencesMutable: MutableStateFlow<P> =
        MutableStateFlow(initialPreferences)

    val preferences: StateFlow<P> =
        preferencesMutable.asStateFlow()

    fun setPreferences(preferences: P) {
        preferencesMutable.value = preferences
    }
}
