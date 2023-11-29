/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.Resource

public object ZipMediaTypeSniffer : MediaTypeSniffer {

    private val fileZipArchiveProvider = FileZipArchiveProvider()

    private val streamingZipArchiveProvider = StreamingZipArchiveProvider()

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return Try.success(MediaType.ZIP)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(readable: Readable): Try<MediaType, MediaTypeSnifferError> {
        (readable as? Resource)?.source?.toFile()
            ?.let { return fileZipArchiveProvider.sniffFile(it) }

        return streamingZipArchiveProvider.sniffBlob(readable)
    }
}
