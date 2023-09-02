/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import java.util.LinkedList
import org.json.JSONObject

internal class LcpDownloadsRepository(
    context: Context
) {
    private val storageDir: File =
        File(context.noBackupFilesDir, LcpDownloadsRepository::class.qualifiedName!!)
            .also { if (!it.exists()) it.mkdirs() }

    private val storageFile: File =
        File(storageDir, "licenses.json")
            .also { if (!it.exists()) { it.writeText("{}", Charsets.UTF_8) } }

    private val snapshot: MutableMap<String, JSONObject> =
        storageFile.readText(Charsets.UTF_8).toData().toMutableMap()

    fun addDownload(id: String, license: JSONObject) {
        snapshot[id] = license
        storageFile.writeText(snapshot.toJson(), Charsets.UTF_8)
    }

    fun removeDownload(id: String) {
        snapshot.remove(id)
        storageFile.writeText(snapshot.toJson(), Charsets.UTF_8)
    }

    fun retrieveLicense(id: String): JSONObject? {
        return snapshot[id]
    }

    private fun Map<String, JSONObject>.toJson(): String {
        val jsonObject = JSONObject()
        for ((id, license) in this.entries) {
            jsonObject.put(id, license)
        }
        return jsonObject.toString()
    }

    private fun String.toData(): Map<String, JSONObject> {
        val jsonObject = JSONObject(this)
        val names = jsonObject.keys().iterator().toList()
        return names.associateWith { jsonObject.getJSONObject(it) }
    }

    private fun <T> Iterator<T>.toList(): List<T> =
        LinkedList<T>().apply {
            while (hasNext())
                this += next()
        }.toMutableList()
}
