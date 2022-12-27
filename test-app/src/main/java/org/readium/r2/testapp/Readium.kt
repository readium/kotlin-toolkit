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
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer

/**
 * Holds the shared Readium objects and services used by the app.
 */
class Readium(context: Context) {

    /**
     * The LCP service decrypts LCP-protected publication and acquire publications from a
     * license file.
     */
    val lcpService = LcpService(context)
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
        pdfFactory = PdfiumDocumentFactory(context)
    )
}

@OptIn(ExperimentalReadiumApi::class)
val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")
