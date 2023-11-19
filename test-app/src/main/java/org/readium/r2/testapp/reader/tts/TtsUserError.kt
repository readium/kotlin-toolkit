/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import androidx.annotation.StringRes
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

@OptIn(ExperimentalReadiumApi::class)
sealed class TtsUserError(
    override val content: UserError.Content,
    override val cause: UserError? = null
) : UserError {

    constructor(@StringRes userMessageId: Int) :
        this(UserError.Content(userMessageId), null)

    class ContentError(val error: TtsNavigator.Error.ContentError) :
        TtsUserError(R.string.tts_error_other)

    sealed class EngineError(@StringRes userMessageId: Int) : TtsUserError(userMessageId) {

        class Network(val error: AndroidTtsEngine.Error.Network) :
            EngineError(R.string.tts_error_network)

        class Other(val error: AndroidTtsEngine.Error) :
            EngineError(R.string.tts_error_other)
    }

    class ServiceError(val error: Error?) :
        TtsUserError(R.string.error_unexpected)

    class Initialization(val error: Error) :
        TtsUserError(R.string.tts_error_initialization)

    companion object {

        operator fun invoke(error: TtsError): TtsUserError =
            when (error) {
                is TtsError.ContentError ->
                    ContentError(error.cause)
                is TtsError.EngineError.Network ->
                    EngineError.Network(error.cause)
                is TtsError.EngineError.Other ->
                    EngineError.Other(error.cause)
                is TtsError.Initialization ->
                    Initialization(error.cause)
                is TtsError.ServiceError ->
                    ServiceError(error.cause)
            }
    }
}
