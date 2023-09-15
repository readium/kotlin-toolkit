/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.Context
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpService
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.resource.CompositeArchiveFactory
import org.readium.r2.shared.resource.CompositeResourceFactory
import org.readium.r2.shared.resource.ContentResourceFactory
import org.readium.r2.shared.resource.DefaultArchiveFactory
import org.readium.r2.shared.resource.DirectoryContainerFactory
import org.readium.r2.shared.resource.FileResourceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.channel.ChannelZipArchiveFactory
import org.readium.r2.shared.util.downloads.android.AndroidDownloadManager
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpResourceFactory
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.streamer.PublicationFactory

/**
 * Holds the shared Readium objects and services used by the app.
 */
class Readium(context: Context) {

    private val mediaTypeRetriever = MediaTypeRetriever()

    val formatRegistry = FormatRegistry()

    val httpClient = DefaultHttpClient(
        mediaTypeRetriever = mediaTypeRetriever
    )

    private val archiveFactory = CompositeArchiveFactory(
        DefaultArchiveFactory(mediaTypeRetriever),
        ChannelZipArchiveFactory(mediaTypeRetriever)
    )

    private val resourceFactory = CompositeResourceFactory(
        FileResourceFactory(mediaTypeRetriever),
        CompositeResourceFactory(
            ContentResourceFactory(context.contentResolver),
            HttpResourceFactory(httpClient)
        )
    )

    private val containerFactory = DirectoryContainerFactory(
        mediaTypeRetriever
    )

    val assetRetriever = AssetRetriever(
        mediaTypeRetriever,
        resourceFactory,
        containerFactory,
        archiveFactory,
        context.contentResolver
    )

    val downloadManager = AndroidDownloadManager(
        context = context,
        mediaTypeRetriever = mediaTypeRetriever,
        destStorage = AndroidDownloadManager.Storage.App
    )

    /**
     * The LCP service decrypts LCP-protected publication and acquire publications from a
     * license file.
     */
    val lcpService = LcpService(
        context,
        assetRetriever,
        mediaTypeRetriever,
        downloadManager
    )?.let { Try.success(it) }
        ?: Try.failure(LcpException.Unknown(Exception("liblcp is missing on the classpath")))

    private val contentProtections = listOfNotNull(
        lcpService.getOrNull()?.contentProtection()
    )

    val protectionRetriever = ContentProtectionSchemeRetriever(
        contentProtections,
        mediaTypeRetriever
    )

    /**
     * The PublicationFactory is used to parse and open publications.
     */
    val publicationFactory = PublicationFactory(
        context,
        contentProtections = contentProtections,
        mediaTypeRetriever = mediaTypeRetriever,
        httpClient = httpClient,
        // Only required if you want to support PDF files using the PDFium adapter.
        pdfFactory = PdfiumDocumentFactory(context)
    )
}

@OptIn(ExperimentalReadiumApi::class)
val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")
