/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface SynchronizedPlayback<L : MediaNavigatorInternal.Locator> :
    MediaNavigatorInternal.Playback<L>, MediaNavigatorInternal.TextSynchronization {

    override val state: MediaNavigatorInternal.State

    override val locator: L

    override val token: L?
}
