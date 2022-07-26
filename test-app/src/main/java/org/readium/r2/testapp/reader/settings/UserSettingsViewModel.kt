/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.settings

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.navigator.settings.MutablePreferences
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Manages user settings.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 *
 * @param profile Publication profile (e.g. EPUB, PDF, audiobook).
 */
@OptIn(ExperimentalReadiumApi::class)
class UserSettingsViewModel(
    application: Application,
    private val profile: Publication.Profile?,
    scope: CoroutineScope
) {
    private val store = PreferencesStore(application, scope)

    /**
     * Current user preferences saved in the store.
     */
    val preferences: StateFlow<Preferences> = store[profile]
        .stateIn(scope, SharingStarted.Eagerly, initialValue = Preferences())

    /**
     * Current [Navigator] settings.
     */
    private val _settings = MutableStateFlow<Configurable.Settings?>(null)
    val settings: StateFlow<Configurable.Settings?> = _settings.asStateFlow()

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

            preferences
                .flowWithLifecycle(lifecycle)
                .onEach { configurable.applyPreferences(it) }
                .launchIn(lifecycleScope)
        }
    }

    /**
     * Edits and saves the user preferences.
     */
    fun edit(changes: MutablePreferences.() -> Unit) {
        store[profile] = preferences.value.copy(changes)
    }
}