/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads

import java.io.File
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.downloads.android.AndroidDownloadManager
import org.readium.r2.shared.util.downloads.foreground.ForegroundDownloadManager
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

    public data class Request(
        val url: Url,
        val title: String,
        val description: String? = null,
        val headers: Map<String, List<String>> = emptyMap()
    )

    public data class Download(
        val file: File,
        val mediaType: MediaType
    )

    @JvmInline
    public value class RequestId(public val value: String)

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error? = null
    ) : org.readium.r2.shared.util.Error {

        public class NotFound(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("File not found.", cause)

        public class Unreachable(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Server is not reachable.", cause)

        public class Server(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("An error occurred on the server-side.", cause)

        public class Forbidden(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Access to the resource was denied.", cause)

        public class DeviceNotFound(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("The storage device is missing.", cause)

        public class CannotResume(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Download couldn't be resumed.", cause)

        public class InsufficientSpace(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("There is not enough space to complete the download.", cause)

        public class FileError(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("IO error on the local device.", cause)

        public class HttpData(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("A data error occurred at the HTTP level.", cause)

        public class TooManyRedirects(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Too many redirects.", cause)

        public class Unknown(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("An unknown error occurred.", cause)
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
        public fun onDownloadFailed(requestId: RequestId, error: Error)

        /**
         * The download with ID [requestId] has been cancelled.
         */
        public fun onDownloadCancelled(requestId: RequestId)
    }

    /**
     * Submits a new request to this [DownloadManager]. The given [listener] will automatically be
     * registered.
     *
     * Returns the ID of the download request, which can be used to cancel it.
     */
    public fun submit(request: Request, listener: Listener): RequestId

    /**
     * Registers a listener for the download with the given [requestId].
     *
     * If your [DownloadManager] supports background downloading, this should typically be used when
     * you create a new instance after the app restarted.
     */
    public fun register(requestId: RequestId, listener: Listener)

    /**
     * Cancels the download with the given [requestId].
     */
    public fun cancel(requestId: RequestId)

    /**
     * Releases any in-memory resource associated with this [DownloadManager].
     *
     * If the pending downloads cannot continue in the background, they will be cancelled.
     */
    public fun close()
}
