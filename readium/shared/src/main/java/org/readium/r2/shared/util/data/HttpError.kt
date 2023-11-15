/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.http.ProblemDetails
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents an error occurring during an HTTP activity.
 */
public sealed class HttpError(
    public override val message: String,
    public override val cause: Error? = null
) : Error {

    public class MalformedResponse(cause: Error?) :
        HttpError("The received response could not be decoded.", cause)

    /** The client, server or gateways timed out. */
    public class Timeout(cause: Error) :
        HttpError("Request timed out.", cause)

    public class UnreachableHost(cause: Error) :
        HttpError("Host could not be reached.", cause)

    public class Cancelled(cause: Error) :
        HttpError("The request was cancelled.", cause)

    /** An unknown networking error. */
    public class Other(cause: Error) :
        HttpError("A networking error occurred.", cause)

    /*
     * @param kind Category of HTTP error.
     * @param mediaType Response media type.
     * @param body Response body.
     */
    public class Response(
        public val kind: Kind,
        public val statusCode: Int,
        public val mediaType: MediaType? = null,
        public val body: ByteArray? = null
    ) : HttpError(kind.message, null) {

        /** Response body parsed as a JSON problem details. */
        public val problemDetails: ProblemDetails? by lazy {
            if (body == null || mediaType?.matches(MediaType.JSON_PROBLEM_DETAILS) != true) {
                return@lazy null
            }

            tryOrLog { ProblemDetails.fromJSON(JSONObject(String(body))) }
        }

        public companion object {

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
                    Response(kind, statusCode, mediaType, body)
                }
        }
    }

    public enum class Kind(public val message: String) {
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
        ServerError("A server error occurred, please try again later.");

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
                    in 406..499 -> ClientError
                    in 500..599 -> ServerError
                    else -> null
                }
        }
    }
}
