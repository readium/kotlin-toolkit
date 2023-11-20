/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.DefaultMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.HintMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError

internal object ZipHintMediaTypeSniffer : HintMediaTypeSniffer {

    private val generalSniffer: MediaTypeSniffer =
        DefaultMediaTypeSniffer()

    private val acceptedMediaTypes: List<MediaType> =
        listOf(
            MediaType.EPUB,
            MediaType.READIUM_WEBPUB,
            MediaType.READIUM_AUDIOBOOK,
            MediaType.DIVINA,
            MediaType.LCP_PROTECTED_PDF,
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            MediaType.LPF,
            MediaType.CBZ,
            MediaType.ZAB
        )

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return Try.success(MediaType.ZIP)
        }

        val mediaType = generalSniffer.sniffHints(hints)
            .getOrElse { return Try.failure(it) }

        if (mediaType in acceptedMediaTypes) {
            return Try.success(MediaType.ZIP)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}
