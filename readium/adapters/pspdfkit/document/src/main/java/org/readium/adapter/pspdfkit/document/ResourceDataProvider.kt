/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.document

import com.pspdfkit.document.providers.DataProvider
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.synchronized
import org.readium.r2.shared.util.toDebugDescription
import timber.log.Timber

internal class ResourceDataProvider(
    resource: Resource,
    private val onResourceError: (ReadError) -> Unit = { Timber.e(it.toDebugDescription()) },
) : DataProvider {

    var error: ReadError? = null

    private val resource =
        // PSPDFKit accesses the resource from multiple threads.
        resource.synchronized()

    private val length by lazy {
        runBlocking {
            resource.length()
                .getOrElse {
                    error = it
                    onResourceError(it)
                    DataProvider.FILE_SIZE_UNKNOWN.toLong()
                }
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
                error = it
                onResourceError(it)
                DataProvider.NO_DATA_AVAILABLE
            }
    }

    override fun release() {
        runBlocking { resource.close() }
    }
}
