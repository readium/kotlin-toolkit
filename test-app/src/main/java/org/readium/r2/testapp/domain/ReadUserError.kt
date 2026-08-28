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
            else -> UserError(R.string.error_unexpected, cause = this)
        }

    is ReadError.Decoding -> UserError(R.string.publication_error_invalid_publication, cause = this)
    is ReadError.OutOfMemory -> UserError(R.string.publication_error_out_of_memory, cause = this)
    is ReadError.UnsupportedOperation -> UserError(
        R.string.publication_error_unexpected,
        cause = this
    )
}

fun HttpError.toUserError(): UserError = when (this) {
    is HttpError.IO -> UserError(R.string.publication_error_network_unexpected, cause = this)
    is HttpError.MalformedResponse -> UserError(
        R.string.publication_error_network_unexpected,
        cause = this
    )
    is HttpError.Redirection -> UserError(
        R.string.publication_error_network_unexpected,
        cause = this
    )
    is HttpError.Timeout -> UserError(R.string.publication_error_network_timeout, cause = this)
    is HttpError.Unreachable -> UserError(
        R.string.publication_error_network_unreachable,
        cause = this
    )
    is HttpError.SslHandshake -> UserError(
        R.string.publication_error_network_ssl_handshake,
        cause = this
    )
    is HttpError.ErrorResponse -> when (status) {
        HttpStatus.Forbidden -> UserError(
            R.string.publication_error_network_forbidden,
            cause = this
        )
        HttpStatus.NotFound -> UserError(R.string.publication_error_network_not_found, cause = this)
        else -> UserError(R.string.publication_error_network_unexpected, cause = this)
    }
}

fun FileSystemError.toUserError(): UserError = when (this) {
    is FileSystemError.Forbidden -> UserError(
        R.string.publication_error_filesystem_forbidden,
        cause = this
    )
    is FileSystemError.IO -> UserError(
        R.string.publication_error_filesystem_unexpected,
        cause = this
    )
    is FileSystemError.InsufficientSpace -> UserError(
        R.string.publication_error_filesystem_insufficient_space,
        cause = this
    )
    is FileSystemError.FileNotFound -> UserError(
        R.string.publication_error_filesystem_not_found,
        cause = this
    )
}

fun ContentResolverError.toUserError(): UserError = when (this) {
    is ContentResolverError.FileNotFound -> UserError(
        R.string.publication_error_filesystem_not_found,
        cause = this
    )
    is ContentResolverError.IO -> UserError(
        R.string.publication_error_filesystem_unexpected,
        cause = this
    )
    is ContentResolverError.Forbidden -> UserError(
        R.string.publication_error_filesystem_forbidden,
        cause = this
    )
    is ContentResolverError.NotAvailable -> UserError(R.string.error_unexpected, cause = this)
}
