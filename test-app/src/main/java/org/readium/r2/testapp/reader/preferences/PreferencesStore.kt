/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass


/**
 * Persists the navigator preferences using Jetpack [DataStore].
 */
class PreferencesStore(
    private val context: Context
) {
    private val Context.preferences: DataStore<Preferences>
        by preferencesDataStore(name = "navigator-preferences")

    operator fun <P: Any> get(klass: KClass<P>, bookId: Long? = null): Flow<String?> =
        context.preferences.data.map { data ->
            bookId
                ?.let { data[key(bookId)] }
                ?: run { data[key(klass)] }
        }

    suspend fun <T: Any> set(preferences: String, klass: KClass<T>, bookId: Long? = null) {
        context.preferences.edit { data ->
            bookId
                ?.let { data[key(bookId)] = preferences }
                ?: run { data[key(klass)] = preferences }
        }
    }

    /** [DataStore] key for the given [bookId]. */
    private fun key(bookId: Long): Preferences.Key<String> =
        stringPreferencesKey("book-$bookId")

    /** [DataStore] key for the given preferences [klass]. */
    private fun <T: Any> key(klass: KClass<T>): Preferences.Key<String> =
        stringPreferencesKey("class-${klass.simpleName}")
}
