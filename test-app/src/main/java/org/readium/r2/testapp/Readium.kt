/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.Context
import android.view.View
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.auth.LcpDialogAuthentication
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.CompositeResourceFactory
import org.readium.r2.shared.util.asset.ContentResourceFactory
import org.readium.r2.shared.util.asset.DefaultMediaTypeSniffer
import org.readium.r2.shared.util.asset.FileResourceFactory
import org.readium.r2.shared.util.asset.HttpResourceFactory
import org.readium.r2.shared.util.asset.MediaTypeRetriever
import org.readium.r2.shared.util.downloads.android.AndroidDownloadManager
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.zip.ZipArchiveFactory
import org.readium.r2.streamer.PublicationFactory

/**
 * Holds the shared Readium objects and services used by the app.
 */
class Readium(context: Context) {

    private val mediaTypeSniffer =
        DefaultMediaTypeSniffer()

    private val archiveFactory =
        ZipArchiveFactory()

    val formatRegistry =
        FormatRegistry()

    private val mediaTypeRetriever =
        MediaTypeRetriever(
            mediaTypeSniffer,
            formatRegistry,
            archiveFactory
        )

    val httpClient = DefaultHttpClient()

    private val resourceFactory = CompositeResourceFactory(
        FileResourceFactory(),
        ContentResourceFactory(context.contentResolver),
        HttpResourceFactory(httpClient)
    )

    val assetRetriever = AssetRetriever(
        mediaTypeRetriever,
        resourceFactory,
        archiveFactory,
        formatRegistry
    )

    val downloadManager = AndroidDownloadManager(
        context = context,
        mediaTypeRetriever = mediaTypeRetriever,
        formatRegistry = formatRegistry,
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
        ?: Try.failure(LcpError.Unknown(MessageError("liblcp is missing on the classpath")))

    private val lcpDialogAuthentication = LcpDialogAuthentication()

    private val contentProtections = listOfNotNull(
        lcpService.getOrNull()?.contentProtection(lcpDialogAuthentication)
    )

    val protectionRetriever = ContentProtectionSchemeRetriever(
        contentProtections
    )

    /**
     * The PublicationFactory is used to parse and open publications.
     */
    val publicationFactory = PublicationFactory(
        context,
        contentProtections = contentProtections,
        formatRegistry = formatRegistry,
        mediaTypeRetriever = mediaTypeRetriever,
        httpClient = httpClient,
        // Only required if you want to support PDF files using the PDFium adapter.
        pdfFactory = PdfiumDocumentFactory(context)
    )

    fun onLcpDialogAuthenticationParentAttached(view: View) {
        lcpDialogAuthentication.onParentViewAttachedToWindow(view)
    }

    fun onLcpDialogAuthenticationParentDetached() {
        lcpDialogAuthentication.onParentViewDetachedFromWindow()
    }
}

@OptIn(ExperimentalReadiumApi::class)
val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")
