/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.tryRecover

internal class SmartArchiveFactory(
    private val archiveFactory: ArchiveFactory,
    private val formatRegistry: FormatRegistry
) : ArchiveFactory {

    override suspend fun create(
        mediaType: MediaType,
        blob: Blob,
        password: String?
    ): Try<Container<Resource>, ArchiveFactory.Error> =
        archiveFactory.create(mediaType, blob, password)
            .tryRecover { error ->
                when (error) {
                    is ArchiveFactory.Error.FormatNotSupported -> {
                        formatRegistry.parentMediaType(mediaType)
                            ?.let { archiveFactory.create(it, blob, password) }
                            ?: Try.failure(error)
                    }
                    is ArchiveFactory.Error.PasswordsNotSupported ->
                        Try.failure(error)
                    is ArchiveFactory.Error.ReadError ->
                        Try.failure(error)
                }
            }
}