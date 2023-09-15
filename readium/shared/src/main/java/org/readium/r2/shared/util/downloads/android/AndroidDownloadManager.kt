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
import android.os.Environment
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
import org.readium.r2.shared.units.hz
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.toUri

/**
 * A [DownloadManager] implementation using the Android download service.
 */
public class AndroidDownloadManager internal constructor(
    private val context: Context,
    private val destStorage: Storage,
    private val dirType: String,
    private val refreshRate: Hz,
    private val allowDownloadsOverMetered: Boolean
) : DownloadManager {

    /**
     * Creates a new instance of [AndroidDownloadManager].
     *
     * @param context Android context
     * @param destStorage Location where downloads should be stored
     * @param refreshRate Frequency with which download status will be checked and
     *   listeners notified
     * @param allowDownloadsOverMetered If downloads must be paused when only metered connexions
     *   are available
     */
    public constructor(
        context: Context,
        destStorage: Storage = Storage.App,
        refreshRate: Hz = 0.1.hz,
        allowDownloadsOverMetered: Boolean = true
    ) : this(
        context = context,
        destStorage = destStorage,
        dirType = Environment.DIRECTORY_DOWNLOADS,
        refreshRate = refreshRate,
        allowDownloadsOverMetered = allowDownloadsOverMetered
    )

    public enum class Storage {
        /**
         * App internal storage.
         */
        App,

        /**
         * Shared storage, accessible by users.
         */
        Shared;
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val downloadManager: SystemDownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as SystemDownloadManager

    private var observeProgressJob: Job? =
        null

    private val listeners: MutableMap<DownloadManager.RequestId, MutableList<DownloadManager.Listener>> =
        mutableMapOf()

    public override fun register(
        requestId: DownloadManager.RequestId,
        listener: DownloadManager.Listener
    ) {
        listeners.getOrPut(requestId) { mutableListOf() }.add(listener)

        if (observeProgressJob == null) {
            maybeStartObservingProgress()
        }
    }

    public override fun submit(
        request: DownloadManager.Request,
        listener: DownloadManager.Listener
    ): DownloadManager.RequestId {
        maybeStartObservingProgress()

        val androidRequest = createRequest(
            uri = request.url.toUri(),
            filename = generateFileName(extension = request.url.extension),
            headers = request.headers,
            title = request.title,
            description = request.description
        )
        val downloadId = downloadManager.enqueue(androidRequest)
        val requestId = DownloadManager.RequestId(downloadId.toString())
        register(requestId, listener)
        return requestId
    }

    private fun generateFileName(extension: String?): String {
        val dottedExtension = extension
            ?.let { ".$it" }
            .orEmpty()
        return "${UUID.randomUUID()}$dottedExtension}"
    }

    public override fun cancel(requestId: DownloadManager.RequestId) {
        val longId = requestId.value.toLong()
        downloadManager.remove(longId)
        val listenersForId = listeners[requestId].orEmpty()
        listenersForId.forEach { it.onDownloadCancelled(requestId) }
        listeners.remove(requestId)
        if (!listeners.any { it.value.isNotEmpty() }) {
            maybeStopObservingProgress()
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
            .setAllowedOverMetered(allowDownloadsOverMetered)

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

    private fun maybeStartObservingProgress() {
        if (observeProgressJob != null || listeners.all { it.value.isEmpty() }) {
            return
        }

        observeProgressJob = coroutineScope.launch {
            while (true) {
                val cursor = downloadManager.query(SystemDownloadManager.Query())
                notify(cursor)
                delay((1.0 / refreshRate.value).seconds)
            }
        }
    }

    private fun maybeStopObservingProgress() {
        if (listeners.all { it.value.isEmpty() }) {
            observeProgressJob?.cancel()
            observeProgressJob = null
        }
    }

    private fun notify(cursor: Cursor) = cursor.use {
        val knownDownloads = mutableSetOf<DownloadManager.RequestId>()

        // Notify about known downloads
        while (cursor.moveToNext()) {
            val facade = DownloadCursorFacade(cursor)
            val id = DownloadManager.RequestId(facade.id.toString())
            val listenersForId = listeners[id].orEmpty()
            if (listenersForId.isNotEmpty()) {
                notifyDownload(id, facade, listenersForId)
            }
            knownDownloads.add(id)
        }

        // Missing downloads have been cancelled.
        val unknownDownloads = listeners - knownDownloads
        unknownDownloads.forEach { entry ->
            entry.value.forEach { it.onDownloadCancelled(entry.key) }
            listeners.remove(entry.key)
        }
        maybeStopObservingProgress()
    }

    private fun notifyDownload(
        id: DownloadManager.RequestId,
        facade: DownloadCursorFacade,
        listenersForId: List<DownloadManager.Listener>
    ) {
        when (facade.status) {
            SystemDownloadManager.STATUS_FAILED -> {
                listenersForId.forEach {
                    it.onDownloadFailed(id, mapErrorCode(facade.reason!!))
                }
                downloadManager.remove(facade.id)
                listeners.remove(id)
                maybeStopObservingProgress()
            }
            SystemDownloadManager.STATUS_PAUSED -> {}
            SystemDownloadManager.STATUS_PENDING -> {}
            SystemDownloadManager.STATUS_SUCCESSFUL -> {
                val destUri = Uri.parse(facade.localUri!!)
                val destFile = File(destUri.path!!)
                val newDest = File(destFile.parent, generateFileName(destFile.extension))
                if (destFile.renameTo(newDest)) {
                    listenersForId.forEach {
                        it.onDownloadCompleted(id, newDest, mediaType = null)
                    }
                } else {
                    listenersForId.forEach {
                        it.onDownloadFailed(id, DownloadManager.Error.FileError())
                    }
                }
                downloadManager.remove(facade.id)
                listeners.remove(id)
                maybeStopObservingProgress()
            }
            SystemDownloadManager.STATUS_RUNNING -> {
                val expected = facade.expected?.takeIf { it > 0 }
                listenersForId.forEach {
                    it.onDownloadProgressed(id, facade.downloadedSoFar, expected)
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

    public override fun close() {
        listeners.clear()
        coroutineScope.cancel()
    }
}
