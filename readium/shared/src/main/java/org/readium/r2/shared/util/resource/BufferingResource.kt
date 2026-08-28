/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadableBuffer

/**
 * Wraps a [Resource] and buffers its content.
 *
 * Expensive interaction with the underlying resource is minimized, since most (smaller) requests
 * can be satisfied by accessing the buffer alone. The drawback is that some extra space is required
 * to hold the buffer and that copying takes place when filling that buffer, but this is usually
 * outweighed by the performance benefits.
 *
 * Note that this implementation is pretty limited and the benefits are only apparent when reading
 * forward and consecutively – e.g. when downloading the resource by chunks. The buffer is ignored
 * when reading backward or far ahead.
 *
 * @param resource Underlying resource which will be buffered.
 * @param resourceLength The total length of the resource, when known. This can improve performance
 *        by avoiding requesting the length from the underlying resource.
 * @param bufferSize Size of the buffer chunks to read.
 */
public class BufferingResource(
    private val resource: Resource,
    resourceLength: Long? = null,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
) : Resource by resource {

    private val buffer: ReadableBuffer =
        ReadableBuffer(resource, resourceLength, bufferSize)

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        buffer.read(range)
}

/**
 * Wraps this resource in a [BufferingResource] to improve reading performances.
 *
 * @param resourceLength The total length of the resource, when known. This can improve performance
 *        by avoiding requesting the length from the underlying resource.
 * @param bufferSize Size of the buffer chunks to read.
 */
public fun Resource.buffered(
    resourceLength: Long? = null,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): BufferingResource =
    BufferingResource(resource = this, resourceLength = resourceLength, bufferSize = bufferSize)
