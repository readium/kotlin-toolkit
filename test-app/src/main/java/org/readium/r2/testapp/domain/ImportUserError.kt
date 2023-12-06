/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class ImportUserError(
    override val content: UserError.Content,
    override val cause: UserError?
) : UserError {

    constructor(@StringRes userMessageId: Int) :
        this(UserError.Content(userMessageId), null)

    constructor(cause: UserError) :
        this(UserError.Content(cause), cause)

    class LcpAcquisitionFailed(
        override val cause: LcpUserError
    ) : ImportUserError(cause)

    class PublicationError(
        override val cause: PublicationUserError
    ) : ImportUserError(cause)

    class DownloadFailed(
        val error: DownloadManager.DownloadError
    ) : ImportUserError(R.string.import_publication_download_failed)

    class OpdsError(
        val error: Error
    ) : ImportUserError(R.string.import_publication_no_acquisition)

    class DatabaseError :
        ImportUserError(R.string.import_publication_unable_add_pub_database)

    companion object {

        operator fun invoke(error: ImportError): ImportUserError =
            when (error) {
                is ImportError.DatabaseError ->
                    DatabaseError()
                is ImportError.DownloadFailed ->
                    DownloadFailed(error.cause)
                is ImportError.LcpAcquisitionFailed ->
                    LcpAcquisitionFailed(LcpUserError(error.cause))
                is ImportError.OpdsError ->
                    OpdsError(error.cause)
                is ImportError.PublicationError ->
                    PublicationError(PublicationUserError(error.cause))
            }
    }
}
