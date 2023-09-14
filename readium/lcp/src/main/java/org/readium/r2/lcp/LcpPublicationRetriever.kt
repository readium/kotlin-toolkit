/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * Util to acquire a protected publication from standalone LCPL's bytes.
 */
public class LcpPublicationRetriever(
    context: Context,
    private val downloadManager: DownloadManager,
    private val mediaTypeRetriever: MediaTypeRetriever
) {

    private val coroutineScope: CoroutineScope =
        MainScope()

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

        public fun onAcquisitionCancelled(
            requestId: RequestId
        )
    }

    private inner class DownloadListener : DownloadManager.Listener {

        override fun onDownloadCompleted(
            requestId: DownloadManager.RequestId,
            file: File
        ) {
            coroutineScope.launch {
                val lcpRequestId = RequestId(requestId.value)
                val listenersForId = checkNotNull(listeners[lcpRequestId])

                val license = downloadsRepository.retrieveLicense(requestId.value)
                    ?.let { LicenseDocument(it) }
                    ?: run {
                        listenersForId.forEach {
                            it.onAcquisitionFailed(
                                lcpRequestId,
                                LcpException.wrap(
                                    Exception("Couldn't retrieve license from local storage.")
                                )
                            )
                        }
                        return@launch
                    }
                downloadsRepository.removeDownload(requestId.value)

                val mediaType = mediaTypeRetriever.retrieve(
                    mediaType = license.publicationLink.type
                )
                    ?: MediaType.EPUB

                try {
                    // Saves the License Document into the downloaded publication
                    val container = createLicenseContainer(file, mediaType)
                    container.write(license)
                } catch (e: Exception) {
                    tryOrLog { file.delete() }
                    listenersForId.forEach {
                        it.onAcquisitionFailed(lcpRequestId, LcpException.wrap(e))
                    }
                    return@launch
                }

                val acquiredPublication = LcpService.AcquiredPublication(
                    localFile = file,
                    suggestedFilename = "${license.id}.${formatRegistry.fileExtension(mediaType) ?: "epub"}",
                    mediaType = mediaType,
                    licenseDocument = license
                )

                listenersForId.forEach {
                    it.onAcquisitionCompleted(lcpRequestId, acquiredPublication)
                }
                listeners.remove(lcpRequestId)
            }
        }

        override fun onDownloadProgressed(
            requestId: DownloadManager.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
            val lcpRequestId = RequestId(requestId.value)
            val listenersForId = checkNotNull(listeners[lcpRequestId])

            listenersForId.forEach {
                it.onAcquisitionProgressed(
                    lcpRequestId,
                    downloaded,
                    expected
                )
            }
        }

        override fun onDownloadFailed(
            requestId: DownloadManager.RequestId,
            error: DownloadManager.Error
        ) {
            val lcpRequestId = RequestId(requestId.value)
            val listenersForId = checkNotNull(listeners[lcpRequestId])

            downloadsRepository.removeDownload(requestId.value)

            listenersForId.forEach {
                it.onAcquisitionFailed(
                    lcpRequestId,
                    LcpException.Network(Exception(error.message))
                )
            }

            listeners.remove(lcpRequestId)
        }

        override fun onDownloadCancelled(requestId: DownloadManager.RequestId) {
            val lcpRequestId = RequestId(requestId.value)
            val listenersForId = checkNotNull(listeners[lcpRequestId])
            listenersForId.forEach {
                it.onAcquisitionCancelled(lcpRequestId)
            }
            listeners.remove(lcpRequestId)
        }
    }

    private val formatRegistry: FormatRegistry =
        FormatRegistry()

    private val downloadsRepository: LcpDownloadsRepository =
        LcpDownloadsRepository(context)

    private val downloadListener: DownloadManager.Listener =
        DownloadListener()

    private val listeners: MutableMap<RequestId, MutableList<Listener>> =
        mutableMapOf()

    public fun register(
        requestId: RequestId,
        listener: Listener
    ) {
        listeners.getOrPut(requestId) {
            downloadManager.register(DownloadManager.RequestId(requestId.value), downloadListener)
            mutableListOf()
        }.add(listener)
    }

    public fun retrieve(
        license: LicenseDocument,
        downloadTitle: String,
        downloadDescription: String? = null,
        listener: Listener
    ): RequestId {
        val requestId = fetchPublication(
            license,
            downloadTitle,
            downloadDescription
        )
        register(requestId, listener)
        return requestId
    }

    public fun cancel(requestId: RequestId) {
        downloadManager.cancel(DownloadManager.RequestId(requestId.value))
        downloadsRepository.removeDownload(requestId.value)
    }

    private fun fetchPublication(
        license: LicenseDocument,
        downloadTitle: String,
        downloadDescription: String?
    ): RequestId {
        val url = Url(license.publicationLink.url)

        val requestId = downloadManager.submit(
            request = DownloadManager.Request(
                url = url,
                title = downloadTitle,
                description = downloadDescription,
                headers = emptyMap()
            ),
            listener = downloadListener
        )

        downloadsRepository.addDownload(requestId.value, license.json)
        return RequestId(requestId.value)
    }
}
