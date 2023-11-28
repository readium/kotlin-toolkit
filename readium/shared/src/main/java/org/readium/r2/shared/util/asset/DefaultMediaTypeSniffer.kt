/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.mediatype.ArchiveMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.BitmapMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.CompositeMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.EpubMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.HtmlMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.JsonMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.LcpLicenseMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.LpfMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.mediatype.OpdsMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.PdfMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.RarMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.SystemMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.W3cWpubMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.WebPubManifestMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.WebPubMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.XhtmlMediaTypeSniffer
import org.readium.r2.shared.util.zip.ZipMediaTypeSniffer

/**
 * The default composite sniffer provided by Readium for all known formats.
 * The sniffers order is important, because some formats are subsets of other formats.
 */
public class DefaultMediaTypeSniffer : MediaTypeSniffer {

    private val sniffer: MediaTypeSniffer =
        CompositeMediaTypeSniffer(
            listOf(
                WebPubMediaTypeSniffer,
                EpubMediaTypeSniffer,
                LpfMediaTypeSniffer,
                ArchiveMediaTypeSniffer,
                PdfMediaTypeSniffer,
                BitmapMediaTypeSniffer,
                XhtmlMediaTypeSniffer,
                HtmlMediaTypeSniffer,
                OpdsMediaTypeSniffer,
                LcpLicenseMediaTypeSniffer,
                W3cWpubMediaTypeSniffer,
                WebPubManifestMediaTypeSniffer,
                JsonMediaTypeSniffer,
                SystemMediaTypeSniffer,
                ZipMediaTypeSniffer,
                RarMediaTypeSniffer
            )
        )

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        sniffer.sniffHints(hints)

    override suspend fun sniffBlob(readable: Readable): Try<MediaType, MediaTypeSnifferError> =
        sniffer.sniffBlob(readable)

    override suspend fun sniffContainer(container: Container<*>): Try<MediaType, MediaTypeSnifferError> =
        sniffer.sniffContainer(container)
}
