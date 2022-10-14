/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface PreferencesDemux<T: Configurable.Preferences> {

    data class Preferences<T: Configurable.Preferences>(
        val shared: T,
        val publication: T
    )

    fun demux(preferences: T): Preferences<T>
}