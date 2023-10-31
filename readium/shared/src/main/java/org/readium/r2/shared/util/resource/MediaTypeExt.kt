/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.ContainerMediaTypeSnifferContent
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferContentError
import org.readium.r2.shared.util.mediatype.ResourceMediaTypeSnifferContent
import org.readium.r2.shared.util.tryRecover

public class ResourceMediaTypeSnifferContent(
    private val resource: Resource
) : ResourceMediaTypeSnifferContent {

    override suspend fun read(range: LongRange?): Try<ByteArray, MediaTypeSnifferContentError> =
        resource.safeRead(range)
            .mapFailure { it.toMediaTypeSnifferContentError() }

    override suspend fun length(): Try<Long, MediaTypeSnifferContentError> =
        resource.length()
            .mapFailure { it.toMediaTypeSnifferContentError() }
}

public class ContainerMediaTypeSnifferContent(
    private val container: Container
) : ContainerMediaTypeSnifferContent {

    override suspend fun entries(): Set<Url>? =
        container.entries()?.map { it.url }?.toSet()

    override suspend fun read(url: Url, range: LongRange?): Try<ByteArray, MediaTypeSnifferContentError> =
        container.get(url).safeRead(range)
            .mapFailure { it.toMediaTypeSnifferContentError() }

    override suspend fun length(url: Url): Try<Long, MediaTypeSnifferContentError> =
        container.get(url).length()
            .mapFailure { it.toMediaTypeSnifferContentError() }

}
private suspend fun Resource.safeRead(range: LongRange?): Try<ByteArray, ResourceError> {
    try {
        val length = length()
            .getOrElse { return Try.failure(it) }

        // We only read files smaller than 5MB to avoid an [OutOfMemoryError].
        if (range == null && length > 5 * 1000 * 1000) {
            return Try.failure(
                ResourceError.Other(
                    MessageError("Reading full content of big files is prevented.")
                )
            )
        }
        return read(range)
    } catch (e: OutOfMemoryError) {
        return Try.failure(ResourceError.OutOfMemory(e))
    }
}

internal fun ResourceError.toMediaTypeSnifferContentError() =
    when (this) {
        is ResourceError.Filesystem ->
            MediaTypeSnifferContentError.Filesystem(cause)
        is ResourceError.Forbidden ->
            MediaTypeSnifferContentError.Forbidden(this)
        is ResourceError.InvalidContent ->
        is ResourceError.Network ->
            MediaTypeSnifferContentError.Network(cause)
        is ResourceError.NotFound ->
            MediaTypeSnifferContentError.NotFound(this)
        is ResourceError.Other ->
        is ResourceError.OutOfMemory ->
    }

internal fun Try<MediaType, MediaTypeSniffer.Error>.toResourceTry(): ResourceTry<MediaType> =
    tryRecover {
        when (it) {
            MediaTypeSniffer.Error.NotRecognized ->
                Try.success(MediaType.BINARY)
           else ->
               Try.failure(it)
        }
    }.mapFailure {
        when (it) {
            MediaTypeSniffer.Error.NotRecognized ->
                throw IllegalStateException()
            is MediaTypeSniffer.Error.SourceError -> {
                when (it.cause) {
                    is MediaTypeSnifferContentError.Filesystem ->
                        ResourceError.Filesystem(it.cause.cause)
                    is MediaTypeSnifferContentError.Forbidden ->
                        ResourceError.Forbidden(it.cause.cause)
                    is MediaTypeSnifferContentError.Network ->
                        ResourceError.Network(it.cause.cause)
                    is MediaTypeSnifferContentError.NotFound ->
                        ResourceError.NotFound(it.cause.cause)
                }
            }
        }

    }
