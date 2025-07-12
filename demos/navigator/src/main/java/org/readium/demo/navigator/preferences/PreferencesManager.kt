/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import org.readium.navigator.common.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Trivial user preferences manager. You can add persistence.
 */
class PreferencesManager<P : Preferences<P>>(
    initialPreferences: P,
) {
    private val preferencesMutable: MutableStateFlow<P> =
        MutableStateFlow(initialPreferences)

    fun setPreferences(preferences: P) {
        preferencesMutable.value = preferences
    }
}
