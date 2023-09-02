/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.downloads.DownloadManagerProvider
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

public class LcpPublicationRetriever(
    context: Context,
    private val listener: Listener,
    downloadManagerProvider: DownloadManagerProvider,
    private val mediaTypeRetriever: MediaTypeRetriever
) {

    @JvmInline
    public value class RequestId(public val value: String)

    public interface Listener {

        public fun onAcquisitionCompleted(
            requestId: RequestId,
            acquiredPublication: LcpService.AcquiredPublication
        )

        public fun onAcquisitionProgressed(
            requestId: RequestId,
            downloaded: Long,
            expected: Long?
        )

        public fun onAcquisitionFailed(
            requestId: RequestId,
            error: LcpException
        )
    }

    private inner class DownloadListener : DownloadManager.Listener {

        override fun onDownloadCompleted(
            requestId: DownloadManager.RequestId,
            file: File
        ) {
            val lcpRequestId = RequestId(requestId.value)

            val license = LicenseDocument(downloadsRepository.retrieveLicense(requestId.value)!!)
            downloadsRepository.removeDownload(requestId.value)

            val link = license.link(LicenseDocument.Rel.Publication)!!

            val mediaType = mediaTypeRetriever.retrieve(mediaType = link.type)
                ?: MediaType.EPUB

            try {
                // Saves the License Document into the downloaded publication
                val container = createLicenseContainer(file, mediaType)
                container.write(license)
            } catch (e: Exception) {
                tryOrLog { file.delete() }
                listener.onAcquisitionFailed(lcpRequestId, LcpException.wrap(e))
                return
            }

            val acquiredPublication = LcpService.AcquiredPublication(
                localFile = file,
                suggestedFilename = "${license.id}.${formatRegistry.fileExtension(mediaType) ?: "epub"}",
                mediaType = mediaType,
                licenseDocument = license
            )

            listener.onAcquisitionCompleted(lcpRequestId, acquiredPublication)
        }

        override fun onDownloadProgressed(
            requestId: DownloadManager.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
            listener.onAcquisitionProgressed(
                RequestId(requestId.value),
                downloaded,
                expected
            )
        }

        override fun onDownloadFailed(
            requestId: DownloadManager.RequestId,
            error: DownloadManager.Error
        ) {
            listener.onAcquisitionFailed(
                RequestId(requestId.value),
                LcpException.Network(Exception(error.message))
            )
        }
    }

    private val downloadManager: DownloadManager =
        downloadManagerProvider.createDownloadManager(
            DownloadListener(),
            LcpPublicationRetriever::class.qualifiedName!!
        )

    private val formatRegistry: FormatRegistry =
        FormatRegistry()

    private val downloadsRepository: LcpDownloadsRepository =
        LcpDownloadsRepository(context)

    public suspend fun retrieve(
        license: ByteArray,
        downloadTitle: String,
        downloadDescription: String? = null
    ): Try<RequestId, LcpException> {
        return try {
            val licenseDocument = LicenseDocument(license)
            val requestId = fetchPublication(
                licenseDocument,
                downloadTitle,
                downloadDescription
            )
            Try.success(requestId)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    public suspend fun retrieve(
        license: File,
        downloadTitle: String,
        downloadDescription: String
    ): Try<RequestId, LcpException> {
        return try {
            retrieve(license.readBytes(), downloadTitle, downloadDescription)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    public suspend fun close() {
        downloadManager.close()
    }

    public suspend fun cancel(requestId: RequestId) {
        downloadManager.cancel(DownloadManager.RequestId(requestId.value))
        downloadsRepository.removeDownload(requestId.value)
    }

    private suspend fun fetchPublication(
        license: LicenseDocument,
        downloadTitle: String,
        downloadDescription: String?
    ): RequestId {
        val link = license.link(LicenseDocument.Rel.Publication)
        val url = link?.url
            ?: throw LcpException.Parsing.Url(rel = LicenseDocument.Rel.Publication.value)

        val requestId = downloadManager.submit(
            DownloadManager.Request(
                url = Url(url),
                title = downloadTitle,
                description = downloadDescription,
                headers = emptyMap()
            )
        )

        downloadsRepository.addDownload(requestId.value, license.json)
        return RequestId(requestId.value)
    }
}
