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
import org.readium.r2.lcp.LcpPublicationRetriever as ReadiumLcpPublicationRetriever
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.util.AbsoluteUrl
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

/**
 * Retrieves a publication from a remote or local source and import it into the bookshelf storage.
 *
 * If the source file is a LCP license document, the protected publication will be downloaded.
 */
class PublicationRetriever(
    private val listener: Listener,
    createLocalPublicationRetriever: (Listener) -> LocalPublicationRetriever,
    createOpdsPublicationRetriever: (Listener) -> OpdsPublicationRetriever
) {

    private val localPublicationRetriever: LocalPublicationRetriever
    private val opdsPublicationRetriever: OpdsPublicationRetriever

    interface Listener {

        fun onSuccess(publication: File, coverUrl: AbsoluteUrl?)
        fun onProgressed(progress: Double)
        fun onError(error: ImportError)
    }

    init {
        localPublicationRetriever = createLocalPublicationRetriever(object : Listener {
            override fun onSuccess(publication: File, coverUrl: AbsoluteUrl?) {
                listener.onSuccess(publication, coverUrl)
            }

            override fun onProgressed(progress: Double) {
                listener.onProgressed(progress)
            }

            override fun onError(error: ImportError) {
                listener.onError(error)
            }
        })

        opdsPublicationRetriever = createOpdsPublicationRetriever(object : Listener {
            override fun onSuccess(publication: File, coverUrl: AbsoluteUrl?) {
                localPublicationRetriever.retrieve(publication, coverUrl)
            }

            override fun onProgressed(progress: Double) {
                listener.onProgressed(progress)
            }

            override fun onError(error: ImportError) {
                listener.onError(error)
            }
        })
    }

    fun retrieveFromStorage(uri: Uri) {
        localPublicationRetriever.retrieve(uri)
    }

    fun retrieveFromOpds(publication: Publication) {
        opdsPublicationRetriever.retrieve(publication)
    }
}

/**
 * Retrieves a publication from a file (publication or LCP license document) stored on the device.
 */
class LocalPublicationRetriever(
    private val listener: PublicationRetriever.Listener,
    private val context: Context,
    private val storageDir: File,
    private val assetRetriever: AssetRetriever,
    private val formatRegistry: FormatRegistry,
    createLcpPublicationRetriever: (PublicationRetriever.Listener) -> LcpPublicationRetriever?
) {

    private val lcpPublicationRetriever: LcpPublicationRetriever?

    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        lcpPublicationRetriever = createLcpPublicationRetriever(LcpListener())
    }

    /**
     * Retrieves the publication from the given local [uri].
     */
    fun retrieve(uri: Uri) {
        coroutineScope.launch {
            val tempFile = uri.copyToTempFile(context, storageDir)
                .getOrElse {
                    listener.onError(ImportError.StorageError(it))
                    return@launch
                }

            retrieveFromStorage(tempFile)
        }
    }

    /**
     * Retrieves the publication stored at the given [tempFile].
     */
    fun retrieve(
        tempFile: File,
        coverUrl: AbsoluteUrl? = null
    ) {
        coroutineScope.launch {
            retrieveFromStorage(tempFile, coverUrl)
        }
    }

    private suspend fun retrieveFromStorage(
        tempFile: File,
        coverUrl: AbsoluteUrl? = null
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
            if (lcpPublicationRetriever == null) {
                listener.onError(
                    ImportError.PublicationError(PublicationError.UnsupportedAsset())
                )
            } else {
                lcpPublicationRetriever.retrieve(sourceAsset, tempFile, coverUrl)
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

    private inner class LcpListener : PublicationRetriever.Listener {
        override fun onSuccess(publication: File, coverUrl: AbsoluteUrl?) {
            coroutineScope.launch {
                retrieve(publication, coverUrl)
            }
        }

        override fun onProgressed(progress: Double) {
            listener.onProgressed(progress)
        }

        override fun onError(error: ImportError) {
            listener.onError(error)
        }
    }
}

/**
 * Retrieves a publication from an OPDS entry.
 */
class OpdsPublicationRetriever(
    private val listener: PublicationRetriever.Listener,
    private val downloadManager: DownloadManager,
    private val downloadRepository: DownloadRepository
) {

    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        coroutineScope.launch {
            for (download in downloadRepository.all()) {
                downloadManager.register(
                    DownloadManager.RequestId(download.id),
                    downloadListener
                )
            }
        }
    }

    /**
     * Retrieves the file of the given OPDS [publication].
     */
    fun retrieve(publication: Publication) {
        coroutineScope.launch {
            val publicationUrl = publication.acquisitionUrl()
                .getOrElse {
                    listener.onError(ImportError.OpdsError(it))
                    return@launch
                }

            val coverUrl = publication.images.firstOrNull()
                ?.let { publication.url(it) }

            val requestId = downloadManager.submit(
                request = DownloadManager.Request(
                    publicationUrl,
                    headers = emptyMap()
                ),
                listener = downloadListener
            )
            downloadRepository.insert(
                id = requestId.value,
                cover = coverUrl as? AbsoluteUrl
            )
        }
    }

    private fun Publication.acquisitionUrl(): Try<Url, Exception> {
        val acquisitionLink = links
            .firstOrNull { it.mediaType?.isPublication == true || it.mediaType == MediaType.LCP_LICENSE_DOCUMENT }
            ?: return Try.failure(Exception("No supported link to acquire publication."))

        return Try.success(acquisitionLink.url())
    }

    private val downloadListener: DownloadListener =
        DownloadListener()

    private inner class DownloadListener : DownloadManager.Listener {
        override fun onDownloadCompleted(
            requestId: DownloadManager.RequestId,
            download: DownloadManager.Download
        ) {
            coroutineScope.launch {
                val coverUrl = downloadRepository.getCover(requestId.value)
                downloadRepository.remove(requestId.value)
                listener.onSuccess(download.file, coverUrl)
            }
        }

        override fun onDownloadProgressed(
            requestId: DownloadManager.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
            coroutineScope.launch {
                val progression = expected?.let { downloaded.toDouble() / expected } ?: return@launch
                listener.onProgressed(progression)
            }
        }

        override fun onDownloadFailed(
            requestId: DownloadManager.RequestId,
            error: DownloadManager.Error
        ) {
            coroutineScope.launch {
                downloadRepository.remove(requestId.value)
                listener.onError(ImportError.DownloadFailed(error))
            }
        }

        override fun onDownloadCancelled(requestId: DownloadManager.RequestId) {
            coroutineScope.launch {
                Timber.v("Download ${requestId.value} has been cancelled.")
                downloadRepository.remove(requestId.value)
            }
        }
    }
}

/**
 * Retrieves a publication from an LCP license document.
 */
class LcpPublicationRetriever(
    private val listener: PublicationRetriever.Listener,
    private val downloadRepository: DownloadRepository,
    private val lcpPublicationRetriever: ReadiumLcpPublicationRetriever
) {

    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        coroutineScope.launch {
            for (download in downloadRepository.all()) {
                lcpPublicationRetriever.register(
                    ReadiumLcpPublicationRetriever.RequestId(download.id),
                    lcpRetrieverListener
                )
            }
        }
    }

    /**
     * Retrieves a publication protected with the given license.
     */
    fun retrieve(
        licenceAsset: Asset.Resource,
        licenceFile: File,
        coverUrl: AbsoluteUrl?
    ) {
        coroutineScope.launch {
            val license = licenceAsset.resource.read()
                .getOrElse {
                    listener.onError(ImportError.StorageError(it))
                    return@launch
                }
                .let {
                    try {
                        LicenseDocument(it)
                    } catch (e: LcpException) {
                        listener.onError(ImportError.LcpAcquisitionFailed(e))
                        return@launch
                    }
                }

            tryOrNull { licenceFile.delete() }

            val requestId = lcpPublicationRetriever.retrieve(
                license,
                lcpRetrieverListener
            )

            downloadRepository.insert(requestId.value, coverUrl)
        }
    }

    private val lcpRetrieverListener: LcpRetrieverListener =
        LcpRetrieverListener()

    private inner class LcpRetrieverListener : ReadiumLcpPublicationRetriever.Listener {
        override fun onAcquisitionCompleted(
            requestId: ReadiumLcpPublicationRetriever.RequestId,
            acquiredPublication: LcpService.AcquiredPublication
        ) {
            coroutineScope.launch {
                val coverUrl = downloadRepository.getCover(requestId.value)
                downloadRepository.remove(requestId.value)
                listener.onSuccess(acquiredPublication.localFile, coverUrl)
            }
        }

        override fun onAcquisitionProgressed(
            requestId: ReadiumLcpPublicationRetriever.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
            coroutineScope.launch {
                val progression = expected?.let { downloaded.toDouble() / expected } ?: return@launch
                listener.onProgressed(progression)
            }
        }

        override fun onAcquisitionFailed(
            requestId: ReadiumLcpPublicationRetriever.RequestId,
            error: LcpException
        ) {
            coroutineScope.launch {
                downloadRepository.remove(requestId.value)
                listener.onError(ImportError.LcpAcquisitionFailed(error))
            }
        }

        override fun onAcquisitionCancelled(requestId: ReadiumLcpPublicationRetriever.RequestId) {
            coroutineScope.launch {
                Timber.v("Acquisition ${requestId.value} has been cancelled.")
                downloadRepository.remove(requestId.value)
            }
        }
    }
}
