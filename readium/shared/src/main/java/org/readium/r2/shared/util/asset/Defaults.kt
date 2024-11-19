/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.archive.CompositeArchiveOpener
import org.readium.r2.shared.util.content.ContentResourceFactory
import org.readium.r2.shared.util.file.FileResourceFactory
import org.readium.r2.shared.util.format.ArchiveSniffer
import org.readium.r2.shared.util.format.AudioSniffer
import org.readium.r2.shared.util.format.BitmapSniffer
import org.readium.r2.shared.util.format.CompositeFormatSniffer
import org.readium.r2.shared.util.format.CssSniffer
import org.readium.r2.shared.util.format.EpubDrmSniffer
import org.readium.r2.shared.util.format.EpubSniffer
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.format.HtmlSniffer
import org.readium.r2.shared.util.format.JavaScriptSniffer
import org.readium.r2.shared.util.format.JsonSniffer
import org.readium.r2.shared.util.format.LcpLicenseSniffer
import org.readium.r2.shared.util.format.LpfSniffer
import org.readium.r2.shared.util.format.Opds1Sniffer
import org.readium.r2.shared.util.format.Opds2Sniffer
import org.readium.r2.shared.util.format.PdfSniffer
import org.readium.r2.shared.util.format.RarSniffer
import org.readium.r2.shared.util.format.RpfSniffer
import org.readium.r2.shared.util.format.RwpmSniffer
import org.readium.r2.shared.util.format.W3cWpubSniffer
import org.readium.r2.shared.util.format.ZipSniffer
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpResourceFactory
import org.readium.r2.shared.util.resource.CompositeResourceFactory
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.shared.util.zip.ZipArchiveOpener

/**
 * Default implementation of [ResourceFactory] supporting file, content and http schemes.
 *
 * @param contentResolver content resolver to use to support content scheme.
 * @param httpClient Http client to use to support http scheme.
 * @param additionalFactories Additional [ResourceFactory] to support additional schemes.
 */
public class DefaultResourceFactory(
    contentResolver: ContentResolver,
    httpClient: HttpClient,
    additionalFactories: List<ResourceFactory> = emptyList(),
) : ResourceFactory by CompositeResourceFactory(
    *additionalFactories.toTypedArray(),
    FileResourceFactory(),
    ContentResourceFactory(contentResolver),
    HttpResourceFactory(httpClient)
)

/**
 * Default implementation of [ArchiveOpener] supporting only ZIP archives.
 *
 * @param additionalOpeners Additional openers to be used.
 */
public class DefaultArchiveOpener(
    additionalOpeners: List<ArchiveOpener> = emptyList(),
) : ArchiveOpener by CompositeArchiveOpener(
    *additionalOpeners.toTypedArray(),
    ZipArchiveOpener()
)

/**
 * Default implementation of [FormatSniffer] guessing as well as possible all formats known by
 * Readium.
 *
 * @param additionalSniffers Additional sniffers to be used to guess content format.
 */
public class DefaultFormatSniffer(
    additionalSniffers: List<FormatSniffer> = emptyList(),
) : FormatSniffer by CompositeFormatSniffer(
    *additionalSniffers.toTypedArray(),
    ZipSniffer,
    RarSniffer,
    EpubSniffer,
    LpfSniffer,
    ArchiveSniffer,
    RpfSniffer,
    PdfSniffer,
    HtmlSniffer,
    BitmapSniffer,
    AudioSniffer,
    JsonSniffer,
    Opds1Sniffer,
    Opds2Sniffer,
    LcpLicenseSniffer,
    EpubDrmSniffer,
    W3cWpubSniffer,
    RwpmSniffer,
    CssSniffer,
    JavaScriptSniffer
)
