/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.preferences

import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface PreferencesViewModel<P : Preferences<P>, S : Settings> {

    /**
     * Unset all preferences.
     */
    fun clear()
}
