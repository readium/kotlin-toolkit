/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.content.ContentResolverError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class ReadUserError(
    override val content: UserError.Content,
    override val cause: UserError? = null
) : UserError {
    constructor(@StringRes userMessageId: Int) :
        this(UserError.Content(userMessageId), null)

    class HttpNotFound(val error: Error) :
        ReadUserError(R.string.publication_error_network_not_found)

    class HttpForbidden(val error: Error) :
        ReadUserError(R.string.publication_error_network_forbidden)

    class HttpConnectivity(val error: Error) :
        ReadUserError(R.string.publication_error_network_connectivity)

    class HttpUnexpected(val error: Error) :
        ReadUserError(R.string.publication_error_network_unexpected)

    class FsNotFound(val error: Error) :
        ReadUserError(R.string.publication_error_filesystem_not_found)

    class FsUnexpected(val error: Error) :
        ReadUserError(R.string.publication_error_filesystem_unexpected)

    class OutOfMemory(val error: Error) :
        ReadUserError(R.string.publication_error_out_of_memory)

    class InvalidPublication(val error: Error) :
        ReadUserError(R.string.publication_error_invalid_publication)

    class Unexpected(val error: Error) :
        ReadUserError(R.string.publication_error_unexpected)

    companion object {

        operator fun invoke(error: ReadError): ReadUserError =
            when (error) {
                is ReadError.Access ->
                    when (val cause = error.cause) {
                        is HttpError -> ReadUserError(cause)
                        is FileSystemError -> ReadUserError(cause)
                        is ContentResolverError -> ReadUserError(cause)
                        else -> Unexpected(cause)
                    }
                is ReadError.Decoding -> InvalidPublication(error)
                is ReadError.OutOfMemory -> OutOfMemory(error)
                is ReadError.UnsupportedOperation -> Unexpected(error)
            }

        private operator fun invoke(error: HttpError): ReadUserError =
            when (error) {
                is HttpError.IO ->
                    HttpUnexpected(error)
                is HttpError.MalformedResponse ->
                    HttpUnexpected(error)
                is HttpError.Redirection ->
                    HttpUnexpected(error)
                is HttpError.Timeout ->
                    HttpConnectivity(error)
                is HttpError.Unreachable ->
                    HttpConnectivity(error)
                is HttpError.Response ->
                    when (error.status) {
                        HttpStatus.Forbidden -> HttpForbidden(error)
                        HttpStatus.NotFound -> HttpNotFound(error)
                        else -> HttpUnexpected(error)
                    }
            }

        private operator fun invoke(error: FileSystemError): ReadUserError =
            when (error) {
                is FileSystemError.Forbidden -> FsUnexpected(error)
                is FileSystemError.IO -> FsUnexpected(error)
                is FileSystemError.NotFound -> FsNotFound(error)
            }

        private operator fun invoke(error: ContentResolverError): ReadUserError =
            when (error) {
                is ContentResolverError.FileNotFound -> FsNotFound(error)
                is ContentResolverError.IO -> FsUnexpected(error)
                is ContentResolverError.NotAvailable -> FsUnexpected(error)
            }
    }
}
