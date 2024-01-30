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
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpPublicationRetriever as ReadiumLcpPublicationRetriever
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.LcpLicenseSpecification
import org.readium.r2.shared.util.getOrElse
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
    private val publicationImporter: PublicationImporter,
    createLocalPublicationRetriever: (Listener) -> LocalPublicationRetriever,
    createOpdsPublicationRetriever: (Listener) -> OpdsPublicationRetriever
) {

    private val localPublicationRetriever: LocalPublicationRetriever
    private val opdsPublicationRetriever: OpdsPublicationRetriever

    interface Listener {

        fun onSuccess(publication: File, format: Format?, coverUrl: AbsoluteUrl?)
        fun onProgressed(progress: Double)
        fun onError(error: ImportError)
    }

    init {
        localPublicationRetriever = createLocalPublicationRetriever(object : Listener {
            override fun onSuccess(publication: File, format: Format?, coverUrl: AbsoluteUrl?) {
                publicationImporter.import(publication, format, coverUrl)
            }

            override fun onProgressed(progress: Double) {
                listener.onProgressed(progress)
            }

            override fun onError(error: ImportError) {
                listener.onError(error)
            }
        })
        opdsPublicationRetriever = createOpdsPublicationRetriever(object : Listener {
            override fun onSuccess(publication: File, format: Format?, coverUrl: AbsoluteUrl?) {
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
 * Stores the final publication file into the library dir.
 */
class PublicationImporter(
    private val listener: PublicationRetriever.Listener,
    private val storageDir: File,
    private val assetRetriever: AssetRetriever
) {

    private val coroutineScope: CoroutineScope =
        MainScope()

    fun import(tempFile: File, format: Format?, coverUrl: AbsoluteUrl?) {
        coroutineScope.launch {
            val actualFormat = format
                ?: assetRetriever.sniffFormat(tempFile)
                    .getOrElse {
                        listener.onError(
                            ImportError.Publication(PublicationError(it))
                        )
                        return@launch
                    }

            val fileName = "${UUID.randomUUID()}.${actualFormat.fileExtension.value}"
            val libraryFile = File(storageDir, fileName)

            try {
                tempFile.moveTo(libraryFile)
            } catch (e: Exception) {
                Timber.d(e)
                tryOrNull { libraryFile.delete() }
                listener.onError(
                    ImportError.Publication(
                        PublicationError.ReadError(
                            ReadError.Access(FileSystemError.IO(e))
                        )
                    )
                )
                return@launch
            }

            listener.onSuccess(libraryFile, actualFormat, coverUrl)
        }
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
    private val publicationImporter: PublicationImporter,
    createLcpPublicationRetriever: (PublicationRetriever.Listener) -> LcpPublicationRetriever?
) {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val lcpPublicationRetriever = createLcpPublicationRetriever(object : PublicationRetriever.Listener {
        override fun onSuccess(publication: File, format: Format?, coverUrl: AbsoluteUrl?) {
            publicationImporter.import(publication, format, coverUrl)
        }

        override fun onProgressed(progress: Double) {
            listener.onProgressed(progress)
        }

        override fun onError(error: ImportError) {
            listener.onError(error)
        }
    })

    /**
     * Retrieves the publication from the given local [uri].
     */
    fun retrieve(uri: Uri) {
        coroutineScope.launch {
            val tempFile = uri.copyToTempFile(context, storageDir)
                .getOrElse {
                    listener.onError(
                        ImportError.FileSystem(FileSystemError.IO(it))
                    )
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
            .getOrElse {
                listener.onError(
                    ImportError.Publication(PublicationError(it))
                )
                return
            }

        if (
            sourceAsset is ResourceAsset &&
            sourceAsset.format.conformsTo(LcpLicenseSpecification)
        ) {
            if (lcpPublicationRetriever == null) {
                listener.onError(ImportError.MissingLcpSupport)
            } else {
                lcpPublicationRetriever.retrieve(sourceAsset, tempFile, coverUrl)
            }
            return
        }
        listener.onSuccess(tempFile, sourceAsset.format, coverUrl)
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
                if (download.submitted) {
                    downloadManager.register(
                        DownloadManager.RequestId(download.id),
                        downloadListener
                    )
                } else {
                    downloadRepository.remove(download.id)
                    listener.onError(
                        ImportError.InconsistentState(
                            DebugError("Download has never been submitted.")
                        )
                    )
                }
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
                    listener.onError(ImportError.Opds(it))
                    return@launch
                }

            val coverUrl = publication.images.firstOrNull()
                ?.let { publication.url(it) }

            val request = DownloadManager.Request(
                publicationUrl,
                headers = emptyMap()
            )

            downloadRepository.insert(
                id = request.id.value,
                cover = coverUrl as? AbsoluteUrl
            )

            downloadManager.submit(
                request = request,
                listener = downloadListener
            )

            downloadRepository.confirm(request.id.value)
        }
    }

    private fun Publication.acquisitionUrl(): Try<AbsoluteUrl, Error> {
        val acquisitionUrl = links
            .filter { it.mediaType?.isPublication == true || it.mediaType == MediaType.LCP_LICENSE_DOCUMENT }
            .firstNotNullOfOrNull { it.url() as? AbsoluteUrl }
            ?: return Try.failure(DebugError("No supported link to acquire publication."))

        return Try.success(acquisitionUrl)
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
                listener.onSuccess(download.file, null, coverUrl)
                downloadRepository.remove(requestId.value)
                downloadManager.remove(requestId)
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
            error: DownloadManager.DownloadError
        ) {
            coroutineScope.launch {
                listener.onError(ImportError.DownloadFailed(error))
                downloadRepository.remove(requestId.value)
                downloadManager.remove(requestId)
            }
        }

        override fun onDownloadCancelled(requestId: DownloadManager.RequestId) {
            coroutineScope.launch {
                Timber.v("Download ${requestId.value} has been cancelled.")
                downloadRepository.remove(requestId.value)
                downloadManager.remove(requestId)
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
                if (download.submitted) {
                    lcpPublicationRetriever.register(
                        ReadiumLcpPublicationRetriever.RequestId(download.id),
                        lcpRetrieverListener
                    )
                } else {
                    downloadRepository.remove(download.id)
                    listener.onError(
                        ImportError.InconsistentState(
                            DebugError("Acquisition has never been started.")
                        )
                    )
                }
            }
        }
    }

    /**
     * Retrieves a publication protected with the given license.
     */
    fun retrieve(
        licenceAsset: ResourceAsset,
        licenceFile: File,
        coverUrl: AbsoluteUrl?
    ) {
        coroutineScope.launch {
            val license = licenceAsset.resource.read()
                .getOrElse {
                    listener.onError(ImportError.Publication(PublicationError.ReadError(it)))
                    return@launch
                }
                .let {
                    LicenseDocument.fromBytes(it)
                        .getOrElse { error ->
                            listener.onError(
                                ImportError.LcpAcquisitionFailed(error)
                            )
                            return@launch
                        }
                }

            tryOrNull { licenceFile.delete() }

            val request = ReadiumLcpPublicationRetriever.Request(license)

            downloadRepository.insert(request.id.value, coverUrl)

            lcpPublicationRetriever.retrieve(
                request,
                lcpRetrieverListener
            )

            downloadRepository.confirm(request.id.value)
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
                listener.onSuccess(acquiredPublication.localFile, null, coverUrl)
                downloadRepository.remove(requestId.value)
                lcpPublicationRetriever.remove(requestId)
            }
        }

        override fun onAcquisitionProgressed(
            requestId: ReadiumLcpPublicationRetriever.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
            val progression = expected?.let { downloaded.toDouble() / expected } ?: return
            listener.onProgressed(progression)
        }

        override fun onAcquisitionFailed(
            requestId: ReadiumLcpPublicationRetriever.RequestId,
            error: LcpError
        ) {
            coroutineScope.launch {
                listener.onError(
                    ImportError.LcpAcquisitionFailed(error)
                )
                downloadRepository.remove(requestId.value)
                lcpPublicationRetriever.remove(requestId)
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
