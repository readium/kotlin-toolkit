/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import java.io.File
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.downloads.DownloadManager
import org.readium.downloads.DownloadManagerProvider
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.data.DownloadRepository

class OpdsDownloader(
    private val downloadRepository: DownloadRepository,
    private val downloadManagerProvider: DownloadManagerProvider,
    private val listener: Listener
) {

    interface Listener {

        fun onDownloadCompleted(publication: File, cover: String?)

        fun onDownloadFailed(error: DownloadManager.Error)
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val managerName: String =
        "opds-downloader"

    private val downloadManager = downloadManagerProvider.createDownloadManager(
        listener = DownloadListener(),
        name = managerName
    )

    private inner class DownloadListener : DownloadManager.Listener {
        override fun onDownloadCompleted(requestId: DownloadManager.RequestId, file: File) {
            coroutineScope.launch {
                val cover = downloadRepository.getOpdsDownloadCover(managerName, requestId.value)
                downloadRepository.removeDownload(managerName, requestId.value)
                listener.onDownloadCompleted(file, cover)
            }
        }

        override fun onDownloadProgressed(
            requestId: DownloadManager.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
        }

        override fun onDownloadFailed(
            requestId: DownloadManager.RequestId,
            error: DownloadManager.Error
        ) {
            listener.onDownloadFailed(error)
        }
    }

    fun download(publication: Publication): Try<Unit, Exception> {
        val publicationUrl = getDownloadURL(publication)
            .getOrElse { return Try.failure(it) }
            .toString()

        val coverUrl = publication
            .images.firstOrNull()?.href

        coroutineScope.launch {
            downloadAsync(publication.metadata.title, publicationUrl, coverUrl)
        }

        return Try.success(Unit)
    }

    private suspend fun downloadAsync(
        publicationTitle: String?,
        publicationUrl: String,
        coverUrl: String?
    ) {
        val requestId = downloadManager.submit(
            DownloadManager.Request(
                Url(publicationUrl)!!,
                title = publicationTitle ?: "Untitled publication",
                description = "Downloading",
                headers = emptyMap()
            )
        )
        downloadRepository.insertOpdsDownload(
            manager = managerName,
            id = requestId.value,
            cover = coverUrl
        )
    }

    private fun getDownloadURL(publication: Publication): Try<URL, Exception> =
        publication.links
            .firstOrNull { it.mediaType?.isPublication == true || it.mediaType == MediaType.LCP_LICENSE_DOCUMENT }
            ?.let {
                try {
                    Try.success(URL(it.href))
                } catch (e: Exception) {
                    Try.failure(e)
                }
            } ?: Try.failure(Exception("No supported link to acquire publication."))
}
