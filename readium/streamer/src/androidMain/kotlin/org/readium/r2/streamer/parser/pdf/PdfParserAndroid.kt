/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.pdf

import android.content.Context
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.util.pdf.PdfDocumentFactory

/**
 * Creates a [PdfParser] caching the opened PDF documents in memory, releasing them on
 * memory pressure signals from Android.
 *
 * @param context Android context, used to observe memory pressure.
 * @param pdfFactory Parses a PDF document, optionally protected by password.
 */
public fun PdfParser(
    context: Context,
    pdfFactory: PdfDocumentFactory<*>,
): PdfParser =
    PdfParser(
        pdfFactory = pdfFactory,
        cacheServiceFactory = InMemoryCacheService.createFactory(context.applicationContext)
    )
