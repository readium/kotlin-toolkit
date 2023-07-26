package org.readium.r2.navigator.media3.api

import androidx.media3.common.Player
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * An object able to pass as a Jetpack media3 [Player].
 */
@ExperimentalReadiumApi
public interface Media3Adapter {
    public fun asMedia3Player(): Player
}
