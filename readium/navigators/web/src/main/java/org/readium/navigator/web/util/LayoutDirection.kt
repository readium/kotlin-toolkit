/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.util

import androidx.compose.ui.unit.LayoutDirection
import org.readium.r2.navigator.preferences.ReadingProgression

internal fun LayoutDirection.toReadingProgression(): ReadingProgression =
    when (this) {
        LayoutDirection.Ltr -> ReadingProgression.LTR
        LayoutDirection.Rtl -> ReadingProgression.RTL
    }

internal fun ReadingProgression.toLayoutDirection(): LayoutDirection =
    when (this) {
        ReadingProgression.LTR -> LayoutDirection.Ltr
        ReadingProgression.RTL -> LayoutDirection.Rtl
    }
