/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import android.content.Context
import android.os.Parcelable
import com.pspdfkit.document.providers.DataProvider
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationId
import org.readium.r2.shared.util.getOrElse
import timber.log.Timber

var pdfPublication: Publication? = null

@Parcelize
class ResourceDataProvider(
    private val link: Link,
    private val identifier: String,
    private val onResourceError: (Resource.Exception) -> Unit = { Timber.e(it) }
) : DataProvider, Parcelable {

    @IgnoredOnParcel
    private val resource = requireNotNull(pdfPublication).get(link)

    @IgnoredOnParcel
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
        resource.read(offset until size)
            .getOrElse {
                onResourceError(it)
                DataProvider.NO_DATA_AVAILABLE
            }
    }

    override fun release() = runBlocking {
        resource.close()
    }
}