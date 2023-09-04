/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpPublicationRetriever
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.license.model.LicenseDocument
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
import org.readium.r2.testapp.utils.extensions.copyToTempFile
import org.readium.r2.testapp.utils.extensions.moveTo
import org.readium.r2.testapp.utils.tryOrNull
import timber.log.Timber

class PublicationRetriever(
    private val context: Context,
    private val storageDir: File,
    private val assetRetriever: AssetRetriever,
    private val formatRegistry: FormatRegistry,
    private val downloadRepository: DownloadRepository,
    private val downloadManager: DownloadManager,
    private val lcpPublicationRetriever: Try<LcpPublicationRetriever, LcpException>,
    private val listener: Listener
) {

    interface Listener {

        fun onSuccess(publication: File, coverUrl: String?)

        fun onError(error: ImportError)
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private inner class DownloadListener : DownloadManager.Listener {
        override fun onDownloadCompleted(requestId: DownloadManager.RequestId, file: File) {
            coroutineScope.launch {
                val coverUrl = downloadRepository.getOpdsDownloadCover(requestId.value)
                downloadRepository.removeOpdsDownload(requestId.value)
                retrieveFromStorage(file, coverUrl)
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
                listener.onError(ImportError.DownloadFailed(error))
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
                retrieveFromStorage(acquiredPublication.localFile, coverUrl)
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
                listener.onError(ImportError.LcpAcquisitionFailed(error))
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

    fun retrieveFromStorage(
        uri: Uri
    ) {
        coroutineScope.launch {
            val tempFile = uri.copyToTempFile(context, storageDir)
                .getOrElse {
                    listener.onError(ImportError.StorageError(it))
                    return@launch
                }

            retrieveFromStorage(tempFile)
        }
    }

    fun retrieveFromOpds(publication: Publication) {
        coroutineScope.launch {
            val publicationUrl = publication.acquisitionUrl()
                .getOrElse {
                    listener.onError(ImportError.OpdsError(it))
                    return@launch
                }.toString()

            val coverUrl = publication
                .images.firstOrNull()?.href

            val requestId = downloadManager.submit(
                request = DownloadManager.Request(
                    Url(publicationUrl)!!,
                    title = publication.metadata.title ?: "Untitled publication",
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
    }

    private fun Publication.acquisitionUrl(): Try<Url, Exception> {
        val acquisitionLink = links
            .firstOrNull { it.mediaType?.isPublication == true || it.mediaType == MediaType.LCP_LICENSE_DOCUMENT }
            ?: return Try.failure(Exception("No supported link to acquire publication."))

        return Url(acquisitionLink.href)
            ?.let { Try.success(it) }
            ?: Try.failure(Exception("Invalid acquisition url."))
    }

    private suspend fun retrieveFromStorage(
        tempFile: File,
        coverUrl: String? = null
    ) {
        val sourceAsset = assetRetriever.retrieve(tempFile)
            ?: run {
                listener.onError(
                    ImportError.PublicationError(PublicationError.UnsupportedAsset())
                )
                return
            }

        if (
            sourceAsset is Asset.Resource &&
            sourceAsset.mediaType.matches(MediaType.LCP_LICENSE_DOCUMENT)
        ) {
            acquireLcpPublication(sourceAsset, tempFile, coverUrl)
                .getOrElse {
                    listener.onError(ImportError.StorageError(it))
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
            listener.onError(ImportError.StorageError(e))
            return
        }

        listener.onSuccess(libraryFile, coverUrl)
    }

    private suspend fun acquireLcpPublication(
        licenceAsset: Asset.Resource,
        licenceFile: File,
        coverUrl: String?
    ): Try<Unit, ImportError> {
        val lcpRetriever = lcpPublicationRetriever
            .getOrElse { return Try.failure(ImportError.LcpAcquisitionFailed(it)) }

        val license = licenceAsset.resource.read()
            .getOrElse { return Try.failure(ImportError.StorageError(it)) }
            .let {
                try {
                    LicenseDocument(it)
                } catch (e: LcpException) {
                    return Try.failure(
                        ImportError.PublicationError(ImportError.LcpAcquisitionFailed(e))
                    )
                }
            }

        tryOrNull { licenceFile.delete() }

        val requestId = lcpRetriever.retrieve(
            license,
            "Fulfilling Lcp publication",
            null,
            lcpRetrieverListener
        )

        downloadRepository.insertLcpDownload(requestId.value, coverUrl)

        return Try.success(Unit)
    }
}
