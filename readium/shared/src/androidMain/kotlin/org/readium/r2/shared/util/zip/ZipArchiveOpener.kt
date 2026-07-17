/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

public class ZipArchiveOpener : ArchiveOpener {

    private val fileZipArchiveProvider = FileZipArchiveProvider()

    private val streamingZipArchiveProvider = StreamingZipArchiveProvider()

    override suspend fun open(
        format: Format,
        source: Readable,
    ): Try<ContainerAsset, ArchiveOpener.OpenError> {
        val container = (source as? Resource)?.sourceUrl?.toFile()
            ?.let { fileZipArchiveProvider.open(format, it) }
            ?: streamingZipArchiveProvider.open(format, source)

        return container.map { ContainerAsset(format, it) }
    }

    override suspend fun sniffOpen(
        source: Readable,
    ): Try<ContainerAsset, ArchiveOpener.SniffOpenError> {
        val container = (source as? Resource)?.sourceUrl?.toFile()
            ?.let { fileZipArchiveProvider.sniffOpen(it) }
            ?: streamingZipArchiveProvider.sniffOpen(source)

        return container.map {
            ContainerAsset(
                format = Format(
                    specification = FormatSpecification(Specification.Zip),
                    mediaType = MediaType.ZIP,
                    fileExtension = FileExtension("zip")
                ),
                container = it
            )
        }
    }
}
