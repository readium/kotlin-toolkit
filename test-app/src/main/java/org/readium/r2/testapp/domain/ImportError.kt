/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import org.readium.r2.lcp.LcpError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class ImportError(
    override val cause: Error?
) : Error {

    override val message: String =
        "Import failed"

    class LcpAcquisitionFailed(
        override val cause: LcpError
    ) : ImportError(cause)

    class PublicationError(
        override val cause: org.readium.r2.testapp.domain.PublicationError
    ) : ImportError(cause)

    class DownloadFailed(
        override val cause: DownloadManager.DownloadError
    ) : ImportError(cause)

    class OpdsError(override val cause: Error) :
        ImportError(cause)

    class DatabaseError(override val cause: Error) :
        ImportError(cause)

    fun toUserError(): UserError = when (this) {
        is DatabaseError -> UserError(R.string.import_publication_unable_add_pub_database)
        is DownloadFailed -> UserError(R.string.import_publication_download_failed)
        is LcpAcquisitionFailed -> cause.toUserError()
        is OpdsError -> UserError(R.string.import_publication_no_acquisition)
        is PublicationError -> cause.toUserError()
    }
}
