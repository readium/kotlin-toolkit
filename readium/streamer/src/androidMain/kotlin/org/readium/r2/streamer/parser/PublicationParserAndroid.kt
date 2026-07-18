/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser

import android.content.Context
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.pdf.PdfDocumentFactory

/**
 * Creates a [DefaultPublicationParser] caching the opened PDF documents in memory, releasing them
 * on memory pressure signals from Android.
 *
 * @param context Android context, used to observe memory pressure.
 * @param additionalParsers Parsers used to open a publication, in addition to the default parsers. They take precedence over the default ones.
 * @param httpClient Service performing HTTP requests.
 * @param pdfFactory Parses a PDF document, optionally protected by password.
 * @param assetRetriever Opens assets in case of indirection.
 */
public fun DefaultPublicationParser(
    context: Context,
    httpClient: HttpClient,
    assetRetriever: AssetRetriever,
    pdfFactory: PdfDocumentFactory<*>?,
    additionalParsers: List<PublicationParser> = emptyList(),
): DefaultPublicationParser =
    DefaultPublicationParser(
        httpClient = httpClient,
        assetRetriever = assetRetriever,
        pdfFactory = pdfFactory,
        additionalParsers = additionalParsers,
        cacheServiceFactory = InMemoryCacheService.createFactory(context.applicationContext)
    )
