/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError

public object ZipMediaTypeSniffer : MediaTypeSniffer {

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return Try.success(MediaType.ZIP)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(blob: Blob): Try<MediaType, MediaTypeSnifferError> {
        blob.source?.toFile()
            ?.let { return FileZipArchiveProvider().sniffBlob(blob) }

        return StreamingZipArchiveProvider().sniffBlob(blob)
    }
}
