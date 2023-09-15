/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import java.util.LinkedList
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.util.CoroutineQueue

internal class LcpDownloadsRepository(
    context: Context
) {
    private val queue = CoroutineQueue()

    private val storageDir: Deferred<File> =
        queue.scope.async {
            withContext(Dispatchers.IO) {
                File(context.noBackupFilesDir, LcpDownloadsRepository::class.qualifiedName!!)
                    .also { if (!it.exists()) it.mkdirs() }
            }
        }

    private val storageFile: Deferred<File> =
        queue.scope.async {
            withContext(Dispatchers.IO) {
                File(storageDir.await(), "licenses.json")
                    .also { if (!it.exists()) { it.writeText("{}", Charsets.UTF_8) } }
            }
        }

    private val snapshot: Deferred<MutableMap<String, JSONObject>> =
        queue.scope.async {
            readSnapshot().toMutableMap()
        }

    fun addDownload(id: String, license: JSONObject) {
        queue.scope.launch {
            val snapshotCompleted = snapshot.await()
            snapshotCompleted[id] = license
            writeSnapshot(snapshotCompleted)
        }
    }

    fun removeDownload(id: String) {
        queue.launch {
            val snapshotCompleted = snapshot.await()
            snapshotCompleted.remove(id)
            writeSnapshot(snapshotCompleted)
        }
    }

    suspend fun retrieveLicense(id: String): JSONObject? =
        queue.await {
            snapshot.await()[id]
        }

    private suspend fun readSnapshot(): Map<String, JSONObject> {
        return withContext(Dispatchers.IO) {
            storageFile.await().readText(Charsets.UTF_8).toData().toMutableMap()
        }
    }

    private suspend fun writeSnapshot(snapshot: Map<String, JSONObject>) {
        val storageFileCompleted = storageFile.await()
        withContext(Dispatchers.IO) {
            storageFileCompleted.writeText(snapshot.toJson(), Charsets.UTF_8)
        }
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
