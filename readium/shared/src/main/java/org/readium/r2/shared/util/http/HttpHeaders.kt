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
data class HttpHeaders(val headers: Map<String, List<String>>) {

    companion object {
        operator fun invoke(headers: Map<String, String>) : HttpHeaders =
            HttpHeaders(headers.mapValues { (_, value) -> listOf(value) })
    }

    /**
     * Finds the first value of the first header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     */
    operator fun get(name: String): String? {
        val n = name.lowercase()
        return headers.firstNotNullOfOrNull { (key, value) ->
            if (key.lowercase() == n) value.firstOrNull()
            else null
        }
    }

    /**
     * Finds all the values of the first header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     */
    fun getAll(name: String): List<String> {
        val n = name.lowercase()
        return headers
            .mapNotNull { (key, value) ->
                if (key.lowercase() == n) value
                else null
            }
            .flatten()
    }

    /**
     * Indicates whether this server supports byte range requests.
     */
    val acceptsByteRanges: Boolean get() {
        return get("Accept-Ranges")?.lowercase() == "bytes"
            || get("Content-Range")?.lowercase()?.startsWith("bytes") == true
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
data class HttpRange(
    val start: Long,
    val end: Long?
) {
    fun toLongRange(contentLength: Long): LongRange =
        start..(end ?: (contentLength - 1))
}