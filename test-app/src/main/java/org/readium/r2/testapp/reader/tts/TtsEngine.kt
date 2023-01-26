/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import org.readium.r2.navigator.media3.androidtts.AndroidTtsEngine
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferences
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferencesEditor
import org.readium.r2.navigator.media3.androidtts.AndroidTtsSettings
import org.readium.r2.navigator.media3.tts.TtsNavigator
import org.readium.r2.navigator.media3.tts.TtsNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
typealias AndroidTtsNavigatorFactory = TtsNavigatorFactory<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor, AndroidTtsEngine.Exception, AndroidTtsEngine.Voice>

@OptIn(ExperimentalReadiumApi::class)
typealias AndroidTtsNavigator = TtsNavigator<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsEngine.Exception, AndroidTtsEngine.Voice>
