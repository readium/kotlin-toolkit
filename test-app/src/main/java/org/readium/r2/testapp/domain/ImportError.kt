/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import org.readium.r2.lcp.LcpError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.file.FileSystemError
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

    class Publication(
        override val cause: PublicationError
    ) : ImportError(cause)

    class FileSystem(
        override val cause: FileSystemError
    ) : ImportError(cause)

    class DownloadFailed(
        override val cause: DownloadManager.DownloadError
    ) : ImportError(cause)

    class Opds(override val cause: Error) :
        ImportError(cause)

    class Database(override val cause: Error) :
        ImportError(cause)

    fun toUserError(): UserError = when (this) {
        is Database -> UserError(R.string.import_publication_unable_add_pub_database)
        is DownloadFailed -> UserError(R.string.import_publication_download_failed)
        is LcpAcquisitionFailed -> cause.toUserError()
        is Opds -> UserError(R.string.import_publication_no_acquisition)
        is Publication -> cause.toUserError()
        is FileSystem -> cause.toUserError()
    }
}
