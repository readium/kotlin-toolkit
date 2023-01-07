/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.navigator.media3.api.SynchronizedPlayback
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
internal data class TtsPlayback(
    override val state: MediaNavigatorInternal.State,
    override val locator: TtsLocator,
    override val token: TtsLocator?
) : SynchronizedPlayback<TtsLocator>
