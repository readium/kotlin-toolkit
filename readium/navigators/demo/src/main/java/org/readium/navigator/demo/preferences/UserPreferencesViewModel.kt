/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
class UserPreferencesViewModel<P : Configurable.Preferences<P>>(
    private val viewModelScope: CoroutineScope,
    private val preferencesManager: PreferencesManager<P>,
    createPreferencesEditor: (P) -> PreferencesEditor<P>
) {
    val preferences: StateFlow<P> = preferencesManager.preferences

    val editor: StateFlow<PreferencesEditor<P>> = preferences
        .mapStateIn(viewModelScope, createPreferencesEditor)

    fun commit() {
        viewModelScope.launch {
            preferencesManager.setPreferences(editor.value.preferences)
        }
    }
}
