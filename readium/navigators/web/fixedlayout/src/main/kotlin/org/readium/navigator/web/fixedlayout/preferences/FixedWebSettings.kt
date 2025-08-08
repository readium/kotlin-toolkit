/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.preferences

import org.readium.navigator.common.Settings
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public data class FixedWebSettings(
    val fit: Fit,
    val readingProgression: ReadingProgression,
    val spreads: Boolean,
) : Settings
