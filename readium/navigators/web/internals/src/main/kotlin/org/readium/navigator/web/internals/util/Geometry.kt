/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect

public fun DpRect.shift(offset: DpOffset) = DpRect(
    left = left + offset.x,
    right = right + offset.x,
    top = top + offset.y,
    bottom = bottom + offset.y
)
