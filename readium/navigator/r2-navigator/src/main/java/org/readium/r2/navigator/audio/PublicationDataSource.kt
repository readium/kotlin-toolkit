/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.audio

import android.net.Uri
import com.google.android.exoplayer2.C.LENGTH_UNSET
import com.google.android.exoplayer2.C.RESULT_END_OF_INPUT
import com.google.android.exoplayer2.upstream.BaseDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.buffered
import org.readium.r2.shared.publication.Publication
import java.io.IOException

/**
 * An ExoPlayer's [DataSource] which retrieves resources from a [Publication].
 */
internal class PublicationDataSource(private val publication: Publication) : BaseDataSource(/* isNetwork = */ true) {

    class Factory(private val publication: Publication, private val transferListener: TransferListener? = null) : DataSource.Factory {

        override fun createDataSource(): DataSource =
            PublicationDataSource(publication).apply {
                if (transferListener != null) {
                    addTransferListener(transferListener)
                }
            }

    }

    sealed class Exception(message: String, cause: Throwable?) : IOException(message, cause) {
        class NotOpened(message: String) : Exception(message, null)
        class NotFound(message: String) : Exception(message, null)
        class ReadFailed(uri: Uri, offset: Int, readLength: Int, cause: Throwable) : Exception("Failed to read $readLength bytes of URI $uri at offset $offset.", cause)
    }

    private data class OpenedResource(
        val resource: Resource,
        val uri: Uri,
        var position: Long,
    )

    private var openedResource: OpenedResource? = null

    override fun open(dataSpec: DataSpec): Long {
        val link = publication.linkWithHref(dataSpec.uri.toString())
            ?: throw Exception.NotFound("Can't find a [Link] for URI: ${dataSpec.uri}. Make sure you only request resources declared in the manifest.")

        val resource = publication.get(link)
            // Significantly improves performances, in particular with deflated ZIP entries.
            .buffered(resourceLength = cachedLengths[dataSpec.uri.toString()])

        openedResource = OpenedResource(
            resource = resource,
            uri = dataSpec.uri,
            position = dataSpec.position,
        )

        val bytesToRead =
            if (dataSpec.length != LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                val contentLength = contentLengthOf(dataSpec.uri, resource)
                    ?: return dataSpec.length
                contentLength - dataSpec.position
            }

        return bytesToRead
    }

    /** Cached content lengths indexed by their URL. */
    private var cachedLengths: MutableMap<String, Long> = mutableMapOf()

    private fun contentLengthOf(uri: Uri, resource: Resource): Long? {
        cachedLengths[uri.toString()]?.let { return it }

        val length = runBlocking { resource.length() }.getOrNull()
            ?: return null

        cachedLengths[uri.toString()] = length
        return length
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        if (length <= 0) {
            return 0
        }

        val openedResource = openedResource ?: throw Exception.NotOpened("No opened resource to read from. Did you call open()?")

        try {
            val data = runBlocking {
                openedResource.resource
                    .read(range = openedResource.position until (openedResource.position + length))
                    .getOrThrow()
            }

            if (data.isEmpty()) {
                return RESULT_END_OF_INPUT
            }

            data.copyInto(
                destination = target,
                destinationOffset = offset,
                startIndex = 0,
                endIndex = data.size
            )

            openedResource.position += data.count()
            return data.count()

        } catch (e: Exception) {
            if (e is InterruptedException) {
                return 0
            }
            throw Exception.ReadFailed(uri = openedResource.uri, offset = offset, readLength = length, cause = e)
        }
    }

    override fun getUri(): Uri? = openedResource?.uri

    override fun close() {
        openedResource?.run {
            try {
                runBlocking { resource.close() }
            } catch (e: Exception) {
                if (e !is InterruptedException) {
                    throw e
                }
            }
        }
        openedResource = null
    }

}
