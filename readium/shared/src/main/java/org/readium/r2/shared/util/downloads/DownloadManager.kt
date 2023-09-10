/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads

import java.io.File
import org.readium.r2.shared.util.Url

public interface DownloadManager {

    public data class Request(
        val url: Url,
        val title: String,
        val description: String? = null,
        val headers: Map<String, List<String>> = emptyMap()
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

        public fun onDownloadCompleted(requestId: RequestId, file: File)

        public fun onDownloadProgressed(requestId: RequestId, downloaded: Long, expected: Long?)

        public fun onDownloadFailed(requestId: RequestId, error: Error)

        public fun onDownloadCancelled(requestId: RequestId)
    }

    public fun submit(request: Request, listener: Listener): RequestId

    public fun register(requestId: RequestId, listener: Listener)

    public fun cancel(requestId: RequestId)

    public fun close()
}
