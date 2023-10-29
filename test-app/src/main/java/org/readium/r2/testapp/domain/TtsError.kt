package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.testapp.R

@OptIn(ExperimentalReadiumApi::class)
sealed class TtsError(@StringRes userMessageId: Int) : UserException(userMessageId) {

    class ContentError(val error: TtsNavigator.State.Error.ContentError) :
        TtsError(R.string.tts_error_other)

    sealed class EngineError(@StringRes userMessageId: Int) : TtsError(userMessageId) {

        class Network(val error: AndroidTtsEngine.Error.Network) :
            EngineError(R.string.tts_error_network)

        class Other(val error: AndroidTtsEngine.Error) :
            EngineError(R.string.tts_error_other)
    }

    class ServiceError(val exception: Exception) :
        TtsError(R.string.error_unexpected)

    class Initialization() :
        TtsError(R.string.tts_error_initialization)
}
