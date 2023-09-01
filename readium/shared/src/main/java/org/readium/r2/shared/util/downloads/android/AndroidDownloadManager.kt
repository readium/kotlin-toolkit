/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.android

import android.app.DownloadManager as SystemDownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.shared.units.Hz
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.toUri

public class AndroidDownloadManager internal constructor(
    private val context: Context,
    private val name: String,
    private val destStorage: Storage,
    private val dirType: String,
    private val refreshRate: Hz,
    private val allowDownloadsOverMetered: Boolean,
    private val listener: DownloadManager.Listener
) : DownloadManager {

    public enum class Storage {
        App,
        Shared;
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val downloadManager: SystemDownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as SystemDownloadManager

    private val downloadsRepository: DownloadsRepository =
        DownloadsRepository(context)

    private var observeProgressJob: Job? =
        null

    init {
        coroutineScope.launch {
            if (downloadsRepository.hasDownloadsOngoing()) {
                startObservingProgress()
            }
        }
    }

    public override suspend fun submit(request: DownloadManager.Request): DownloadManager.RequestId {
        startObservingProgress()

        val androidRequest = createRequest(
            uri = request.url.toUri(),
            filename = generateFileName(extension = request.url.extension),
            headers = request.headers,
            title = request.title,
            description = request.description
        )
        val downloadId = downloadManager.enqueue(androidRequest)
        downloadsRepository.addId(name, downloadId)
        return DownloadManager.RequestId(downloadId.toString())
    }

    private fun generateFileName(extension: String?): String {
        val dottedExtension = extension
            ?.let { ".$it" }
            .orEmpty()
        return "${UUID.randomUUID()}$dottedExtension}"
    }

    public override suspend fun cancel(requestId: DownloadManager.RequestId) {
        val longId = requestId.value.toLong()
        downloadManager.remove()
        downloadsRepository.removeId(name, longId)
        if (!downloadsRepository.hasDownloadsOngoing()) {
            stopObservingProgress()
        }
    }

    private fun createRequest(
        uri: Uri,
        filename: String,
        headers: Map<String, List<String>>,
        title: String,
        description: String?
    ): SystemDownloadManager.Request =
        SystemDownloadManager.Request(uri)
            .setNotificationVisibility(SystemDownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestination(filename)
            .setHeaders(headers)
            .setTitle(title)
            .apply { description?.let { setDescription(it) } }
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

    private fun SystemDownloadManager.Request.setHeaders(
        headers: Map<String, List<String>>
    ): SystemDownloadManager.Request {
        for (header in headers) {
            for (value in header.value) {
                addRequestHeader(header.key, value)
            }
        }
        return this
    }

    private fun SystemDownloadManager.Request.setDestination(
        filename: String
    ): SystemDownloadManager.Request {
        when (destStorage) {
            Storage.App ->
                setDestinationInExternalFilesDir(context, dirType, filename)

            Storage.Shared ->
                setDestinationInExternalPublicDir(dirType, filename)
        }
        return this
    }

    private fun startObservingProgress() {
        if (observeProgressJob != null) {
            return
        }

        observeProgressJob = coroutineScope.launch {
            while (true) {
                val ids = downloadsRepository.idsForName(name)
                val cursor = downloadManager.query(SystemDownloadManager.Query())
                notify(cursor, ids)
                delay((1.0 / refreshRate.value).seconds)
            }
        }
    }

    private fun stopObservingProgress() {
        observeProgressJob?.cancel()
        observeProgressJob = null
    }

    private suspend fun notify(cursor: Cursor, ids: List<Long>) = cursor.use {
        while (cursor.moveToNext()) {
            val facade = DownloadCursorFacade(cursor)
            val id = DownloadManager.RequestId(facade.id.toString())

            if (facade.id !in ids) {
                continue
            }

            when (facade.status) {
                SystemDownloadManager.STATUS_FAILED -> {
                    listener.onDownloadFailed(id, mapErrorCode(facade.reason!!))
                    downloadManager.remove(facade.id)
                    downloadsRepository.removeId(name, facade.id)
                    if (!downloadsRepository.hasDownloadsOngoing()) {
                        stopObservingProgress()
                    }
                }
                SystemDownloadManager.STATUS_PAUSED -> {}
                SystemDownloadManager.STATUS_PENDING -> {}
                SystemDownloadManager.STATUS_SUCCESSFUL -> {
                    val destUri = Uri.parse(facade.localUri!!)
                    val destFile = File(destUri.path!!)
                    val newDest = File(destFile.parent, generateFileName(destFile.extension))
                    if (destFile.renameTo(newDest)) {
                        listener.onDownloadCompleted(id, newDest)
                    } else {
                        listener.onDownloadFailed(id, DownloadManager.Error.FileError())
                    }
                    downloadManager.remove(facade.id)
                    downloadsRepository.removeId(name, facade.id)
                    if (!downloadsRepository.hasDownloadsOngoing()) {
                        stopObservingProgress()
                    }
                }
                SystemDownloadManager.STATUS_RUNNING -> {
                    val expected = facade.expected
                    listener.onDownloadProgressed(id, facade.downloadedSoFar, expected)
                }
            }
        }
    }

    private fun mapErrorCode(code: Int): DownloadManager.Error =
        when (code) {
            401, 403 ->
                DownloadManager.Error.Forbidden()
            404 ->
                DownloadManager.Error.NotFound()
            500, 501 ->
                DownloadManager.Error.Server()
            502, 503, 504 ->
                DownloadManager.Error.Unreachable()
            SystemDownloadManager.ERROR_CANNOT_RESUME ->
                DownloadManager.Error.CannotResume()
            SystemDownloadManager.ERROR_DEVICE_NOT_FOUND ->
                DownloadManager.Error.DeviceNotFound()
            SystemDownloadManager.ERROR_FILE_ERROR ->
                DownloadManager.Error.FileError()
            SystemDownloadManager.ERROR_HTTP_DATA_ERROR ->
                DownloadManager.Error.HttpData()
            SystemDownloadManager.ERROR_INSUFFICIENT_SPACE ->
                DownloadManager.Error.InsufficientSpace()
            SystemDownloadManager.ERROR_TOO_MANY_REDIRECTS ->
                DownloadManager.Error.TooManyRedirects()
            SystemDownloadManager.ERROR_UNHANDLED_HTTP_CODE ->
                DownloadManager.Error.Unknown()
            SystemDownloadManager.ERROR_UNKNOWN ->
                DownloadManager.Error.Unknown()
            else ->
                DownloadManager.Error.Unknown()
        }

    public override suspend fun close() {
        coroutineScope.cancel()
    }
}
