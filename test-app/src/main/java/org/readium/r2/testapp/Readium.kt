/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.Context
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.lcp.LcpService
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.asset.AssetFactory
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.resource.CompositeArchiveFactory
import org.readium.r2.shared.resource.CompositeResourceFactory
import org.readium.r2.shared.resource.DefaultArchiveFactory
import org.readium.r2.shared.resource.DirectoryContainerFactory
import org.readium.r2.shared.resource.FileResourceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.channel.ChannelZipArchiveFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpResourceFactory
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.streamer.FetcherFactory
import org.readium.r2.streamer.Streamer

/**
 * Holds the shared Readium objects and services used by the app.
 */
class Readium(context: Context) {

    val httpClient = DefaultHttpClient()

    val archiveFactory = CompositeArchiveFactory(
        DefaultArchiveFactory(),
        ChannelZipArchiveFactory(httpClient)
    )

    val resourceFactory = CompositeResourceFactory(
        FileResourceFactory(),
        HttpResourceFactory(httpClient)
    )

    val containerFactory = DirectoryContainerFactory()

    val assetRetriever = AssetRetriever(
        resourceFactory,
        containerFactory,
        archiveFactory
    )

    val mediaTypeRetriever = MediaTypeRetriever(
        resourceFactory,
        containerFactory,
        archiveFactory
    )

    val assetFactory = AssetFactory(
        archiveFactory,
        resourceFactory,
        containerFactory
    )

    /**
     * The LCP service decrypts LCP-protected publication and acquire publications from a
     * license file.
     */
    val lcpService = LcpService(context, mediaTypeRetriever, resourceFactory, archiveFactory)
        ?.let { Try.success(it) }
        ?: Try.failure(Exception("liblcp is missing on the classpath"))

    /**
     * The Streamer is used to open and parse publications.
     */
    val streamer = Streamer(
        context,
        contentProtections = listOfNotNull(
            lcpService.getOrNull()?.contentProtection()
        ),
        // Only required if you want to support PDF files using the PDFium adapter.
        pdfFactory = PdfiumDocumentFactory(context),
        // Build a composite archive factory to enable remote zip reading.
        fetcherFactory = FetcherFactory(httpClient, mediaTypeRetriever)::createFetcher
    )
}

@OptIn(ExperimentalReadiumApi::class)
val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")
