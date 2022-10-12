/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.readium.r2.testapp.reader.NavigatorKind
import androidx.datastore.preferences.core.Preferences as JetpackPreferences

/**
 * Persists the navigator preferences using Jetpack [DataStore].
 */

/**
 * Observes the [Preferences] for the publication with the given [bookId].
 */
inline operator fun <reified P> DataStore<JetpackPreferences>.get(bookId: Long): Flow<P?> =
    data.map { data -> getPreferences(key(bookId), data) }

/**
 * Observes the [Preferences] for the navigator [kind].
 */
inline operator fun <reified P> DataStore<JetpackPreferences>.get(kind: NavigatorKind?): Flow<P?> =
    data.map { data -> getPreferences(key(kind), data) }

inline fun<reified P> getPreferences(key: JetpackPreferences.Key<String>, data: JetpackPreferences) =
    data[key]?.let { Json.decodeFromString<P>(it) }

/**
 * Sets the [preferences] for the publication with the given [bookId].
 */
suspend inline fun<reified P> DataStore<JetpackPreferences>.set(bookId: Long, preferences: P?) {
    edit { data ->
        val key = key(bookId)
        if (preferences == null) {
            data.remove(key)
        } else {
            data[key] = Json.encodeToString(preferences)
        }
    }
}

/**
 * Sets the [preferences] for navigator [kind].
 */
suspend inline fun <reified P> DataStore<JetpackPreferences>.set(kind: NavigatorKind?, preferences: P?) {
    edit { data ->
        val key = key(kind)
        if (preferences == null) {
            data.remove(key)
        } else {
            data[key] = Json.encodeToString(preferences)
        }
    }
}

/** [DataStore] key for the given [bookId]. */
fun key(bookId: Long): JetpackPreferences.Key<String> =
    stringPreferencesKey("book-$bookId")

/** [DataStore] key for the given navigator [kind]. */
fun key(kind: NavigatorKind?): JetpackPreferences.Key<String> =
    if (kind != null) stringPreferencesKey("kind-${kind.name}")
    else stringPreferencesKey("default")
