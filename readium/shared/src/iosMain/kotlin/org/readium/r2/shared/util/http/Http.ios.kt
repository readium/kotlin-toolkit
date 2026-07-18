/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import org.readium.r2.shared.util.ThrowableError
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorClientCertificateRejected
import platform.Foundation.NSURLErrorClientCertificateRequired
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorSecureConnectionFailed
import platform.Foundation.NSURLErrorServerCertificateHasBadDate
import platform.Foundation.NSURLErrorServerCertificateHasUnknownRoot
import platform.Foundation.NSURLErrorServerCertificateNotYetValid
import platform.Foundation.NSURLErrorServerCertificateUntrusted
import platform.Foundation.NSURLErrorTimedOut

internal actual fun defaultHttpClientEngine(): HttpClientEngine =
    Darwin.create()

internal actual fun platformMapHttpException(exception: Throwable): HttpError? {
    val nsError = (exception as? DarwinHttpRequestException)?.origin
        ?: return null

    if (nsError.domain != NSURLErrorDomain) {
        return null
    }

    return when (nsError.code) {
        NSURLErrorTimedOut ->
            HttpError.Timeout(ThrowableError(exception))
        NSURLErrorCannotFindHost,
        NSURLErrorCannotConnectToHost,
        NSURLErrorDNSLookupFailed,
        NSURLErrorNotConnectedToInternet,
        ->
            HttpError.Unreachable(ThrowableError(exception))
        NSURLErrorSecureConnectionFailed,
        NSURLErrorServerCertificateHasBadDate,
        NSURLErrorServerCertificateHasUnknownRoot,
        NSURLErrorServerCertificateNotYetValid,
        NSURLErrorServerCertificateUntrusted,
        NSURLErrorClientCertificateRejected,
        NSURLErrorClientCertificateRequired,
        ->
            HttpError.SslHandshake(ThrowableError(exception))
        else -> null
    }
}
