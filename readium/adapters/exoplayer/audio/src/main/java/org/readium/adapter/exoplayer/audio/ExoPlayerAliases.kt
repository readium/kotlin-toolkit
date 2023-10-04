/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import org.readium.navigator.media.audio.AudioNavigator
import org.readium.navigator.media.audio.AudioNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
public typealias ExoPlayerNavigatorFactory = AudioNavigatorFactory<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerPreferencesEditor>

@OptIn(ExperimentalReadiumApi::class)
public typealias ExoPlayerNavigator = AudioNavigator<ExoPlayerSettings, ExoPlayerPreferences>
