/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import org.readium.r2.navigator.media3.audio.AudioBookNavigator
import org.readium.r2.navigator.media3.audio.AudioNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
typealias ExoPlayersNavigatorFactory = AudioNavigatorFactory<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerPreferencesEditor>

@OptIn(ExperimentalReadiumApi::class)
typealias ExoPlayerNavigator = AudioBookNavigator<ExoPlayerSettings, ExoPlayerPreferences>