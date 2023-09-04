/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import java.io.File
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpPublicationRetriever
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.data.DownloadRepository
import org.readium.r2.testapp.utils.extensions.moveTo
import org.readium.r2.testapp.utils.tryOrNull
import timber.log.Timber

class PublicationRetriever(
    private val storageDir: File,
    private val assetRetriever: AssetRetriever,
    private val formatRegistry: FormatRegistry,
    private val downloadRepository: DownloadRepository,
    private val downloadManager: DownloadManager,
    private val lcpPublicationRetriever: Try<LcpPublicationRetriever, Exception>,
    private val listener: Listener
) {

    interface Listener {

        fun onImportSucceeded(publication: File, coverUrl: String?)

        fun onImportError(error: ImportError)
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private inner class DownloadListener : DownloadManager.Listener {
        override fun onDownloadCompleted(requestId: DownloadManager.RequestId, file: File) {
            coroutineScope.launch {
                val coverUrl = downloadRepository.getOpdsDownloadCover(requestId.value)
                downloadRepository.removeOpdsDownload(requestId.value)
                importFromAppStorage(file, coverUrl)
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
            coroutineScope.launch {
                downloadRepository.removeOpdsDownload(requestId.value)
                listener.onImportError(ImportError.DownloadFailed(error))
            }
        }
    }

    private inner class LcpRetrieverListener : LcpPublicationRetriever.Listener {
        override fun onAcquisitionCompleted(
            requestId: LcpPublicationRetriever.RequestId,
            acquiredPublication: LcpService.AcquiredPublication
        ) {
            coroutineScope.launch {
                val coverUrl = downloadRepository.getLcpDownloadCover(requestId.value)
                downloadRepository.removeLcpDownload(requestId.value)
                importFromAppStorage(acquiredPublication.localFile, coverUrl)
            }
        }

        override fun onAcquisitionProgressed(
            requestId: LcpPublicationRetriever.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
        }

        override fun onAcquisitionFailed(
            requestId: LcpPublicationRetriever.RequestId,
            error: LcpException
        ) {
            coroutineScope.launch {
                downloadRepository.removeLcpDownload(requestId.value)
                listener.onImportError(ImportError.LcpAcquisitionFailed(error))
            }
        }
    }

    private val downloadListener: DownloadListener =
        DownloadListener()

    private val lcpRetrieverListener: LcpRetrieverListener =
        LcpRetrieverListener()

    init {
        coroutineScope.launch {
            for (download in downloadRepository.getOpdsDownloads()) {
                downloadManager.register(
                    DownloadManager.RequestId(download.id),
                    downloadListener
                )
            }

            lcpPublicationRetriever.map { publicationRetriever ->
                for (download in downloadRepository.getLcpDownloads()) {
                    publicationRetriever.register(
                        LcpPublicationRetriever.RequestId(download.id),
                        lcpRetrieverListener
                    )
                }
            }
        }
    }

    fun downloadFromOpds(publication: Publication) {
        val publicationUrl = getDownloadURL(publication)
            .getOrElse {
                listener.onImportError(ImportError.OpdsError(it))
                return
            }.toString()

        val coverUrl = publication
            .images.firstOrNull()?.href

        coroutineScope.launch {
            downloadAsync(publication.metadata.title, publicationUrl, coverUrl)
        }
    }

    private suspend fun downloadAsync(
        publicationTitle: String?,
        publicationUrl: String,
        coverUrl: String?
    ) {
        val requestId = downloadManager.submit(
            request = DownloadManager.Request(
                Url(publicationUrl)!!,
                title = publicationTitle ?: "Untitled publication",
                description = "Downloading",
                headers = emptyMap()
            ),
            listener = downloadListener
        )
        downloadRepository.insertOpdsDownload(
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

    suspend fun importFromAppStorage(
        tempFile: File,
        coverUrl: String? = null
    ) {
        val sourceAsset = assetRetriever.retrieve(tempFile)
            ?: run {
                listener.onImportError(mediaTypeNotSupportedError())
                return
            }

        if (
            sourceAsset is Asset.Resource &&
            sourceAsset.mediaType.matches(MediaType.LCP_LICENSE_DOCUMENT)
        ) {
            acquireLcpPublication(sourceAsset, coverUrl)
                .getOrElse {
                    listener.onImportError(ImportError.ImportBookFailed(it))
                    return
                }
            return
        }

        val fileExtension = formatRegistry.fileExtension(sourceAsset.mediaType) ?: "epub"
        val fileName = "${UUID.randomUUID()}.$fileExtension"
        val libraryFile = File(storageDir, fileName)

        try {
            tempFile.moveTo(libraryFile)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { libraryFile.delete() }
            listener.onImportError(ImportError.ImportBookFailed(e))
            return
        }

        listener.onImportSucceeded(libraryFile, coverUrl)
    }

    private fun mediaTypeNotSupportedError(): ImportError.PublicationError =
        ImportError.PublicationError(
            PublicationError.UnsupportedPublication(
                Publication.OpeningException.UnsupportedAsset(
                    "Unsupported media type"
                )
            )
        )

    private suspend fun acquireLcpPublication(
        licenceAsset: Asset.Resource,
        coverUrl: String?
    ): Try<Unit, Exception> {
        val lcpRetriever = lcpPublicationRetriever
            .getOrElse { return Try.failure(it) }

        val license = licenceAsset.resource.read()
            .getOrElse { return Try.failure(it) }

        val requestId = lcpRetriever.retrieve(
            license,
            "Fulfilling Lcp publication",
            null,
            lcpRetrieverListener
        ).getOrElse {
            return Try.failure(it)
        }

        downloadRepository.insertLcpDownload(requestId.value, coverUrl)

        return Try.success(Unit)
    }
}
