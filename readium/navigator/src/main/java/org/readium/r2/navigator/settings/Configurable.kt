/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface Configurable {

    /**
     * Marker interface for the [Setting] properties holder.
     */
    interface Settings

    val settings: StateFlow<Settings>

    /**
     * Submits a new set of Presentation preferences used by the Navigator to recompute its
     * Presentation Settings.
     *
     * Note that the Navigator might not update its presentation right away, or might even ignore
     * some of the provided settings. They are only used as guidelines to compute the Presentation
     * Properties.
     */
    suspend fun applyPreferences(preferences: Preferences)
}
