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

@OptIn(ExperimentalReadiumApi::class)
sealed class TtsError(
    override val message: String,
    override val cause: Error? = null
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
}
