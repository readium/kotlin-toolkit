/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import androidx.compose.ui.unit.DpSize

public data class DisplayArea(
    val viewportSize: DpSize,
    val safeDrawingPadding: AbsolutePaddingValues,
)
