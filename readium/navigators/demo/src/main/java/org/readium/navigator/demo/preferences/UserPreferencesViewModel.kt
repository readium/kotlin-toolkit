/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.navigator.common.SettingsEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
class UserPreferencesViewModel<S : Settings, P : Preferences<P>>(
    private val viewModelScope: CoroutineScope,
    private val preferencesManager: PreferencesManager<P>,
    createSettingsEditor: (P) -> SettingsEditor<P, S>
) {
    val preferences: StateFlow<P> = preferencesManager.preferences

    val editor: StateFlow<SettingsEditor<P, S>> = preferences
        .mapStateIn(viewModelScope, createSettingsEditor)

    val settings: StateFlow<S> = editor.mapStateIn(viewModelScope) { it.settings }

    fun commit() {
        viewModelScope.launch {
            preferencesManager.setPreferences(editor.value.preferences)
        }
    }
}
