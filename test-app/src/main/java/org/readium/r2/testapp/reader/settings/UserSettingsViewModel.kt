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
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
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
    private val bookId: Long,
    private val publication: Publication,
    private val kind: NavigatorKind?,
    private val scope: CoroutineScope,
    initialPreferences: Configurable.Preferences?
) {
    private val preferencesMixer: PreferencesMixer =
        PreferencesMixer(application)

    private val _preferences: StateFlow<Configurable.Preferences?> =
        when {
            kind != null && initialPreferences != null ->
                preferencesMixer.getPreferences(bookId, kind)!!
                    .stateIn(scope, SharingStarted.Eagerly, initialPreferences)
            else ->
                MutableStateFlow<Configurable.Preferences?>(null)
        }

    fun <P: Configurable.Preferences> getPreferences(preferencesClass: KClass<P>) =
        preferencesMixer.getPreferences(bookId, preferencesClass)

    /**
     * Current user preferences saved in the store.
     */

    /**
     * Current [Navigator] settings.
     */
    private val _settings = MutableStateFlow<Configurable.Settings?>(null)
    val settings = _settings.asStateFlow()

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

    val editor: StateFlow<PreferencesEditor?> = combine(scope, _settings, _preferences) { settings, preferences ->
        when {
            settings is PsPdfKitSettingsValues && preferences is PsPdfKitPreferences -> {
                PsPdfKitPreferencesEditor(
                    currentSettings = settings,
                    initialPreferences = preferences,
                    publicationMetadata = publication.metadata,
                    defaults = NavigatorDefaults.pdfDefaults,
                    onPreferencesEdited = ::submitPreferences
                )
            }
            else -> null
        }
    }

    private fun<T1, T2, R> combine(scope: CoroutineScope, flow: StateFlow<T1>, flow2: StateFlow<T2>, transform: (T1, T2) -> R): StateFlow<R> {
        val initialValue =  transform(flow.value, flow2.value)
        return combine(flow, flow2, transform).stateIn(scope, SharingStarted.Eagerly, initialValue)
    }

    private fun <P: Configurable.Preferences> submitPreferences(preferences: P) = scope.launch {
        preferencesMixer.setPreferences(bookId, preferences)
    }

    /**
     * A preset is a named group of settings applied together.
     */
    class Preset(
        val title: String,
        val changes: PreferencesEditor.() -> Unit
    )

    /**
     * Returns the presets associated with the [Configurable.Settings] receiver.
     */
    val presets: List<Preset> =
        when (kind) {
            NavigatorKind.EPUB_REFLOWABLE -> listOf(
                Preset("Increase legibility") {
                    (this as? WordSpacingEditor)?.wordSpacing = 0.6
                    (this as? FontSizeEditor)?.fontSize = 1.4
                    (this as? TextNormalizationEditor)?.textNormalization = TextNormalization.ACCESSIBILITY
                },
                Preset("Document") {
                    (this as? ScrollEditor)?.scroll = true
                },
                Preset("Ebook") {
                    (this as? ScrollEditor)?.scroll = false
                },
                Preset("Manga") {
                    (this as? ScrollEditor)?.scroll = false
                    (this as? ReadingProgressionEditor)?.readingProgression = ReadingProgression.RTL
                }
            )
            else -> emptyList()
        }
}
