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
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.*
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.testapp.reader.*
import org.readium.r2.testapp.utils.extensions.combine
import kotlin.reflect.KClass

/**
 * Manages user settings.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 *
 * @param bookId Database ID for the book.
 * @param kind Navigator kind (e.g. EPUB, PDF, audiobook).
 */
sealed class UserPreferencesViewModel<S: Configurable.Settings, P: Configurable.Preferences, E: PreferencesEditor<P>>(
    private val bookId: Long,
    private val publication: Publication,
    private val viewModelScope: CoroutineScope,
    private val preferences: StateFlow<P>,
    private val preferencesClass: KClass<P>,
    private val preferencesStore: PreferencesStore,
    private val preferencesFilter: PreferencesFilter<P>,
    private val preferencesSerializer: PreferencesSerializer<P>,
    private val navigatorFactory: NavigatorFactory<S, P, E>
) {

    companion object {

        @OptIn(ExperimentalMedia2::class)
        operator fun invoke(
            viewModelScope: CoroutineScope,
            preferencesStore: PreferencesStore,
            readerInitData: ReaderInitData
        ): UserPreferencesViewModel<*, *, *>? =
            when (readerInitData) {
                is EpubReaderInitData -> with (readerInitData) {
                    EpubPreferencesViewModel(
                        bookId, publication, viewModelScope,
                        preferencesFlow, preferencesStore,
                        preferencesFilter, preferencesSerializer, navigatorFactory
                    )
                }
                is PdfReaderInitData -> with (readerInitData) {
                    PsPdfKitPreferencesViewModel(
                        bookId, publication, viewModelScope,
                        preferencesFlow, preferencesStore,
                        preferencesFilter, preferencesSerializer, navigatorFactory
                    )
                }
                is DummyReaderInitData, is MediaReaderInitData, is ImageReaderInitData ->
                    null
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
        }
    }

    open val editor: StateFlow<E?> =
        combine(viewModelScope, SharingStarted.Eagerly, _settings, preferences) { settings, preferences ->
            settings?.let { settingsNotNull ->
                navigatorFactory.createPreferencesEditor(settingsNotNull, preferences)
            }
        }

    fun submitPreferences(preferences: P) = viewModelScope.launch {
            val serializer = navigatorFactory.createPreferencesSerializer()
            val sharedPreferences = preferencesFilter.filterSharedPreferences(preferences)
            val publicationPreferences = preferencesFilter.filterPublicationPreferences(preferences)
            preferencesStore.set(serializer.serialize(sharedPreferences), preferencesClass)
            preferencesStore.set(serializer.serialize(publicationPreferences), preferencesClass, bookId)
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

// Create concrete classes to workaround the inability to switch over type parameter due to type erasure.

class EpubPreferencesViewModel(
    bookId: Long,
    publication: Publication,
    viewModelScope: CoroutineScope,
    preferences: StateFlow<EpubPreferences>,
    preferencesStore: PreferencesStore,
    preferencesFilter: EpubPreferencesFilter,
    preferencesSerializer: EpubPreferencesSerializer,
    navigatorFactory: EpubNavigatorFactory
) : UserPreferencesViewModel<EpubSettings, EpubPreferences, EpubPreferencesEditor>(
    bookId = bookId,
    publication = publication,
    viewModelScope = viewModelScope,
    preferences = preferences,
    preferencesClass = EpubPreferences::class,
    preferencesStore = preferencesStore,
    preferencesFilter = preferencesFilter,
    preferencesSerializer = preferencesSerializer,
    navigatorFactory = navigatorFactory
)

class PsPdfKitPreferencesViewModel(
    bookId: Long,
    publication: Publication,
    viewModelScope: CoroutineScope,
    preferences: StateFlow<PsPdfKitPreferences>,
    preferencesStore: PreferencesStore,
    preferencesFilter: PsPdfKitPreferencesFilter,
    preferencesSerializer: PsPdfKitPreferencesSerializer,
    navigatorFactory: PsPdfKitNavigatorFactory
) : UserPreferencesViewModel<PsPdfKitSettings, PsPdfKitPreferences, PsPdfKitPreferencesEditor>(
    bookId = bookId,
    publication = publication,
    viewModelScope = viewModelScope,
    preferences = preferences,
    preferencesStore = preferencesStore,
    preferencesClass = PsPdfKitPreferences::class,
    preferencesFilter = preferencesFilter,
    preferencesSerializer = preferencesSerializer,
    navigatorFactory = navigatorFactory
)
