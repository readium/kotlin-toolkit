/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
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

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> =
        sniffer.sniffBlob(source)

    override suspend fun sniffContainer(container: Container<Readable>): Try<MediaType, MediaTypeSnifferError> =
        sniffer.sniffContainer(container)
}
