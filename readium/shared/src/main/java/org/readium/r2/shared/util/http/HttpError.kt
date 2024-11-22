/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.data.AccessError
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents an error occurring during an HTTP activity.
 */
public sealed class HttpError(
    public override val message: String,
    public override val cause: Error? = null,
) : AccessError {

    /** Malformed HTTP response. */
    public class MalformedResponse(cause: Error?) :
        HttpError("The received response could not be decoded.", cause)

    /** The client, server or gateways timed out. */
    public class Timeout(cause: Error) :
        HttpError("Request timed out.", cause)

    /** Server could not be reached. */
    public class Unreachable(cause: Error) :
        HttpError("Server could not be reached.", cause)

    /** Redirection failed. */
    public class Redirection(cause: Error) :
        HttpError("Redirection failed.", cause)

    /** SSL Handshake failed. */
    public class SslHandshake(cause: Error) :
        HttpError("SSL handshake failed.", cause)

    /** An unknown networking error. */
    public class IO(cause: Error) :
        HttpError("An IO error occurred.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /**
     * Server responded with an error status code.
     *
     * @param status HTTP status code.
     * @param mediaType Response media type.
     * @param body Response body.
     */
    public class ErrorResponse(
        public val status: HttpStatus,
        public val mediaType: MediaType? = null,
        public val body: ByteArray? = null,
    ) : HttpError("HTTP Error ${status.code}", null) {

        /** Response body parsed as a JSON problem details. */
        public val problemDetails: ProblemDetails? by lazy {
            if (body == null || mediaType?.matches(MediaType.JSON_PROBLEM_DETAILS) != true) {
                return@lazy null
            }

            tryOrLog { ProblemDetails.fromJSON(JSONObject(String(body))) }
        }
    }
}
