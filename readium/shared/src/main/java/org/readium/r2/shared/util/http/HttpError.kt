/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType

public typealias HttpTry<SuccessT> = Try<SuccessT, HttpError>

/**
 * Represents an error occurring during an HTTP activity.
 *
 * @param kind Category of HTTP error.
 * @param mediaType Response media type.
 * @param body Response body.
 * @param cause Underlying error, if any.
 */
public class HttpError(
    public val kind: Kind,
    public val mediaType: MediaType? = null,
    public val body: ByteArray? = null,
    public override val cause: Error? = null
) : Error {

    public enum class Kind(public val message: String) {
        /** The provided request was not valid. */
        MalformedRequest("The provided request was not valid."),

        /** The received response couldn't be decoded. */
        MalformedResponse("The received response could not be decoded."),

        /** The client, server or gateways timed out. */
        Timeout("Request timed out."),

        /** (400) The server cannot or will not process the request due to an apparent client error. */
        BadRequest("The provided request was not valid."),

        /** (401) Authentication is required and has failed or has not yet been provided. */
        Unauthorized("Authentication required."),

        /** (403) The server refuses the action, probably because we don't have the necessary permissions. */
        Forbidden("You are not authorized."),

        /** (404) The requested resource could not be found. */
        NotFound("Page not found."),

        /** (405) Method not allowed. */
        MethodNotAllowed("Method not allowed."),

        /** (4xx) Other client errors */
        ClientError("A client error occurred."),

        /** (5xx) Server errors */
        ServerError("A server error occurred, please try again later."),

        /** The device is offline. */
        Offline("Your Internet connection appears to be offline."),

        /** Too many redirects */
        TooManyRedirects("There were too many redirects to follow."),

        /** The request was cancelled. */
        Cancelled("The request was cancelled."),

        /** An error whose kind is not recognized. */
        Other("A networking error occurred.");

        public companion object {

            /** Resolves the kind of the HTTP error associated with the given [statusCode]. */
            public fun ofStatusCode(statusCode: Int): Kind? =
                when (statusCode) {
                    in 200..399 -> null
                    400 -> BadRequest
                    401 -> Unauthorized
                    403 -> Forbidden
                    404 -> NotFound
                    405 -> MethodNotAllowed
                    in 406..498 -> ClientError
                    499 -> Cancelled
                    in 500..599 -> ServerError
                    else -> MalformedResponse
                }
        }
    }

    override val message: String
        get() = kind.message

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
        public val CANCELLED: HttpError = HttpError(kind = Kind.Cancelled)

        /**
         * Creates an HTTP error from a status code.
         *
         * Returns null if the status code is a success.
         */
        public operator fun invoke(
            statusCode: Int,
            mediaType: MediaType? = null,
            body: ByteArray? = null
        ): HttpError? =
            Kind.ofStatusCode(statusCode)?.let { kind ->
                HttpError(kind, mediaType, body)
            }
    }
}
