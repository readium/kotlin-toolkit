/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.settings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.navigator.settings.MutablePreferences
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Manages user settings.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 */
@OptIn(ExperimentalReadiumApi::class)
class UserSettingsViewModel {

    fun bind(navigator: Navigator, lifecycleOwner: LifecycleOwner) {
        val configurable = (navigator as? Configurable) ?: return
        bind(configurable, lifecycleOwner)
    }

    fun bind(configurable: Configurable, lifecycleOwner: LifecycleOwner) {
        with(lifecycleOwner) {
            configurable.settings
                .flowWithLifecycle(lifecycle)
                .onEach {
                    _settings.value = it
                }
                .launchIn(lifecycleScope)

            _preferences
                .flowWithLifecycle(lifecycle)
                .onEach { prefs ->
                    configurable.applyPreferences(prefs)
                }
                .launchIn(lifecycleScope)
        }
    }

    private val _preferences = MutableStateFlow(Preferences()) // FIXME
    val preferences: StateFlow<Preferences> = _preferences.asStateFlow()

    private val _settings = MutableStateFlow<Configurable.Settings?>(null)
    val settings: StateFlow<Configurable.Settings?> = _settings.asStateFlow()

    fun updatePreferences(changes: MutablePreferences.() -> Unit) {
        _preferences.update { it.copy(changes) }
    }
}
