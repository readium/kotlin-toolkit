/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.UserException
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.testapp.R

sealed class ImportError(
    content: Content,
    cause: Exception?
) : UserException(content, cause) {

    constructor(@StringRes userMessageId: Int) :
        this(Content(userMessageId), null)

    constructor(cause: UserException) :
        this(Content(cause), cause)

    class LcpAcquisitionFailed(
        override val cause: UserException
    ) : ImportError(cause)

    class PublicationError(
        override val cause: UserException
    ) : ImportError(cause) {

        companion object {

            operator fun invoke(
                error: AssetRetriever.Error
            ): ImportError = PublicationError(
                org.readium.r2.testapp.domain.PublicationError(
                    error
                )
            )

            operator fun invoke(
                error: Publication.OpeningException
            ): ImportError = PublicationError(
                org.readium.r2.testapp.domain.PublicationError(
                    error
                )
            )
        }
    }

    class ImportBookFailed(
        override val cause: Throwable
    ) : ImportError(R.string.import_publication_unexpected_io_exception)

    class DownloadFailed(
        val error: DownloadManager.Error
    ) : ImportError(R.string.import_publication_download_failed)

    class OpdsError(
        override val cause: Throwable
    ) : ImportError(R.string.import_publication_no_acquisition)

    class ImportDatabaseFailed :
        ImportError(R.string.import_publication_unable_add_pub_database)
}
