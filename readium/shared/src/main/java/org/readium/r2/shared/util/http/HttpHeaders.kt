/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.InternalReadiumApi

/**
 * Helper to parse HTTP request/response headers.
 */
@InternalReadiumApi
public data class HttpHeaders(val headers: Map<String, List<String>>) {

    public companion object {
        public operator fun invoke(headers: Map<String, String>): HttpHeaders =
            HttpHeaders(headers.mapValues { (_, value) -> listOf(value) })
    }

    /**
     * Finds the last header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     * The returned string can contain a single value or a comma-separated list of values if
     * the field supports it.
     */
    public operator fun get(name: String): String? = getAll(name)
        .lastOrNull()

    /**
     * Finds all the headers matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     * Each item of the returned list can contain a single value or a comma-separated list of
     * values if the field supports it.
     */
    public fun getAll(name: String): List<String> = headers
        .filter { it.key.lowercase() == name.lowercase() }
        .values
        .flatten()

    /**
     * Indicates whether this server supports byte range requests.
     */
    val acceptsByteRanges: Boolean get() {
        return get("Accept-Ranges")?.lowercase() == "bytes" ||
            get("Content-Range")?.lowercase()?.startsWith("bytes") == true
    }

    /**
     * The expected content length for this response, when known.
     *
     * Warning: For byte range requests, this will be the length of the chunk, not the full
     * resource.
     */
    val contentLength: Long? get() =
        get("Content-Length")
            ?.toLongOrNull()
            ?.takeIf { it >= 0 }

    /**
     * Returns the HTTP content range.
     */
    val range: HttpRange? get() {
        val rangeRequest = get("Range") ?: return null
        if (!rangeRequest.startsWith("bytes=")) return null

        val components = rangeRequest
            .removePrefix("bytes=")
            .split(",")
            .firstOrNull()
            ?.split("-", limit = 2)
            ?.map { it.toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return HttpRange(
            start = components.first() ?: return null,
            end = components.getOrNull(1)
        )
    }
}

/**
 * HTTP content range.
 *
 * [end] is inclusive.
 */
public data class HttpRange(
    val start: Long,
    val end: Long?,
) {
    public fun toLongRange(contentLength: Long): LongRange =
        start..(end ?: (contentLength - 1))
}
