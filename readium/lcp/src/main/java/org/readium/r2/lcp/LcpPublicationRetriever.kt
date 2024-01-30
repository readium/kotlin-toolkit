/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.util.sha256
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

    public class Request private constructor(
        public val id: RequestId,
        public val license: LicenseDocument
    ) {
        public constructor(
            license: LicenseDocument
        ) : this(RequestId(UUID.randomUUID().toString()), license)
    }

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
     * Submits a new request to acquire the publication protected with the given the license
     * passed into the [request].
     *
     * The given [listener] will automatically be registered.
     *
     * Returns the ID of the acquisition request, which can be used to cancel it.
     */
    public fun retrieve(
        request: Request,
        listener: Listener
    ) {
        addListener(request.id, listener)

        downloadsRepository.addDownload(request.id.value, request.license.json)

        val downloadRequest =
            DownloadManager.Request(
                url = request.license.publicationLink.url() as AbsoluteUrl,
                headers = emptyMap()
            )

        downloadManager.submit(
            request = downloadRequest,
            listener = downloadListener
        )

        downloadsRepository.confirmDownload(request.id.value, downloadRequest.id)
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
                coroutineScope.launch {
                    val data = downloadsRepository.getDownload(requestId.value)
                        ?: return@launch
                    val downloadId = data.downloadId
                        ?: return@launch

                    downloadManager.register(
                        DownloadManager.RequestId(downloadId.value),
                        downloadListener
                    )
                }
            }
        )
    }

    /**
     * Cancels the acquisition with the given [requestId].
     */
    public fun remove(requestId: RequestId) {
        downloadManager.remove(DownloadManager.RequestId(requestId.value))
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

    init {
        downloadsRepository.removeUnconfirmed()
    }

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

    private inner class DownloadListener : DownloadManager.Listener {

        @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
        override fun onDownloadCompleted(
            requestId: DownloadManager.RequestId,
            download: DownloadManager.Download
        ) {
            coroutineScope.launch {
                val data = downloadsRepository.getDownload(requestId)
                    ?: return@launch // Repository is corrupted.

                val downloadId = checkNotNull(data.downloadId)
                val lcpRequestId = RequestId(data.requestId)
                val listenersForId = checkNotNull(listeners.remove(lcpRequestId))

                fun failWithError(error: LcpError) {
                    listenersForId.forEach {
                        it.onAcquisitionFailed(lcpRequestId, error)
                    }
                    tryOrLog { download.file.delete() }
                    downloadManager.remove(downloadId)
                    downloadsRepository.removeDownload(requestId.value)
                }

                val license = LicenseDocument(data.license)

                license.publicationLink.hash
                    ?.takeIf { download.file.checkSha256(it) == false }
                    ?.run {
                        failWithError(
                            LcpError.Network(
                                Exception("Digest mismatch: download looks corrupted.")
                            )
                        )
                        return@launch
                    }

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
                        when (it) {
                            is AssetRetriever.RetrieveError.Reading -> {
                                failWithError(LcpError.wrap(ErrorException(it)))
                                return@launch
                            }

                            is AssetRetriever.RetrieveError.FormatNotSupported -> {
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
                        }
                    }

                try {
                    // Saves the License Document into the downloaded publication
                    val container = createLicenseContainer(download.file, format.specification)
                    container.write(license)
                } catch (e: Exception) {
                    failWithError(LcpError.wrap(e))
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
                downloadManager.remove(requestId)
                downloadsRepository.removeDownload(requestId.value)
            }
        }

        override fun onDownloadProgressed(
            requestId: DownloadManager.RequestId,
            downloaded: Long,
            expected: Long?
        ) {
            coroutineScope.launch {
                val data = downloadsRepository.getDownload(requestId)
                    ?: return@launch // Repository is corrupted.

                val lcpRequestId = RequestId(data.requestId)
                val listenersForId = checkNotNull(listeners[lcpRequestId])

                listenersForId.forEach {
                    it.onAcquisitionProgressed(
                        lcpRequestId,
                        downloaded,
                        expected
                    )
                }
            }
        }

        override fun onDownloadFailed(
            requestId: DownloadManager.RequestId,
            error: DownloadManager.DownloadError
        ) {
            coroutineScope.launch {
                val data = downloadsRepository.getDownload(requestId)
                    ?: return@launch // Repository is corrupted.

                val lcpRequestId = RequestId(data.requestId)
                val listenersForId = checkNotNull(listeners[lcpRequestId])

                listenersForId.forEach {
                    it.onAcquisitionFailed(
                        lcpRequestId,
                        LcpError.Network(ErrorException(error))
                    )
                }

                listeners.remove(lcpRequestId)
                downloadsRepository.removeDownload(requestId.value)
                downloadManager.remove(requestId)
            }
        }

        override fun onDownloadCancelled(requestId: DownloadManager.RequestId) {
            coroutineScope.launch {
                val data = downloadsRepository.getDownload(requestId)
                    ?: return@launch // Repository is corrupted.

                val lcpRequestId = RequestId(data.requestId)
                val listenersForId = checkNotNull(listeners[lcpRequestId])

                listenersForId.forEach {
                    it.onAcquisitionCancelled(lcpRequestId)
                }

                listeners.remove(lcpRequestId)
                downloadsRepository.removeDownload(requestId.value)
                downloadManager.remove(requestId)
            }
        }
    }

    /**
     * Checks that the sha256 sum of file content matches the expected one.
     * Returns null if we can't decide.
     */
    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    private fun File.checkSha256(expected: String): Boolean? {
        val actual = sha256() ?: return null

        // Supports hexadecimal encoding for compatibility.
        // See https://github.com/readium/lcp-specs/issues/52
        return when (expected.length) {
            44 -> Base64.encode(actual) == expected
            64 -> actual.toHexString() == expected
            else -> null
        }
    }
}
