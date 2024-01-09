/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.format.EpubSpecification
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.LcpSpecification
import org.readium.r2.shared.util.format.ZipSpecification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Utility to acquire a protected publication from an LCP License Document.
 */
public class LcpPublicationRetriever(
    context: Context,
    private val downloadManager: DownloadManager,
    private val assetRetriever: AssetRetriever
) {

    @JvmInline
    public value class RequestId(public val value: String)

    public interface Listener {

        /**
         * Called when the publication has been successfully acquired.
         */
        public fun onAcquisitionCompleted(
            requestId: RequestId,
            acquiredPublication: LcpService.AcquiredPublication
        )

        /**
         * The acquisition with ID [requestId] has downloaded [downloaded] out of [expected] bytes.
         */
        public fun onAcquisitionProgressed(
            requestId: RequestId,
            downloaded: Long,
            expected: Long?
        )

        /**
         * The acquisition with ID [requestId] has failed with the given [error].
         */
        public fun onAcquisitionFailed(
            requestId: RequestId,
            error: LcpError
        )

        /**
         * The acquisition with ID [requestId] has been cancelled.
         */
        public fun onAcquisitionCancelled(
            requestId: RequestId
        )
    }

    /**
     * Submits a new request to acquire the publication protected with the given [license].
     *
     * The given [listener] will automatically be registered.
     *
     * Returns the ID of the acquisition request, which can be used to cancel it.
     */
    public fun retrieve(
        license: LicenseDocument,
        listener: Listener
    ): RequestId {
        val requestId = fetchPublication(license)
        addListener(requestId, listener)
        return requestId
    }

    /**
     * Registers a listener for the acquisition with the given [requestId].
     *
     * If the [downloadManager] provided during construction supports background downloading, this
     * should typically be used when you create a new instance after the app restarted.
     */
    public fun register(
        requestId: RequestId,
        listener: Listener
    ) {
        addListener(
            requestId,
            listener,
            onFirstListenerAdded = {
                downloadManager.register(
                    DownloadManager.RequestId(requestId.value),
                    downloadListener
                )
            }
        )
    }

    /**
     * Cancels the acquisition with the given [requestId].
     */
    public fun cancel(requestId: RequestId) {
        downloadManager.cancel(DownloadManager.RequestId(requestId.value))
        downloadsRepository.removeDownload(requestId.value)
    }

    /**
     * Releases any in-memory resource associated with this [LcpPublicationRetriever].
     *
     * If the pending acquisitions cannot continue in the background, they will be cancelled.
     */
    public fun close() {
        downloadManager.close()
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val downloadsRepository: LcpDownloadsRepository =
        LcpDownloadsRepository(context)

    private val downloadListener: DownloadManager.Listener =
        DownloadListener()

    private val listeners: MutableMap<RequestId, MutableList<Listener>> =
        mutableMapOf()

    private fun addListener(
        requestId: RequestId,
        listener: Listener,
        onFirstListenerAdded: () -> Unit = {}
    ) {
        listeners
            .getOrPut(requestId) {
                onFirstListenerAdded()
                mutableListOf()
            }
            .add(listener)
    }

    private fun fetchPublication(
        license: LicenseDocument
    ): RequestId {
        val url = license.publicationLink.url() as AbsoluteUrl

        val requestId = downloadManager.submit(
            request = DownloadManager.Request(
                url = url,
                headers = emptyMap()
            ),
            listener = downloadListener
        )

        downloadsRepository.addDownload(requestId.value, license.json)
        return RequestId(requestId.value)
    }

    private inner class DownloadListener : DownloadManager.Listener {

        override fun onDownloadCompleted(
            requestId: DownloadManager.RequestId,
            download: DownloadManager.Download
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
                                LcpError.wrap(
                                    Exception("Couldn't retrieve license from local storage.")
                                )
                            )
                        }
                        return@launch
                    }
                downloadsRepository.removeDownload(requestId.value)

                val format =
                    assetRetriever.sniffFormat(
                        download.file,
                        FormatHints(
                            mediaTypes = listOfNotNull(
                                license.publicationLink.mediaType,
                                download.mediaType
                            )
                        )
                    ).getOrElse {
                        Format(
                            specification = FormatSpecification(
                                ZipSpecification,
                                EpubSpecification,
                                LcpSpecification
                            ),
                            mediaType = MediaType.EPUB,
                            fileExtension = FileExtension("epub")
                        )
                    }

                try {
                    // Saves the License Document into the downloaded publication
                    val container = createLicenseContainer(download.file, format.specification)
                    container.write(license)
                } catch (e: Exception) {
                    tryOrLog { download.file.delete() }
                    listenersForId.forEach {
                        it.onAcquisitionFailed(lcpRequestId, LcpError.wrap(e))
                    }
                    return@launch
                }

                val acquiredPublication = LcpService.AcquiredPublication(
                    localFile = download.file,
                    suggestedFilename = "${license.id}.${format.fileExtension}",
                    format,
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
            error: DownloadManager.DownloadError
        ) {
            val lcpRequestId = RequestId(requestId.value)
            val listenersForId = checkNotNull(listeners[lcpRequestId])

            downloadsRepository.removeDownload(requestId.value)

            listenersForId.forEach {
                it.onAcquisitionFailed(
                    lcpRequestId,
                    LcpError.Network(ErrorException(error))
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
}
