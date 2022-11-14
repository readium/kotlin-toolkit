/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.readium.adapters.pdfium.navigator.PdfiumPreferences
import org.readium.adapters.pdfium.navigator.PdfiumPreferencesSerializer
import org.readium.adapters.pdfium.navigator.PdfiumPublicationPreferencesFilter
import org.readium.adapters.pdfium.navigator.PdfiumSharedPreferencesFilter
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import org.readium.r2.navigator.epub.EpubPublicationPreferencesFilter
import org.readium.r2.navigator.epub.EpubSharedPreferencesFilter
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.utils.extensions.stateInFirst
import kotlin.reflect.KClass

class PreferencesManager<T: Configurable.Preferences> internal constructor(
    val preferences: StateFlow<T>,
    @Suppress("Unused") // Keep the scope alive until the PreferencesManager is garbage collected
    private val coroutineScope: CoroutineScope,
    private val editPreferences: suspend (T) -> Unit,
) {

    suspend fun setPreferences(preferences: T) {
        editPreferences(preferences)
    }
}

sealed class PreferencesManagerFactory<T: Configurable.Preferences>(
    private val dataStore: DataStore<Preferences>,
    private val klass: KClass<T>,
    private val sharedPreferencesFilter: PreferencesFilter<T>,
    private val publicationPreferencesFilter: PreferencesFilter<T>,
    private val preferencesSerializer: PreferencesSerializer<T>,
    private val emptyPreferences: T,
    private val plus: (T).(T) -> T
) {
    suspend fun createPreferenceManager(bookId: Long): PreferencesManager<T> {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val preferences = getPreferences(bookId, coroutineScope)

        return PreferencesManager(
            preferences = preferences,
            coroutineScope = coroutineScope,
            editPreferences = { setPreferences(bookId, it) }
        )
    }

    private suspend fun setPreferences(bookId: Long, preferences: T) {
        dataStore.edit { data ->
            data[key(klass)] = sharedPreferencesFilter
                .filter(preferences)
                .let { preferencesSerializer.serialize(it) }
        }

        dataStore.edit { data ->
            data[key(bookId)] = publicationPreferencesFilter
                .filter(preferences)
                .let { preferencesSerializer.serialize(it) }
        }
    }

    private suspend fun getPreferences(bookId: Long, scope: CoroutineScope): StateFlow<T> {
        val sharedPrefs = dataStore.data
            .map { data -> data[key(klass)] }
            .map { json -> json?.let { preferencesSerializer.deserialize(it) } ?: emptyPreferences }

        val pubPrefs = dataStore.data
            .map { data -> data[key(bookId)] }
            .map { json -> json?.let { preferencesSerializer.deserialize(it) } ?: emptyPreferences }

        return combine(sharedPrefs, pubPrefs, plus).stateInFirst(scope, SharingStarted.Eagerly)
    }

    /** [DataStore] key for the given [bookId]. */
    private fun key(bookId: Long): Preferences.Key<String> =
        stringPreferencesKey("book-$bookId")

    /** [DataStore] key for the given preferences [klass]. */
    private fun <T: Any> key(klass: KClass<T>): Preferences.Key<String> =
        stringPreferencesKey("class-${klass.simpleName}")
}

class EpubPreferencesManagerFactory(
    dataStore: DataStore<Preferences>,
) : PreferencesManagerFactory<EpubPreferences>(
        dataStore = dataStore,
        klass = EpubPreferences::class,
        sharedPreferencesFilter = EpubSharedPreferencesFilter,
        publicationPreferencesFilter = EpubPublicationPreferencesFilter,
        preferencesSerializer = EpubPreferencesSerializer(),
        emptyPreferences = EpubPreferences(),
        plus = EpubPreferences::plus
)

class PdfiumPreferencesManagerFactory(
    dataStore: DataStore<Preferences>,
) : PreferencesManagerFactory<PdfiumPreferences>(
    dataStore = dataStore,
    klass = PdfiumPreferences::class,
    sharedPreferencesFilter = PdfiumSharedPreferencesFilter,
    publicationPreferencesFilter = PdfiumPublicationPreferencesFilter,
    preferencesSerializer = PdfiumPreferencesSerializer(),
    emptyPreferences = PdfiumPreferences(),
    plus = PdfiumPreferences::plus
)
