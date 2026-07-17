/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents a successful HTTP response received from a server.
 *
 * @param request Request associated with the response.
 * @param url Final URL of the response.
 * @param statusCode Response status code.
 * @param headers HTTP response headers, indexed by their name.
 * @param mediaType Media type from the `Content-Type` header.
 */
public data class HttpResponse(
    val request: HttpRequest,
    val url: AbsoluteUrl,
    val statusCode: HttpStatus,
    val headers: Map<String, List<String>>,
    val mediaType: MediaType?,
) {

    private val httpHeaders = HttpHeaders(headers)

    /**
     * Finds the last header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     * The returned string can contain a single value or a comma-separated list of values if
     * the field supports it.
     */
    public fun header(name: String): String? = httpHeaders[name]

    /**
     * Finds all the headers matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     * Each item of the returned list can contain a single value or a comma-separated list of
     * values if the field supports it.
     */
    public fun headers(name: String): List<String> = httpHeaders.getAll(name)

    /**
     * Indicates whether this server supports byte range requests.
     */
    val acceptsByteRanges: Boolean get() = httpHeaders.acceptsByteRanges

    /**
     * The expected content length for this response, when known.
     *
     * Warning: For byte range requests, this will be the length of the chunk, not the full
     * resource.
     */
    val contentLength: Long? get() = httpHeaders.contentLength
}
