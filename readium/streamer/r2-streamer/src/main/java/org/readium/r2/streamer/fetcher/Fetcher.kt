/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.server.Resources
import java.io.InputStream

@Deprecated("Use [publication.get(link)] to access publication content.")
class Fetcher(var publication: Publication, var container: Container, private val userPropertiesPath: String?, customResources: Resources? = null) {
    private var rootFileDirectory: String
    private var contentFilters: ContentFilters?

    init {
        val rootFilePath = publication.internalData["rootfile"]
                ?: throw Exception("Missing root file")
        if (rootFilePath.isNotEmpty() && rootFilePath.contains('/')) {
            rootFileDirectory = rootFilePath.replaceAfterLast("/", "", rootFilePath)
            rootFileDirectory = rootFileDirectory.dropLast(1)
        } else {
            rootFileDirectory = ""
        }
        contentFilters = getContentFilters(container.rootFile.mimetype, customResources)
    }

    fun data(path: String): ByteArray? {
        var data: ByteArray? = container.data(path)
        if (data != null)
            data = contentFilters?.apply(data, publication, container, path)
        return data
    }

    fun dataStream(path: String): InputStream {
        var inputStream = container.dataInputStream(path)
        inputStream = contentFilters?.apply(inputStream, publication, container, path) ?: inputStream
        return inputStream
    }

    fun dataLength(path: String): Long {
        val relativePath = rootFileDirectory.plus(path)

        publication.resourceWithHref(path) ?: throw Exception("Missing file")
        return container.dataLength(relativePath)
    }

    private fun getContentFilters(mimeType: String?, customResources: Resources? = null): ContentFilters {
        val mediaType = mimeType?.let { MediaType.parse(it) }
        return when (mediaType) {
            MediaType.EPUB -> ContentFiltersEpub(userPropertiesPath, customResources)
            MediaType.CBZ -> ContentFiltersCbz()
            else -> throw Exception("Missing container or MIMEtype")
        }
    }
}