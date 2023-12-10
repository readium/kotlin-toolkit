/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.ArchiveOpener
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.SniffError
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.resource.Resource

public class ZipArchiveOpener : ArchiveOpener {

    private val fileZipArchiveProvider = FileZipArchiveProvider()

    private val streamingZipArchiveProvider = StreamingZipArchiveProvider()

    override suspend fun open(
        format: Format,
        source: Readable
    ): Try<Container<Resource>, ArchiveOpener.OpenError> =
        (source as? Resource)?.sourceUrl?.toFile()
            ?.let { fileZipArchiveProvider.open(format, it) }
            ?: streamingZipArchiveProvider.open(format, source)

    override suspend fun sniffOpen(source: Readable): Try<ContainerAsset, SniffError> {
        (source as? Resource)?.sourceUrl?.toFile()
            ?.let { return fileZipArchiveProvider.sniff(it) }

        return streamingZipArchiveProvider.sniffOpen(source)
    }
}
