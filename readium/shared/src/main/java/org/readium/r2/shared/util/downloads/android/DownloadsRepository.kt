/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.android

import android.content.Context
import java.io.File
import java.util.LinkedList
import org.json.JSONArray
import org.json.JSONObject

internal class DownloadsRepository(
    context: Context
) {

    private val storageDir: File =
        File(context.noBackupFilesDir, DownloadsRepository::class.qualifiedName!!)
            .also { if (!it.exists()) it.mkdirs() }

    private val storageFile: File =
        File(storageDir, "downloads.json")
            .also { if (!it.exists()) { it.writeText("{}", Charsets.UTF_8) } }

    private var snapshot: MutableMap<String, List<Long>> =
        storageFile.readText(Charsets.UTF_8).toData().toMutableMap()

    fun addId(name: String, id: Long) {
        snapshot[name] = snapshot[name].orEmpty() + id
        storageFile.writeText(snapshot.toJson(), Charsets.UTF_8)
    }

    fun removeId(name: String, id: Long) {
        snapshot[name] = snapshot[name].orEmpty() - id
        storageFile.writeText(snapshot.toJson(), Charsets.UTF_8)
    }

    fun idsForName(name: String): List<Long> {
        return snapshot[name].orEmpty()
    }

    fun hasDownloads(): Boolean =
        snapshot.values.flatten().isNotEmpty()

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
