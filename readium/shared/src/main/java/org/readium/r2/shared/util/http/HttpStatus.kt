/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

@JvmInline
/**
 * Status code of an HTTP response.
 */
public value class HttpStatus(
    public val code: Int,
) : Comparable<HttpStatus> {

    override fun compareTo(other: HttpStatus): Int =
        code.compareTo(other.code)

    public companion object {

        public val Success: HttpStatus = HttpStatus(200)

        /** (400) The server cannot or will not process the request due to an apparent client error. */
        public val BadRequest: HttpStatus = HttpStatus(400)

        /** (401) Authentication is required and has failed or has not yet been provided. */
        public val Unauthorized: HttpStatus = HttpStatus(401)

        /** (403) The server refuses the action, probably because we don't have the necessary permissions. */
        public val Forbidden: HttpStatus = HttpStatus(403)

        /** (404) The requested resource could not be found. */
        public val NotFound: HttpStatus = HttpStatus(404)

        /** (405) Method not allowed. */
        public val MethodNotAllowed: HttpStatus = HttpStatus(405)

        /** (500) Internal Server Error */
        public val InternalServerError: HttpStatus = HttpStatus(500)
    }
}
