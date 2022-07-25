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

    /**
     * Current [Settings].
     */
    val settings: StateFlow<Settings>

    /**
     * Submits a new set of preferences used by the [Configurable] to recompute its [Settings].
     *
     * Note that the [Configurable] might not update its settings right away, or might even ignore
     * some of the provided preferences. They are only used as guidelines to compute the new
     * [Settings].
     */
    fun applyPreferences(preferences: Preferences)
}
