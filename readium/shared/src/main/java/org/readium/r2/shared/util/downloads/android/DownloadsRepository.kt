/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.LinkedList
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
            .map { data -> data.ids }

    suspend fun addId(name: String, id: Long) {
        context.dataStore.edit { data ->
            val current = data.ids
            val currentThisName = current[name].orEmpty()
            val newEntryThisName = name to (currentThisName + id)
            data[downloadIdsKey] = (current + newEntryThisName).toJson()
        }
    }

    suspend fun removeId(name: String, id: Long) {
        context.dataStore.edit { data ->
            val current = data.ids
            val currentThisName = current[name].orEmpty()
            val newEntryThisName = name to (currentThisName - id)
            data[downloadIdsKey] = (current + newEntryThisName).toJson()
        }
    }

    suspend fun idsForName(name: String): List<Long> {
        return downloadIds.first()[name].orEmpty()
    }

    suspend fun hasDownloadsOngoing(): Boolean =
        downloadIds.first().values.flatten().isNotEmpty()

    private val Preferences.ids: Map<String, List<Long>>
        get() = get(downloadIdsKey)?.toData().orEmpty()

    private fun Map<String, List<Long>>.toJson(): String {
        val jsonObject = JSONObject()
        for ((name, ids) in this.entries) {
            jsonObject.put(name, JSONArray(ids))
        }
        return jsonObject.toString()
    }

    private fun String.toData(): Map<String, List<Long>> {
        val jsonObject = JSONObject(this)
        val names = jsonObject.keys().iterator().toList()
        return names.associateWith { jsonToIds(jsonObject.getJSONArray(it)) }
    }

    private fun jsonToIds(jsonArray: JSONArray): List<Long> {
        val list = mutableListOf<Long>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getLong(i))
        }
        return list
    }

    private fun <T> Iterator<T>.toList(): List<T> =
        LinkedList<T>().apply {
            while (hasNext())
                this += next()
        }.toMutableList()
}
