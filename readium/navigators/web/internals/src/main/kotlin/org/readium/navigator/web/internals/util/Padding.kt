/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

public data class AbsolutePaddingValues(
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp,
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

@Composable
public fun WindowInsets.asAbsolutePaddingValues(): AbsolutePaddingValues {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val top = with(density) { getTop(density).toDp() }
    val right = with(density) { getRight(density, layoutDirection).toDp() }
    val bottom = with(density) { getBottom(density).toDp() }
    val left = with(density) { getLeft(density, layoutDirection).toDp() }
    return AbsolutePaddingValues(top = top, right = right, bottom = bottom, left = left)
}

public fun WindowInsets.symmetric(): WindowInsets =
    SymmetricWindowsInsets(this)

private class SymmetricWindowsInsets(
    private val baseWindowsInsets: WindowInsets,
) : WindowInsets {

    override fun getLeft(
        density: Density,
        layoutDirection: LayoutDirection,
    ): Int {
        val left = baseWindowsInsets.getLeft(density, layoutDirection)
        val right = baseWindowsInsets.getRight(density, layoutDirection)
        return max(left, right)
    }

    override fun getTop(density: Density): Int {
        val top = baseWindowsInsets.getTop(density)
        val bottom = baseWindowsInsets.getBottom(density)
        return max(top, bottom)
    }

    override fun getRight(
        density: Density,
        layoutDirection: LayoutDirection,
    ): Int {
        return getLeft(density, layoutDirection)
    }

    override fun getBottom(density: Density): Int {
        return getTop(density)
    }
}
