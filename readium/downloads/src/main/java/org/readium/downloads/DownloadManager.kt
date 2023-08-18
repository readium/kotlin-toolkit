/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.downloads

import android.net.Uri
import org.readium.r2.shared.error.Error
import org.readium.r2.shared.util.Url

public interface DownloadManager {

    public data class Request(
        val url: Url,
        val headers: Map<String, List<String>>,
        val title: String,
        val description: String,
    )

    @JvmInline
    public value class RequestId(public val value: Long)

    public sealed class Error : org.readium.r2.shared.error.Error {

        override val cause: org.readium.r2.shared.error.Error? =
            null

        public data object NotFound : Error() {

            override val message: String =
                "File not found."
        }

        public data object Unreachable : Error() {

            override val message: String =
                "Server is not reachable."
        }

        public data object Server : Error() {

            override val message: String =
                "An error occurred on the server-side."
        }

        public data object Forbidden : Error() {

            override val message: String =
                "Access to the resource was denied"
        }

        public data object DeviceNotFound : Error() {

            override val message: String =
                "The storage device is missing."
        }

        public data object CannotResume : Error() {

            override val message: String =
                "Download couldn't be resumed."
        }

        public data object InsufficientSpace : Error() {

            override val message: String =
                "There is not enough space to complete the download."
        }

        public data object FileError : Error() {

            override val message: String =
                "IO error on the local device."
        }

        public data object HttpData : Error() {

            override val message: String =
                "A data error occurred at the HTTP level."
        }

        public data object TooManyRedirects : Error() {

            override val message: String =
                "Too many redirects."
        }

        public data object Unknown : Error() {

            override val message: String =
                "An unknown error occurred."
        }
    }

    public interface Listener {

        public fun onDownloadCompleted(requestId: RequestId, destUri: Uri)

        public fun onDownloadProgressed(requestId: RequestId, downloaded: Long, total: Long)

        public fun onDownloadFailed(requestId: RequestId, error: Error)
    }

    public suspend fun submit(request: Request): RequestId

    public suspend fun close()
}
