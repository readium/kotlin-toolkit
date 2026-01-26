/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import android.net.Uri
import androidx.media3.common.C.LENGTH_UNSET
import androidx.media3.common.C.RESULT_END_OF_INPUT
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.buffered
import org.readium.r2.shared.util.toUrl
import timber.log.Timber

/**
 * An ExoPlayer's [DataSource] which retrieves resources from a [Publication].
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@InternalReadiumApi
public class ExoPlayerDataSource internal constructor(
    private val publication: Publication,
) : BaseDataSource(/* isNetwork = */ true) {

    public class Factory(
        private val publication: Publication,
        private val transferListener: TransferListener? = null,
    ) : DataSource.Factory {

        override fun createDataSource(): DataSource =
            ExoPlayerDataSource(publication).apply {
                if (transferListener != null) {
                    addTransferListener(transferListener)
                }
            }
    }

    private data class OpenedResource(
        val resource: Resource,
        val uri: Uri,
        var position: Long,
        var remaining: Long,
    )

    private var openedResource: OpenedResource? = null

    override fun open(dataSpec: DataSpec): Long {
        val link = dataSpec.uri.toUrl()
            ?.let { publication.linkWithHref(it) }
            ?: throw IllegalStateException(
                "Can't find a [Link] for URI: ${dataSpec.uri}. Make sure you only request resources declared in the manifest."
            )

        val resource = publication.get(link)
            // Significantly improves performances, in particular with deflated ZIP entries.
            ?.buffered(resourceLength = cachedLengths[dataSpec.uri.toString()])
            ?: throw ReadException(
                ReadError.Decoding(
                    DebugError(
                        "Can't find an entry for URI: ${dataSpec.uri}. Publication looks invalid."
                    )
                )
            )

        val bytesToRead =
            if (dataSpec.length != LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                val contentLength = contentLengthOf(dataSpec.uri, resource)
                if (contentLength == null) {
                    LENGTH_UNSET.toLong()
                } else {
                    contentLength - dataSpec.position
                }
            }

        openedResource = OpenedResource(
            resource = resource,
            uri = dataSpec.uri,
            position = dataSpec.position,
            remaining = bytesToRead
        )

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

        val openedResource = openedResource ?: throw IllegalStateException(
            "No opened resource to read from. Did you call open()?"
        )

        if (openedResource.remaining == 0L) {
            return RESULT_END_OF_INPUT
        }

        val bytesToRead = length.toLong().coerceAtMost(openedResource.remaining)

        try {
            val data = runBlocking {
                openedResource.resource
                    .read(
                        range = openedResource.position until (openedResource.position + bytesToRead)
                    )
                    .mapFailure {
                        Timber.v("Failed to read $length bytes of URI $uri at offset $offset.")
                        ReadException(it)
                    }.getOrThrow()
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
            openedResource.remaining -= data.count()
            return data.count()
        } catch (e: Exception) {
            if (e is InterruptedException) {
                return 0
            }
            throw e
        }
    }

    override fun getUri(): Uri? = openedResource?.uri

    override fun close() {
        openedResource?.resource?.close()
        openedResource = null
    }
}
