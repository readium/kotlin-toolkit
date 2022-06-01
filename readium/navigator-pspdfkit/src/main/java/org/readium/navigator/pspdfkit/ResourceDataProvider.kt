/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import com.pspdfkit.document.providers.DataProvider
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.PublicationId
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.isLazyInitialized
import timber.log.Timber

class ResourceDataProvider(
    publicationId: PublicationId,
    private val resource: Resource,
    private val onResourceError: (Resource.Exception) -> Unit = { Timber.e(it) }
) : DataProvider {

    private val identifier: String = runBlocking {
        "$publicationId#${resource.link().href}"
    }

    private val length: Long = runBlocking {
        resource.length()
            .getOrElse {
                onResourceError(it)
                DataProvider.FILE_SIZE_UNKNOWN.toLong()
            }
    }

    override fun getSize(): Long = length

    override fun getTitle(): String? = null

    override fun getUid(): String = identifier

    override fun read(size: Long, offset: Long): ByteArray = runBlocking {
        val range = offset until (offset + size)
        resource.read(range)
            .getOrElse {
                onResourceError(it)
                DataProvider.NO_DATA_AVAILABLE
            }
    }

    override fun release() {
        if (::resource.isLazyInitialized) {
            runBlocking { resource.close() }
        }
    }
}