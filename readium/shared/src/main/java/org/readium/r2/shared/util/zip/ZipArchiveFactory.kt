/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.ArchiveFactory
import org.readium.r2.shared.util.resource.Resource

public class ZipArchiveFactory : ArchiveFactory {

    private val fileZipArchiveProvider = FileZipArchiveProvider()

    private val streamingZipArchiveProvider = StreamingZipArchiveProvider()

    override suspend fun create(
        mediaType: MediaType,
        blob: Blob,
        password: String?
    ): Try<Container<Resource>, ArchiveFactory.Error> =
        blob.source?.toFile()
            ?.let { fileZipArchiveProvider.create(mediaType, blob, password) }
            ?: streamingZipArchiveProvider.create(mediaType, blob, password)
}