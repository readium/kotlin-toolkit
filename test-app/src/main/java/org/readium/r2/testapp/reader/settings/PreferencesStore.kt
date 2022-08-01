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
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import androidx.datastore.preferences.core.Preferences as JetpackPreferences

/**
 * Persists the navigator preferences using Jetpack [DataStore].
 *
 * A different set of [Preferences] is stored for each [Publication.Profile].
 */
@OptIn(ExperimentalReadiumApi::class)
class PreferencesStore(
    context: Context,
    private val scope: CoroutineScope
) {

    /**
     * Observes the [Preferences] for the publication with the given [profile].
     */
    operator fun get(profile: Publication.Profile?): Flow<Preferences> =
        store.data
            .map { data -> Preferences(data[profile.preferencesKey]) }

    /**
     * Sets the [preferences] for the publication with the given [profile].
     */
    operator fun set(profile: Publication.Profile?, preferences: Preferences) {
        scope.launch {
            store.edit { data ->
                data[profile.preferencesKey] = preferences.toJsonString()
            }
        }
    }

    private val store = context.preferences

    private val Publication.Profile?.preferencesKey: JetpackPreferences.Key<String> get() =
        if (this != null) stringPreferencesKey(uri)
        else stringPreferencesKey("default")
}

private val Context.preferences: DataStore<JetpackPreferences>
    by preferencesDataStore(name = "navigator-preferences")
