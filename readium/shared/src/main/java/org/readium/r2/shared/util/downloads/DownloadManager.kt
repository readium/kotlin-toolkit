/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads

import java.io.File
import java.util.UUID
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.downloads.android.AndroidDownloadManager
import org.readium.r2.shared.util.downloads.foreground.ForegroundDownloadManager
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Manages a set of concurrent files downloaded through HTTP.
 *
 * Choose the implementation that best fits your needs:
 * - [AndroidDownloadManager] for downloading files in the background with the Android system
 * service, even if the app is stopped.
 * - [ForegroundDownloadManager] for a simpler implementation based on HttpClient which cancels
 * the on-going download when the app is closed.
 */
public interface DownloadManager {

    public class Request private constructor(
        public val id: RequestId,
        public val url: AbsoluteUrl,
        public val headers: Map<String, List<String>> = emptyMap()
    ) {
        public constructor(
            url: AbsoluteUrl,
            headers: Map<String, List<String>> = emptyMap()
        ) : this(RequestId(UUID.randomUUID().toString()), url, headers)
    }

    public data class Download(
        val file: File,
        val mediaType: MediaType?
    )

    @JvmInline
    public value class RequestId(public val value: String)

    public sealed class DownloadError(
        override val message: String,
        override val cause: Error? = null
    ) : Error {

        public class Http(
            cause: org.readium.r2.shared.util.http.HttpError
        ) : DownloadError(cause.message, cause)

        public class CannotResume(
            cause: Error? = null
        ) : DownloadError("Download couldn't be resumed.", cause)

        public class FileSystem(
            override val cause: FileSystemError
        ) : DownloadError("IO error on the local device.", cause)

        public class Unknown(
            cause: Error? = null
        ) : DownloadError("An unknown error occurred.", cause)
    }

    public interface Listener {

        /**
         * The download with ID [requestId] has been successfully completed.
         */
        public fun onDownloadCompleted(requestId: RequestId, download: Download)

        /**
         * The request with ID [requestId] has downloaded [downloaded] out of [expected] bytes.
         */
        public fun onDownloadProgressed(requestId: RequestId, downloaded: Long, expected: Long?)

        /**
         * The download with ID [requestId] failed due to [error].
         */
        public fun onDownloadFailed(requestId: RequestId, error: DownloadError)

        /**
         * The download with ID [requestId] has been cancelled.
         */
        public fun onDownloadCancelled(requestId: RequestId)
    }

    /**
     * Submits a new request to this [DownloadManager]. The given [listener] will automatically be
     * registered. Requests already submitted will be ignored.
     *
     * Returns the ID of the download request, which can be used to cancel it.
     */
    public fun submit(request: Request, listener: Listener)

    /**
     * Registers a listener for the download with the given [requestId].
     *
     * If your [DownloadManager] supports background downloading, this should typically be used when
     * you create a new instance after the app restarted.
     */
    public fun register(requestId: RequestId, listener: Listener)

    /**
     * Removes the download with the given [requestId] from the [DownloadManager].
     *
     * If it was ongoing, il will be cancelled.
     */
    public fun remove(requestId: RequestId)

    /**
     * Releases any in-memory resource associated with this [DownloadManager].
     *
     * If the pending downloads cannot continue in the background, they will be cancelled.
     */
    public fun close()
}
