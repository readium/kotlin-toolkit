/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.document

import com.pspdfkit.document.providers.DataProvider
import java.util.*
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.synchronized
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.isLazyInitialized
import timber.log.Timber

class ResourceDataProvider(
    resource: Resource,
    private val onResourceError: (Resource.Exception) -> Unit = { Timber.e(it) }
) : DataProvider {

    private val resource =
        // PSPDFKit accesses the resource from multiple threads.
        resource.synchronized()

    private val length: Long = runBlocking {
        resource.length()
            .getOrElse {
                onResourceError(it)
                DataProvider.FILE_SIZE_UNKNOWN.toLong()
            }
    }

    override fun getSize(): Long = length

    override fun getTitle(): String? = null

    /**
     * Unique document identifier used in all caching processes in PSPDFKit. Must be equal or
     * shorter than 50 chars. This method must be implemented for caching to work properly.
     */
    // FIXME: Check whether we need to use a persistent ID.
    override fun getUid(): String = UUID.randomUUID().toString().take(50)

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
