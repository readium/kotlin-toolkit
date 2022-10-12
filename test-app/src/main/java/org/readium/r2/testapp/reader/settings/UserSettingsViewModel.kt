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
import kotlinx.coroutines.launch
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferences
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferencesEditor
import org.readium.adapters.pspdfkit.navigator.PsPdfKitSettingsValues
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.navigator.settings.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.reader.NavigatorDefaults
import org.readium.r2.testapp.reader.NavigatorKind
import kotlin.reflect.KClass

/**
 * Manages user settings.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 *
 * @param bookId Database ID for the book.
 * @param kind Navigator kind (e.g. EPUB, PDF, audiobook).
 */
@OptIn(ExperimentalReadiumApi::class)
class UserSettingsViewModel(
    application: Application,
    val bookId: Long,
    val publication: Publication,
    val kind: NavigatorKind?,
    val scope: CoroutineScope,
    initialPreferences: Configurable.Preferences?
) {
    private val preferencesMixer: PreferencesMixer =
        PreferencesMixer(application)

    val preferences: StateFlow<Configurable.Preferences>? =
        (if (kind != null && initialPreferences != null) {
            preferencesMixer.getPreferences(bookId, kind)
        } else {
            null
        })?.stateIn(scope, SharingStarted.Eagerly, initialPreferences!!)

    fun <P: Configurable.Preferences> getPreferences(preferencesClass: KClass<P>) =
        preferencesMixer.getPreferences(bookId, preferencesClass)

    /**
     * Current user preferences saved in the store.
     */

    /**
     * Current [Navigator] settings.
     */
    private val _settings = MutableStateFlow<Configurable.Settings?>(null)
    val settings: StateFlow<Configurable.Settings?> = _settings.asStateFlow()

    /**
     * Current reader theme.
     */
    val theme: StateFlow<Theme> = settings
        .filterIsInstance<EpubSettings.Reflowable>()
        .map { it.theme.value }
        .stateIn(scope, SharingStarted.Lazily, initialValue = Theme.LIGHT)

    fun bind(navigator: Navigator, lifecycleOwner: LifecycleOwner) {
        val configurable = (navigator as? Configurable<*, *>) ?: return
        bind(configurable, lifecycleOwner)
    }

    fun bind(configurable: Configurable<*, *>, lifecycleOwner: LifecycleOwner) {
        with(lifecycleOwner) {
            configurable.settings
                .flowWithLifecycle(lifecycle)
                .onEach {
                    _settings.value = it
                }
                .launchIn(lifecycleScope)
        }
    }

    fun <P: Configurable.Preferences> submitPreferences(preferences: P) = scope.launch {
        preferencesMixer.setPreferences(bookId, preferences)
    }

    fun <P: Configurable.Preferences> clearPreferences() = scope.launch {
        preferencesMixer.setPreferences(bookId, null)
    }

    fun createEditor(settings: Configurable.Settings, preferences: Configurable.Preferences): Any? =
        when {
            settings is PsPdfKitSettingsValues && preferences is PsPdfKitPreferences -> {
                PsPdfKitPreferencesEditor(
                    currentSettings = settings,
                    initialPreferences = preferences,
                    pubMetadata = publication.metadata,
                    defaults = NavigatorDefaults.pdfDefaults,
                    onPreferencesEdited = ::submitPreferences
                )
            }
            else -> null
        }
}
