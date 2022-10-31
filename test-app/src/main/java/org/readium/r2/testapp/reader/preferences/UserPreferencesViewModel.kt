/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.preferences

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.adapters.pspdfkit.navigator.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorFactory
import org.readium.r2.navigator.epub.*
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.reader.*

/**
 * Manages user settings.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 *
 * @param bookId Database ID for the book.
 */
class UserPreferencesViewModel<S: Configurable.Settings, P: Configurable.Preferences, E: PreferencesEditor<P>>(
    private val bookId: Long,
    private val viewModelScope: CoroutineScope,
    private val preferencesManager: PreferencesManager<P>,
    navigatorFactory: NavigatorFactory<S, P, E>
) {

    companion object {

        operator fun invoke(
            viewModelScope: CoroutineScope,
            readerInitData: ReaderInitData
        ): UserPreferencesViewModel<*, *, *>? =
            when (readerInitData) {
                is EpubReaderInitData -> with (readerInitData) {
                    UserPreferencesViewModel(
                        bookId, viewModelScope, preferencesManager, navigatorFactory
                    )
                }
                is PdfReaderInitData -> with (readerInitData) {
                    UserPreferencesViewModel(
                        bookId, viewModelScope, preferencesManager, navigatorFactory
                    )
                }
                else -> null
            }
    }

    /**
     * Current [Navigator] settings.
     */
    private val _settings = MutableStateFlow<S?>(null)

    /**
     * Current reader theme.
     */
    val theme: StateFlow<Theme> = _settings
        .filterIsInstance<EpubSettings>()
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = Theme.LIGHT)


    fun bind(configurable: Configurable<S, P>, lifecycleOwner: LifecycleOwner) {
        with(lifecycleOwner) {
            configurable.settings
                .flowWithLifecycle(lifecycle)
                .onEach {
                    _settings.value = it
                }
                .launchIn(lifecycleScope)

            preferencesManager.preferences
                .flowWithLifecycle(lifecycle)
                .onEach { configurable.submitPreferences(it) }
                .launchIn(lifecycleScope)
        }
    }

    val editor: E = navigatorFactory
        .createPreferencesEditor(preferencesManager.preferences.value)

    fun commitPreferences() = viewModelScope.launch {
            preferencesManager.setPreferences(editor.preferences)
        }
}
