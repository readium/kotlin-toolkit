/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.downloads.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "readium-downloads-android"
)

private val downloadIdsKey: Preferences.Key<String> = stringPreferencesKey("downloadIds")

internal class DownloadsRepository(
    private val context: Context
) {

    private val downloadIds: Flow<Map<String, List<Long>>> =
        context.dataStore.data
            .map { data -> data[downloadIdsKey] }
            .map { string -> string?.toData().orEmpty() }

    suspend fun addId(name: String, id: Long) {
        context.dataStore.edit { data ->
            val current = downloadIds.first()
            val currentThisName = downloadIds.first()[name].orEmpty()
            val newEntryThisName = name to (currentThisName + id)
            data[downloadIdsKey] = (current + newEntryThisName).toJson()
        }
    }

    suspend fun removeId(name: String, id: Long) {
        context.dataStore.edit { data ->
            val current = downloadIds.first()
            val currentThisName = downloadIds.first()[name].orEmpty()
            val newEntryThisName = name to (currentThisName - id)
            data[downloadIdsKey] = (current + newEntryThisName).toJson()
        }
    }

    suspend fun idsForName(name: String): List<Long> {
        return downloadIds.first()[name].orEmpty()
    }

    private fun Map<String, List<Long>>.toJson(): String {
        val strings = map { idsToJson(it.key, it.value) }
        val array = JSONArray(strings)
        return array.toString()
    }

    private fun String.toData(): Map<String, List<Long>> {
        val array = JSONArray(this)
        val objects = (0 until array.length()).map { array.getJSONObject(it) }
        return objects.associate { jsonToIds(it) }
    }

    private fun idsToJson(name: String, downloads: List<Long>): JSONObject =
        JSONObject()
            .put("name", name)
            .put("downloads", JSONArray(downloads))

    private fun jsonToIds(jsonObject: JSONObject): Pair<String, List<Long>> {
        val name = jsonObject.getString("name")
        val downloads = jsonObject.getJSONArray("downloads")
        val downloadList = mutableListOf<Long>()
        for (i in 0 until downloads.length()) {
            downloadList.add(downloads.getLong(i))
        }
        return name to downloadList
    }
}
