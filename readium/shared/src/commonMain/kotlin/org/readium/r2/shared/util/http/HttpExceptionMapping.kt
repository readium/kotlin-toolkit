/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import org.readium.r2.shared.util.ThrowableError

/**
 * Creates an [HttpError] from an exception raised by the HTTP engine.
 */
internal fun mapHttpException(exception: Throwable): HttpError =
    platformMapHttpException(exception)
        ?: when (exception) {
            is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
                HttpError.Timeout(ThrowableError(exception))
            is UnresolvedAddressException ->
                HttpError.Unreachable(ThrowableError(exception))
            else ->
                HttpError.IO(ThrowableError(exception))
        }

/**
 * Maps engine-specific exceptions which cannot be identified from common code, or null when
 * [exception] is not recognized.
 */
internal expect fun platformMapHttpException(exception: Throwable): HttpError?
