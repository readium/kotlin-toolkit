/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.preferences

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import kotlin.reflect.KClass


/**
 * Persists the navigator preferences using Jetpack [DataStore].
 */
@OptIn(ExperimentalReadiumApi::class)
class PreferencesStore(
    private val application: Application
) {
    private val Context.preferences: DataStore<Preferences>
        by preferencesDataStore(name = "navigator-preferences")

    @Suppress("Unchecked_cast")
    fun <T : Any> get(klass: KClass<T>, bookId: Long? = null): Flow<T> =
        when (klass) {
            EpubPreferences.Reflowable::class -> {
                application.preferences[EpubPreferences.Reflowable::class, bookId]
                    .map { it ?: EpubPreferences.Reflowable() } as Flow<T>
            }
            EpubPreferences.FixedLayout::class -> {
                application.preferences[EpubPreferences.FixedLayout::class, bookId]
                    .map { it ?: EpubPreferences.FixedLayout() } as Flow<T>
            }
            PsPdfKitPreferences::class -> {
                application.preferences[PsPdfKitPreferences::class, bookId]
                    .map { it ?: PsPdfKitPreferences() } as Flow<T>
            }
            else -> {
                throw IllegalArgumentException("Class ${klass.simpleName} not supported by the PreferencesStore ")
            }
        }

    suspend fun <T: Any> set(preferences: T, klass: KClass<T>, bookId: Long? = null) {
        when (klass) {
            EpubPreferences.Reflowable::class -> {
                application.preferences.set(preferences as EpubPreferences.Reflowable, bookId)
            }
            EpubPreferences.FixedLayout::class -> {
                application.preferences.set(preferences as EpubPreferences.FixedLayout, bookId)
            }
            PsPdfKitPreferences::class -> {
                application.preferences.set(preferences as PsPdfKitPreferences, bookId)
            }
            else -> {
                throw IllegalArgumentException("Class ${klass.simpleName} not supported by the PreferencesStore ")
            }
        }
    }

    inline operator fun <reified P: Any> DataStore<Preferences>.get(klass: KClass<P>, bookId: Long?): Flow<P?> =
        data.map { data -> bookId
            ?.let { getPreferences<P>(key(bookId), data) }
            ?: getPreferences(key(klass), data) }

    suspend inline fun <reified P: Any> DataStore<Preferences>.set(preferences: P, bookId: Long?) {
        edit { data -> bookId
            ?.let { data[key(bookId)] = Json.encodeToString(preferences) }
            ?: run { data[key(preferences::class)] = Json.encodeToString(preferences) }
        }
    }

    inline fun<reified P> getPreferences(key: Preferences.Key<String>, data: Preferences) =
        data[key]?.let { Json.decodeFromString<P>(it) }

    /** [DataStore] key for the given [bookId]. */
    fun key(bookId: Long): Preferences.Key<String> =
        stringPreferencesKey("book-$bookId")

    /** [DataStore] key for the given preferences [klass]. */
    fun <T: Any> key(klass: KClass<T>): Preferences.Key<String> =
        stringPreferencesKey("class-${klass.simpleName}")
}
