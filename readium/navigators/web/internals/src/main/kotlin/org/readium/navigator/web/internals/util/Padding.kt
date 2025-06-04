/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data class AbsolutePaddingValues(
    val top: Dp,
    val right: Dp,
    val bottom: Dp,
    val left: Dp,
) {
    public constructor(vertical: Dp = 0.dp, horizontal: Dp = 0.dp) :
        this(top = vertical, right = horizontal, bottom = vertical, left = horizontal)

    public operator fun plus(other: AbsolutePaddingValues): AbsolutePaddingValues = copy(
        top = top + other.top,
        right = right + other.right,
        bottom = bottom + other.bottom,
        left = left + other.left
    )
}

public fun Modifier.absolutePadding(paddingValues: AbsolutePaddingValues): Modifier =
    this.absolutePadding(
        top = paddingValues.top,
        right = paddingValues.right,
        bottom = paddingValues.bottom,
        left = paddingValues.left
    )
