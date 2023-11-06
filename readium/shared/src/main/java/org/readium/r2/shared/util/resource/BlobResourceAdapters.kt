package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.tryRecover

internal class KnownMediaTypeResourceAdapter(
    private val blob: Blob<ReadError>,
    private val mediaType: MediaType
) : Resource, Blob<ReadError> by blob {

    override suspend fun mediaType(): Try<MediaType, ReadError> =
        Try.success(mediaType)

    override suspend fun properties(): Try<Resource.Properties, ReadError> {
        return Try.success(Resource.Properties())
    }
}

internal class GuessMediaTypeResourceAdapter(
    private val blob: Blob<ReadError>,
    private val mediaTypeRetriever: MediaTypeRetriever,
    private val mediaTypeHints: MediaTypeHints
) : Resource, Blob<ReadError> by blob {

    override suspend fun mediaType(): Try<MediaType, ReadError> =
        mediaTypeRetriever.retrieve(
            hints = mediaTypeHints,
            blob = blob
        ).tryRecover { error ->
            when (error) {
                is MediaTypeSnifferError.DataAccess ->
                    Try.failure(error.cause)
                MediaTypeSnifferError.NotRecognized ->
                    Try.success(MediaType.BINARY)
            }
        }

    override suspend fun properties(): Try<Resource.Properties, ReadError> {
        return Try.success(Resource.Properties())
    }
}
