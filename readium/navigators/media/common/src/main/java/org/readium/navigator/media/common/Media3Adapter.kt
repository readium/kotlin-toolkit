/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.common

import androidx.media3.common.Player
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * An object able to pass as a Jetpack media3 [Player].
 */
@ExperimentalReadiumApi
public interface Media3Adapter {
    public fun asMedia3Player(): Player
}
