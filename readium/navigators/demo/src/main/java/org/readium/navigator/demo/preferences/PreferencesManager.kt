/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Trivial user preferences manager without persistence.
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
