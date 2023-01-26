/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import org.readium.r2.navigator.media3.tts.TtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
data class AndroidTtsSettings(
    override val language: Language?,
    val voices: Map<Language, String>,
    val pitch: Double,
    val speed: Double,
) : TtsEngine.Settings
