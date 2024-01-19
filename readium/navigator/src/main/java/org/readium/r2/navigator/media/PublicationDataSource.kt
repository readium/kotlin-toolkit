/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Everything in this file will be deprecated
@file:Suppress("DEPRECATION")

package org.readium.r2.navigator.media

import android.net.Uri
import com.google.android.exoplayer2.C.LENGTH_UNSET
import com.google.android.exoplayer2.C.RESULT_END_OF_INPUT
import com.google.android.exoplayer2.upstream.BaseDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.buffered
import org.readium.r2.shared.util.toUrl

/**
 * An ExoPlayer's [DataSource] which retrieves resources from a [Publication].
 */
internal class PublicationDataSource(private val publication: Publication) : BaseDataSource(/* isNetwork = */
    true
) {

    class Factory(
        private val publication: Publication,
        private val transferListener: TransferListener? = null
    ) : DataSource.Factory {

        override fun createDataSource(): DataSource =
            PublicationDataSource(publication).apply {
                if (transferListener != null) {
                    addTransferListener(transferListener)
                }
            }
    }

    private data class OpenedResource(
        val resource: Resource,
        val uri: Uri,
        var position: Long
    )

    private var openedResource: OpenedResource? = null

    override fun open(dataSpec: DataSpec): Long {
        val resource = dataSpec.uri.toUrl()
            ?.let { publication.linkWithHref(it) }
            ?.let { publication.get(it) }
            // Significantly improves performances, in particular with deflated ZIP entries.
            ?.buffered(resourceLength = cachedLengths[dataSpec.uri.toString()])
            ?: throw IllegalStateException(
                "Can't find a [Link] for URI: ${dataSpec.uri}. Make sure you only request resources declared in the manifest."
            )

        openedResource = OpenedResource(
            resource = resource,
            uri = dataSpec.uri,
            position = dataSpec.position
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

        val openedResource = openedResource
            ?: throw IllegalStateException("No opened resource to read from. Did you call open()?")

        val data = runBlocking {
            openedResource.resource
                .read(range = openedResource.position until (openedResource.position + length))
                .mapFailure { ReadException(it) }
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
