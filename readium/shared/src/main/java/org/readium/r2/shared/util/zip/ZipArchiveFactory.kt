/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

public class ZipArchiveFactory : ArchiveFactory {

    private val fileZipArchiveProvider = FileZipArchiveProvider()

    private val streamingZipArchiveProvider = StreamingZipArchiveProvider()

    override suspend fun create(
        mediaType: MediaType,
        source: Readable
    ): Try<Container<Resource>, ArchiveFactory.Error> =
        (source as? Resource)?.source?.toFile()
            ?.let { fileZipArchiveProvider.create(mediaType, it) }
            ?: streamingZipArchiveProvider.create(mediaType, source)
}
