/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.toUrl

internal actual fun defaultHttpClientEngine(): HttpClientEngine =
    OkHttp.create()

internal actual fun platformMapHttpException(exception: Throwable): HttpError? =
    when (exception) {
        is UnknownHostException, is NoRouteToHostException, is ConnectException ->
            HttpError.Unreachable(ThrowableError(exception))
        is SocketTimeoutException ->
            HttpError.Timeout(ThrowableError(exception))
        is SSLHandshakeException ->
            HttpError.SslHandshake(ThrowableError(exception))
        else -> null
    }

/**
 * Downloads the resource from the given [request] to the [destination] file.
 *
 * @param request The [HttpRequest] detailing the resource to be downloaded.
 * @param destination The [File] where the downloaded resource should be saved.
 * @param onProgress A closure called regularly with the download progress, from 0.0 to 1.0.
 */
public suspend fun HttpClient.download(
    request: HttpRequest,
    destination: File,
    onProgress: (Double) -> Unit = {},
): Try<HttpResponse, HttpDownloadError> =
    download(request, destination.toUrl(isDirectory = false), onProgress)
