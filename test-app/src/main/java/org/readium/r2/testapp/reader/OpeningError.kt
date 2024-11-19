/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import org.readium.r2.lcp.LcpError
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R
import org.readium.r2.testapp.domain.toUserError
import org.readium.r2.testapp.utils.UserError

sealed class OpeningError(
    override val cause: Error?,
) : Error {

    override val message: String =
        "Could not open publication"

    class PublicationError(
        override val cause: org.readium.r2.testapp.domain.PublicationError,
    ) : OpeningError(cause)

    class RestrictedPublication(
        cause: Error,
    ) : OpeningError(cause)

    class CannotRender(cause: Error) :
        OpeningError(cause)

    class AudioEngineInitialization(
        cause: Error,
    ) : OpeningError(cause)

    fun toUserError(): UserError =
        when (this) {
            is AudioEngineInitialization ->
                UserError(R.string.opening_publication_audio_engine_initialization, cause = this)
            is PublicationError ->
                cause.toUserError()
            is RestrictedPublication ->
                (cause as? LcpError)?.toUserError()
                    ?: UserError(R.string.publication_error_restricted, cause = this)
            is CannotRender ->
                UserError(R.string.opening_publication_cannot_render, cause = this)
        }
}
