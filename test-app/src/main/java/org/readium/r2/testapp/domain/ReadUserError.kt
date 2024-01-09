/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import org.readium.r2.shared.util.content.ContentResolverError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

fun ReadError.toUserError(): UserError = when (this) {
    is ReadError.Access ->
        when (val cause = this.cause) {
            is HttpError -> cause.toUserError()
            is FileSystemError -> cause.toUserError()
            is ContentResolverError -> cause.toUserError()
            else -> UserError(R.string.error_unexpected)
        }

    is ReadError.Decoding -> UserError(R.string.publication_error_invalid_publication)
    is ReadError.OutOfMemory -> UserError(R.string.publication_error_out_of_memory)
    is ReadError.UnsupportedOperation -> UserError(R.string.publication_error_unexpected)
}

fun HttpError.toUserError(): UserError = when (this) {
    is HttpError.IO -> UserError(R.string.publication_error_network_unexpected)
    is HttpError.MalformedResponse -> UserError(R.string.publication_error_network_unexpected)
    is HttpError.Redirection -> UserError(R.string.publication_error_network_unexpected)
    is HttpError.Timeout -> UserError(R.string.publication_error_network_timeout)
    is HttpError.Unreachable -> UserError(R.string.publication_error_network_unreachable)
    is HttpError.SslHandshake -> UserError(R.string.publication_error_network_ssl_handshake)
    is HttpError.ErrorResponse -> when (status) {
        HttpStatus.Forbidden -> UserError(R.string.publication_error_network_forbidden)
        HttpStatus.NotFound -> UserError(R.string.publication_error_network_not_found)
        else -> UserError(R.string.publication_error_network_unexpected)
    }
}

fun FileSystemError.toUserError(): UserError = when (this) {
    is FileSystemError.Forbidden -> UserError(R.string.publication_error_filesystem_unexpected)
    is FileSystemError.IO -> UserError(R.string.publication_error_filesystem_unexpected)
    is FileSystemError.InsufficientSpace -> UserError(
        R.string.publication_error_filesystem_insufficient_space
    )
    is FileSystemError.FileNotFound -> UserError(R.string.publication_error_filesystem_not_found)
}

fun ContentResolverError.toUserError(): UserError = when (this) {
    is ContentResolverError.FileNotFound -> UserError(
        R.string.publication_error_filesystem_not_found
    )
    is ContentResolverError.IO -> UserError(R.string.publication_error_filesystem_unexpected)
    is ContentResolverError.NotAvailable -> UserError(R.string.error_unexpected)
}
