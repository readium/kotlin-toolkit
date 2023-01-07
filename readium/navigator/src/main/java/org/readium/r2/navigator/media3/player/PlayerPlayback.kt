/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.player

import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
data class PlayerPlayback(
    override val state: MediaNavigatorInternal.State,
    override val locator: PlayerLocator,
    override val buffer: MediaNavigatorInternal.Buffer
) : MediaNavigatorInternal.Playback<PlayerLocator>, MediaNavigatorInternal.BufferProvider
