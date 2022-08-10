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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.Setting
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import androidx.datastore.preferences.core.Preferences as JetpackPreferences

/**
 * Persists the navigator preferences using Jetpack [DataStore].
 *
 * The [Preferences] are split between:
 *   - the preferences related to a single book (e.g. language, reading progression)
 *   - the preferences shared between all the books of the same publication profile (e.g. EPUB)
 */
@OptIn(ExperimentalReadiumApi::class)
class PreferencesStore(
    context: Context,
    private val scope: CoroutineScope
) {
    /**
     * Observes the [Preferences] for the publication with the given [bookId] and
     * publication [profile].
     */
    operator fun get(bookId: Long, profile: Publication.Profile?): Flow<Preferences> =
        store.data.map { data ->
            val profilePrefs = Preferences(data[key(profile)])
            val bookPrefs = Preferences(data[key(bookId)])
            profilePrefs + bookPrefs
        }

    /**
     * Sets the [preferences] for the publication with the given [bookId] and publication [profile].
     */
    operator fun set(bookId: Long, profile: Publication.Profile?, preferences: Preferences) {
        scope.launch {
            store.edit { data ->
                // Filter the preferences that are related to the publication.
                data[key(bookId)] = preferences.filter(*Setting.PUBLICATION_SETTINGS).toJsonString()
                // Filter the preferences that will be shared between publications of the same profile.
                data[key(profile)] = preferences.filterNot(*Setting.PUBLICATION_SETTINGS).toJsonString()
            }
        }
    }

    private val store = context.preferences

    /** [DataStore] key for the given [bookId]. */
    private fun key(bookId: Long): JetpackPreferences.Key<String> =
        stringPreferencesKey("book-$bookId")

    /** [DataStore] key for the given publication [profile]. */
    private fun key(profile: Publication.Profile?): JetpackPreferences.Key<String> =
        if (profile != null) stringPreferencesKey(profile.uri)
        else stringPreferencesKey("default")
}

private val Context.preferences: DataStore<JetpackPreferences>
    by preferencesDataStore(name = "navigator-preferences")
