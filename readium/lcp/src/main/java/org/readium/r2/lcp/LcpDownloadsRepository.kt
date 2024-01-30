/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.util.CoroutineQueue
import org.readium.r2.shared.util.downloads.DownloadManager

internal class LcpDownloadsRepository(
    context: Context
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    private val queue: CoroutineQueue =
        CoroutineQueue()

    private val storageDir: Deferred<File> =
        coroutineScope.async {
            withContext(Dispatchers.IO) {
                File(context.noBackupFilesDir, LcpDownloadsRepository::class.qualifiedName!!)
                    .also { if (!it.exists()) it.mkdirs() }
            }
        }

    private val storageFile: Deferred<File> =
        coroutineScope.async {
            withContext(Dispatchers.IO) {
                File(storageDir.await(), "licenses.json")
                    .also { if (!it.exists()) { it.writeText("[]", Charsets.UTF_8) } }
            }
        }

    private val snapshot: Deferred<MutableList<Download>> =
        coroutineScope.async {
            readSnapshot().toMutableList()
        }

    fun addDownload(id: String, license: JSONObject) {
        coroutineScope.launch {
            val snapshotCompleted = snapshot.await()
            snapshotCompleted.add(Download(id, null, license))
            writeSnapshot(snapshotCompleted)
        }
    }

    fun removeDownload(id: String) {
        queue.launch {
            val snapshotCompleted = snapshot.await()
            val current = snapshotCompleted.firstOrNull { it.requestId == id }
                ?: return@launch
            snapshotCompleted.remove(current)
            writeSnapshot(snapshotCompleted)
        }
    }

    fun removeUnconfirmed() {
        queue.launch {
            val snapshotCompleted = snapshot.await()
            snapshotCompleted.removeAll { it.downloadId == null }
            writeSnapshot(snapshotCompleted)
        }
    }

    fun confirmDownload(id: String, downloadId: DownloadManager.RequestId) {
        queue.launch {
            val snapshotCompleted = snapshot.await()
            val current = snapshotCompleted.firstOrNull { it.requestId == id }
                ?: return@launch
            snapshotCompleted.remove(current)
            snapshotCompleted.add(current.copy(downloadId = downloadId))
            writeSnapshot(snapshotCompleted)
        }
    }

    suspend fun getDownload(downloadId: DownloadManager.RequestId): Download? =
        queue.await {
            snapshot.await().firstOrNull { it.downloadId == downloadId }
        }

    suspend fun getDownload(id: String): Download? =
        queue.await {
            snapshot.await().firstOrNull { it.requestId == id }
        }

    private suspend fun readSnapshot(): List<Download> {
        return withContext(Dispatchers.IO) {
            storageFile.await().readText(Charsets.UTF_8).toData()
        }
    }

    private suspend fun writeSnapshot(snapshot: List<Download>) {
        val storageFileCompleted = storageFile.await()
        withContext(Dispatchers.IO) {
            storageFileCompleted.writeText(snapshot.toJson(), Charsets.UTF_8)
        }
    }

    private fun List<Download>.toJson(): String {
        val jsonArray = JSONArray()
        for (download in this) {
            jsonArray.put(download.toJSON())
        }
        return jsonArray.toString()
    }

    private fun String.toData(): List<Download> {
        val jsonArray = JSONArray(this)
        return jsonArray.mapNotNull { item -> (item as? JSONObject)?.let { Download.fromJSON(it) } }
    }

    data class Download(
        val requestId: String,
        val downloadId: DownloadManager.RequestId?,
        val license: JSONObject
    ) : JSONable {

        override fun toJSON(): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("requestId", requestId)
            jsonObject.put("downloadId", downloadId?.value)
            jsonObject.put("license", license)
            return jsonObject
        }

        companion object {

            fun fromJSON(jsonObject: JSONObject): Download {
                val requestId = jsonObject
                    .getString("requestId")

                val downloadId = jsonObject
                    .takeIf { it.has("downloadId") }
                    ?.getString("downloadId")

                val license = jsonObject
                    .getJSONObject("license")

                return Download(
                    requestId = requestId,
                    downloadId = downloadId?.let { DownloadManager.RequestId(it) },
                    license = license
                )
            }
        }
    }
}
