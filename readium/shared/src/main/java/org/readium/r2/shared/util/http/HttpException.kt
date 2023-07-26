/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import android.content.Context
import androidx.annotation.StringRes
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import org.json.JSONObject
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.mediatype.MediaType

public typealias HttpTry<SuccessT> = Try<SuccessT, HttpException>

/**
 * Represents an error occurring during an HTTP activity.
 *
 * @param kind Category of HTTP error.
 * @param mediaType Response media type.
 * @param body Response body.
 * @param cause Underlying error, if any.
 */
public class HttpException(
    public val kind: Kind,
    public val mediaType: MediaType? = null,
    public val body: ByteArray? = null,
    cause: Throwable? = null,
) : UserException(kind.userMessageId, cause = cause) {

    public enum class Kind(@StringRes public val userMessageId: Int) {
        /** The provided request was not valid. */
        MalformedRequest(R.string.readium_shared_http_exception_malformed_request),
        /** The received response couldn't be decoded. */
        MalformedResponse(R.string.readium_shared_http_exception_malformed_response),
        /** The client, server or gateways timed out. */
        Timeout(R.string.readium_shared_http_exception_timeout),
        /** (400) The server cannot or will not process the request due to an apparent client error. */
        BadRequest(R.string.readium_shared_http_exception_bad_request),
        /** (401) Authentication is required and has failed or has not yet been provided. */
        Unauthorized(R.string.readium_shared_http_exception_unauthorized),
        /** (403) The server refuses the action, probably because we don't have the necessary permissions. */
        Forbidden(R.string.readium_shared_http_exception_forbidden),
        /** (404) The requested resource could not be found. */
        NotFound(R.string.readium_shared_http_exception_not_found),
        /** (4xx) Other client errors */
        ClientError(R.string.readium_shared_http_exception_client_error),
        /** (5xx) Server errors */
        ServerError(R.string.readium_shared_http_exception_server_error),
        /** The device is offline. */
        Offline(R.string.readium_shared_http_exception_offline),
        /** The request was cancelled. */
        Cancelled(R.string.readium_shared_http_exception_cancelled),
        /** An error whose kind is not recognized. */
        Other(R.string.readium_shared_http_exception_other);

        public companion object {

            /** Resolves the kind of the HTTP error associated with the given [statusCode]. */
            public fun ofStatusCode(statusCode: Int): Kind? =
                when (statusCode) {
                    in 200..399 -> null
                    400 -> BadRequest
                    401 -> Unauthorized
                    403 -> Forbidden
                    404 -> NotFound
                    in 405..498 -> ClientError
                    499 -> Cancelled
                    in 500..599 -> ServerError
                    else -> MalformedResponse
                }
        }
    }

    override fun getUserMessage(context: Context, includesCauses: Boolean): String {
        problemDetails?.let { error ->
            var message = error.title
            if (error.detail != null) {
                message += "\n" + error.detail
            }
            return message
        }

        return super.getUserMessage(context, includesCauses)
    }

    override fun getLocalizedMessage(): String {
        var message = "HTTP error: ${kind.name}"
        problemDetails?.let { details ->
            message += ": ${details.title} ${details.detail}"
        }
        return message
    }

    /** Response body parsed as a JSON problem details. */
    public val problemDetails: ProblemDetails? by lazy {
        if (body == null || mediaType?.matches(MediaType.JSON_PROBLEM_DETAILS) != true) {
            return@lazy null
        }

        tryOrLog { ProblemDetails.fromJSON(JSONObject(String(body))) }
    }

    public companion object {

        /**
         * Shortcut for a cancelled HTTP error.
         */
        public val CANCELLED: HttpException = HttpException(kind = Kind.Cancelled)

        /**
         * Creates an HTTP error from a status code.
         *
         * Returns null if the status code is a success.
         */
        public operator fun invoke(
            statusCode: Int,
            mediaType: MediaType? = null,
            body: ByteArray? = null
        ): HttpException? =
            Kind.ofStatusCode(statusCode)?.let { kind ->
                HttpException(kind, mediaType, body)
            }

        /**
         * Creates an HTTP error from a generic exception.
         */
        public fun wrap(cause: Throwable): HttpException {
            val kind = when (cause) {
                is HttpException -> return cause
                is MalformedURLException -> Kind.MalformedRequest
                is CancellationException -> Kind.Cancelled
                is SocketTimeoutException -> Kind.Timeout
                else -> Kind.Other
            }

            return HttpException(kind = kind, cause = cause)
        }
    }
}
