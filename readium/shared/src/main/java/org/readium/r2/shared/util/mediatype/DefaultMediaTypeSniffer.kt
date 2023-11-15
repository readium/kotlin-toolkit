/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container

/**
 * The default composite sniffer provided by Readium for all known formats.
 * The sniffers order is important, because some formats are subsets of other formats.
 */
public class DefaultMediaTypeSniffer : MediaTypeSniffer {

    private val sniffer: MediaTypeSniffer =
        CompositeMediaTypeSniffer(
            listOf(
                XhtmlMediaTypeSniffer(),
                HtmlMediaTypeSniffer(),
                OpdsMediaTypeSniffer,
                LcpLicenseMediaTypeSniffer,
                BitmapMediaTypeSniffer,
                WebPubManifestMediaTypeSniffer(),
                WebPubMediaTypeSniffer(),
                W3cWpubMediaTypeSniffer,
                EpubMediaTypeSniffer(),
                LpfMediaTypeSniffer,
                ArchiveMediaTypeSniffer,
                PdfMediaTypeSniffer,
                JsonMediaTypeSniffer
            )
        )

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        sniffer.sniffHints(hints)

    override suspend fun sniffBlob(blob: Blob): Try<MediaType, MediaTypeSnifferError> =
        sniffer.sniffBlob(blob)

    override suspend fun sniffContainer(container: Container<*>): Try<MediaType, MediaTypeSnifferError> =
        sniffer.sniffContainer(container)
}
