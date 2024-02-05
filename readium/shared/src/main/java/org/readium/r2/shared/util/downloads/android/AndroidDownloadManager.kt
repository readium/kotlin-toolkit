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
import androidx.core.net.toFile
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUri
import org.readium.r2.shared.util.units.Hz
import org.readium.r2.shared.util.units.hz

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
     * Because of discrepancies across different devices, default notifications are disabled.
     * If you want to use this [DownloadManager], you will need permission
     * android.permission.DOWNLOAD_WITHOUT_NOTIFICATION.
     *
     * @param context Android context
     * communicates it.
     * @param destStorage Location where downloads should be stored
     * @param refreshRate Frequency with which download status will be checked and
     *   listeners notified
     * @param allowDownloadsOverMetered If downloads must be paused when only metered connexions
     *   are available
     */
    public constructor(
        context: Context,
        destStorage: Storage = Storage.App,
        refreshRate: Hz = 60.0.hz,
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
    ) {
        if (requestAlreadyExists(request.id)) {
            return
        }

        maybeStartObservingProgress()

        val androidRequest = createRequest(
            id = request.id,
            uri = request.url.toUri(),
            filename = generateFileName(extension = request.url.extension?.value),
            headers = request.headers
        )

        val downloadId = downloadManager.enqueue(androidRequest)
        DownloadManager.RequestId(downloadId.toString())
        register(request.id, listener)
    }

    private fun requestAlreadyExists(requestId: DownloadManager.RequestId): Boolean {
        val cursor = downloadManager.query(SystemDownloadManager.Query())

        while (cursor.moveToNext()) {
            val facade = DownloadCursorFacade(cursor)
            val desc = checkNotNull(facade.desc)
            if (desc == requestId.value) {
                return true
            }
        }

        return false
    }

    private fun generateFileName(extension: String?): String {
        val dottedExtension = extension
            ?.let { ".$it" }
            .orEmpty()
        return "${UUID.randomUUID()}$dottedExtension"
    }

    public override fun remove(requestId: DownloadManager.RequestId) {
        val listenersForId = listeners[requestId].orEmpty()
        listenersForId.forEach { it.onDownloadCancelled(requestId) }
        listeners.remove(requestId)
        if (!listeners.any { it.value.isNotEmpty() }) {
            maybeStopObservingProgress()
        }

        val cursor = downloadManager.query(SystemDownloadManager.Query())

        while (cursor.moveToNext()) {
            val facade = DownloadCursorFacade(cursor)
            val id = facade.id
            val desc = checkNotNull(facade.desc)
            if (desc == requestId.value) {
                downloadManager.remove(id)
            }
        }
    }

    private fun createRequest(
        id: DownloadManager.RequestId,
        uri: Uri,
        filename: String,
        headers: Map<String, List<String>>
    ): SystemDownloadManager.Request =
        SystemDownloadManager.Request(uri)
            .setNotificationVisibility(SystemDownloadManager.Request.VISIBILITY_HIDDEN)
            .setDescription(id.value)
            .setDestination(filename)
            .setHeaders(headers)
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
            val desc = checkNotNull(facade.desc)
            val requestId = DownloadManager.RequestId(desc)
            val listenersForId = listeners[requestId].orEmpty()
            if (listenersForId.isNotEmpty()) {
                notifyDownload(requestId, facade, listenersForId)
            }
            knownDownloads.add(requestId)
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
                listeners.remove(id)
                maybeStopObservingProgress()
            }
            SystemDownloadManager.STATUS_PAUSED -> {}
            SystemDownloadManager.STATUS_PENDING -> {}
            SystemDownloadManager.STATUS_SUCCESSFUL -> {
                val download = DownloadManager.Download(
                    file = Uri.parse(facade.localUri!!)!!.toFile(),
                    mediaType = facade.mediaType?.let { MediaType(it) }
                )
                listenersForId.forEach { it.onDownloadCompleted(id, download) }
                listeners.remove(id)
                maybeStopObservingProgress()
            }
            SystemDownloadManager.STATUS_RUNNING -> {
                listenersForId.forEach {
                    it.onDownloadProgressed(id, facade.downloadedSoFar, facade.expected)
                }
            }
        }
    }

    private fun mapErrorCode(code: Int): DownloadManager.DownloadError =
        when (code) {
            in 400 until 1000 ->
                DownloadManager.DownloadError.Http(httpErrorForCode(code))
            SystemDownloadManager.ERROR_UNHANDLED_HTTP_CODE ->
                DownloadManager.DownloadError.Http(httpErrorForCode(code))
            SystemDownloadManager.ERROR_HTTP_DATA_ERROR ->
                DownloadManager.DownloadError.Http(HttpError.MalformedResponse(null))
            SystemDownloadManager.ERROR_TOO_MANY_REDIRECTS ->
                DownloadManager.DownloadError.Http(
                    HttpError.Redirection(DebugError("Too many redirects."))
                )
            SystemDownloadManager.ERROR_CANNOT_RESUME ->
                DownloadManager.DownloadError.CannotResume()
            SystemDownloadManager.ERROR_DEVICE_NOT_FOUND ->
                DownloadManager.DownloadError.FileSystem(
                    FileSystemError.FileNotFound(
                        DebugError("Missing device.")
                    )
                )
            SystemDownloadManager.ERROR_FILE_ERROR ->
                DownloadManager.DownloadError.FileSystem(
                    FileSystemError.IO(DebugError("An error occurred on the filesystem."))
                )
            SystemDownloadManager.ERROR_INSUFFICIENT_SPACE ->
                DownloadManager.DownloadError.FileSystem(FileSystemError.InsufficientSpace())
            SystemDownloadManager.ERROR_UNKNOWN ->
                DownloadManager.DownloadError.Unknown()
            else ->
                DownloadManager.DownloadError.Unknown()
        }

    private fun httpErrorForCode(code: Int): HttpError =
        when (code) {
            in 0 until 1000 -> HttpError.ErrorResponse(HttpStatus(code))
            else -> HttpError.MalformedResponse(DebugError("Unknown HTTP status code."))
        }

    public override fun close() {
        listeners.clear()
        coroutineScope.cancel()
    }
}
