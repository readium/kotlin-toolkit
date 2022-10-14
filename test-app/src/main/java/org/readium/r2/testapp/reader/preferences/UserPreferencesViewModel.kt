/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.preferences

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.adapters.pspdfkit.navigator.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesDemux
import org.readium.r2.navigator.epub.EpubPreferencesEditor
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.testapp.reader.NavigatorKind

/**
 * Manages user settings.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 *
 * @param bookId Database ID for the book.
 * @param kind Navigator kind (e.g. EPUB, PDF, audiobook).
 */
@OptIn(ExperimentalReadiumApi::class)
class UserPreferencesViewModel(
    private val bookId: Long,
    private val publication: Publication,
    private val kind: NavigatorKind?,
    private val scope: CoroutineScope,
    private val preferences: StateFlow<Configurable.Preferences>,
    private val preferencesStore: PreferencesStore
) {

    /**
     * Current [Navigator] settings.
     */
    private val _settings = MutableStateFlow<Configurable.Settings?>(null)

    /**
     * Current reader theme.
     */
    val theme: StateFlow<Theme> = _settings
        .filterIsInstance<EpubSettings>()
        .map { it.theme }
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

    val editor: StateFlow<PreferencesEditor<*>?> =
        combine(scope, _settings, preferences) { settings, preferences ->
            when {
                settings is PsPdfKitSettings && preferences is PsPdfKitPreferences -> {
                    PsPdfKitPreferencesEditor(
                        currentSettings = settings,
                        initialPreferences = preferences,
                        publicationMetadata = publication.metadata,
                        defaults = PsPdfKitDefaults(),
                        configuration = PsPdfKitPreferencesEditor.Configuration()
                    )
                }
                settings is EpubSettings && preferences is EpubPreferences -> {
                    EpubPreferencesEditor(
                        currentSettings = settings,
                        initialPreferences = preferences,
                        publicationMetadata = publication.metadata,
                    )
                }
                else -> null
            }
        }

    private fun<T1, T2, R> combine(
        scope: CoroutineScope,
        flow: StateFlow<T1>,
        flow2: StateFlow<T2>,
        transform: (T1, T2) -> R
    ): StateFlow<R> {
        val initialValue = transform(flow.value, flow2.value)
        return combine(flow, flow2, transform).stateIn(scope, SharingStarted.Eagerly, initialValue)
    }

    fun <T: Configurable.Preferences> submitPreferences(preferences: T) =
        scope.launch {
            when (preferences) {
                is PsPdfKitPreferences -> {
                    val (sharedPrefs, pubPrefs) = PsPdfKitPreferencesDemux().demux(preferences)
                    preferencesStore.set(pubPrefs, PsPdfKitPreferences::class, bookId)
                    preferencesStore.set(sharedPrefs, PsPdfKitPreferences::class)
                }
                is EpubPreferences -> {
                    val (sharedPrefs, pubPrefs) = EpubPreferencesDemux().demux(preferences)
                    preferencesStore.set(pubPrefs, EpubPreferences::class, bookId)
                    preferencesStore.set(sharedPrefs, EpubPreferences::class)
                }
            }
        }

    /**
     * A preset is a named group of settings applied together.
     */
    class Preset(
        val title: String,
        val apply: () -> Unit
    )

    /**
     * Returns the presets associated with the [Configurable.Settings] receiver.
     */
    val presets: List<Preset> get() =
        when (val editor = editor.value) {
            is EpubPreferencesEditor ->
                when (editor.layout) {
                    EpubLayout.FIXED -> emptyList()
                    EpubLayout.REFLOWABLE -> listOf(
                        Preset("Increase legibility") {
                            editor.wordSpacing.value = 0.6
                            editor.fontSize.value = 1.4
                            editor.textNormalization.value = TextNormalization.ACCESSIBILITY
                        },
                        Preset("Document") {
                            editor.scroll.value = true
                        },
                        Preset("Ebook") {
                            editor.scroll.value = false
                        },
                        Preset("Manga") {
                            editor.scroll.value = false
                            editor.readingProgression.value = ReadingProgression.RTL
                        }
                    )
                }
            else ->
                emptyList()
        }
}
