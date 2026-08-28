/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.TtsNavigatorFactory
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

@OptIn(ExperimentalReadiumApi::class)
sealed class TtsError(
    override val message: String,
    override val cause: Error? = null,
) : Error {

    class ContentError(override val cause: TtsNavigator.Error.ContentError) :
        TtsError(cause.message, cause.cause)

    sealed class EngineError(override val cause: AndroidTtsEngine.Error) :
        TtsError(cause.message, cause.cause) {

        class Network(override val cause: AndroidTtsEngine.Error.Network) :
            EngineError(cause)

        class Other(override val cause: AndroidTtsEngine.Error) :
            EngineError(cause)
    }

    class ServiceError(val exception: Exception) :
        TtsError("Could not open session.", ThrowableError(exception))

    class Initialization(override val cause: TtsNavigatorFactory.Error) :
        TtsError(cause.message, cause)

    fun toUserError(): UserError = when (this) {
        is ContentError -> UserError(R.string.tts_error_other, cause = this)
        is EngineError.Network -> UserError(R.string.tts_error_network, cause = this)
        is EngineError.Other -> UserError(R.string.tts_error_other, cause = this)
        is Initialization -> UserError(R.string.tts_error_initialization, cause = this)
        is ServiceError -> UserError(R.string.error_unexpected, cause = this)
    }
}
