/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.readium

import android.content.Context
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.parser.epub.EpubPositionsService

/**
 * Creates a [ReadiumWebPubParser] caching the opened PDF documents in memory, releasing them on
 * memory pressure signals from Android.
 *
 * @param context Android context, used to observe memory pressure.
 * @param httpClient Service performing HTTP requests.
 * @param pdfFactory Parses a PDF document, optionally protected by password.
 * @param epubReflowablePositionsStrategy Strategy used to calculate the number
 * of positions in a reflowable resource of a web publication conforming to the
 * EPUB profile.
 */
@OptIn(ExperimentalReadiumApi::class)
public fun ReadiumWebPubParser(
    context: Context?,
    httpClient: HttpClient,
    pdfFactory: PdfDocumentFactory<*>?,
    epubReflowablePositionsStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.recommended,
): ReadiumWebPubParser =
    ReadiumWebPubParser(
        httpClient = httpClient,
        pdfFactory = pdfFactory,
        epubReflowablePositionsStrategy = epubReflowablePositionsStrategy,
        cacheServiceFactory = InMemoryCacheService.createFactory(context?.applicationContext)
    )
