/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts

import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.navigator.media.tts.android.AndroidTtsPreferencesEditor
import org.readium.navigator.media.tts.android.AndroidTtsSettings
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
public typealias AndroidTtsNavigatorFactory = TtsNavigatorFactory<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor, AndroidTtsEngine.Error, AndroidTtsEngine.Voice>

@OptIn(ExperimentalReadiumApi::class)
public typealias AndroidTtsNavigator = TtsNavigator<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsEngine.Error, AndroidTtsEngine.Voice>
