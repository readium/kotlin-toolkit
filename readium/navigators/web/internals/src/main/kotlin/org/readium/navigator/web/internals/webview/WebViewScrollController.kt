/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webview

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.readium.navigator.web.internals.gestures.DefaultScrollable2DState
import org.readium.navigator.web.internals.gestures.Scrollable2DState
import org.readium.r2.shared.ExperimentalReadiumApi

public class WebViewScrollController(
    private val webView: RelaxedWebView,
) : Scrollable2DState by DefaultScrollable2DState({ webView.scrollBy(it) }) {
    public val scrollX: Int
        get() = webView.scrollX

    public val scrollY: Int
        get() = webView.scrollY

    public val maxScrollX: Int
        get() = webView.maxScrollX

    public val maxScrollY: Int
        get() = webView.maxScrollY

    public val canMoveLeft: Boolean
        get() = webView.scrollX > webView.width / 2

    public val canMoveRight: Boolean
        get() = webView.maxScrollX - webView.scrollX > webView.width / 2

    public val canMoveTop: Boolean
        get() = webView.scrollY > webView.height / 2

    public val canMoveBottom: Boolean
        get() = webView.maxScrollY - webView.scrollY > webView.height / 2

    public fun moveLeft() {
        webView.scrollBy(-webView.width, 0)
    }

    public fun moveRight() {
        webView.scrollBy(webView.width, 0)
    }

    public fun moveTop() {
        webView.scrollBy(0, -webView.height)
    }

    public fun moveBottom() {
        webView.scrollBy(0, webView.height)
    }

    public fun scrollBy(delta: Offset): Offset {
        return webView.scrollBy(delta)
    }

    public fun scrollToMin(orientation: Orientation): Int {
        return when (orientation) {
            Orientation.Vertical -> {
                val delta = -scrollY
                webView.scrollBy(0, delta)
                delta
            }

            Orientation.Horizontal -> {
                val delta = -scrollX
                webView.scrollBy(delta, 0)
                delta
            }
        }
    }

    public fun scrollToMax(orientation: Orientation): Int {
        return when (orientation) {
            Orientation.Vertical -> {
                val delta = webView.maxScrollY - scrollY
                webView.scrollBy(0, delta)
                delta
            }
            Orientation.Horizontal -> {
                val delta = webView.maxScrollX - scrollX
                webView.scrollBy(delta, 0)
                delta
            }
        }
    }

    public fun startProgression(orientation: Orientation, direction: LayoutDirection): Double? =
        webView.startProgression(orientation, direction).takeIf { it.isFinite() }

    public fun endProgression(orientation: Orientation, direction: LayoutDirection): Double? =
        webView.endProgression(orientation, direction).takeIf { it.isFinite() }

    public fun moveToProgression(
        progression: Double,
        snap: Boolean,
        orientation: Orientation,
        direction: LayoutDirection,
    ) {
        check(webView.height != 0)
        check(webView.width != 0)

        webView.scrollToProgression(
            progression = progression,
            orientation = orientation,
            direction = direction
        )

        if (snap) {
            snap(orientation)
        }
    }

    public fun snap(orientation: Orientation) {
        when (orientation) {
            Orientation.Vertical -> {
                val offset = webView.scrollY % webView.height
                webView.scrollBy(0, -offset)
            }
            Orientation.Horizontal -> {
                val offset = webView.scrollX % webView.width
                webView.scrollBy(-offset, 0)
            }
        }
    }

    public fun moveToOffset(
        offset: Int,
        snap: Boolean,
        orientation: Orientation,
    ) {
        webView.scrollToOffset(
            offset = offset,
            orientation = orientation
        )
        if (snap) {
            snap(orientation)
        }
    }

    public fun moveForward(orientation: Orientation, direction: LayoutDirection): Unit =
        when (orientation) {
            Orientation.Vertical -> moveBottom()
            Orientation.Horizontal -> when (direction) {
                LayoutDirection.Ltr -> moveRight()
                LayoutDirection.Rtl -> moveLeft()
            }
        }

    public fun moveBackward(orientation: Orientation, direction: LayoutDirection): Unit =
        when (orientation) {
            Orientation.Vertical -> moveTop()
            Orientation.Horizontal -> when (direction) {
                LayoutDirection.Ltr -> moveLeft()
                LayoutDirection.Rtl -> moveRight()
            }
        }

    public fun canMoveForward(orientation: Orientation, direction: LayoutDirection): Boolean =
        when (orientation) {
            Orientation.Vertical -> canMoveBottom
            Orientation.Horizontal -> when (direction) {
                LayoutDirection.Ltr -> canMoveRight
                LayoutDirection.Rtl -> canMoveLeft
            }
        }

    public fun canMoveBackward(orientation: Orientation, direction: LayoutDirection): Boolean =
        when (orientation) {
            Orientation.Vertical -> canMoveTop
            Orientation.Horizontal -> when (direction) {
                LayoutDirection.Ltr -> canMoveLeft
                LayoutDirection.Rtl -> canMoveRight
            }
        }
}

private fun RelaxedWebView.scrollToProgression(
    progression: Double,
    orientation: Orientation,
    direction: LayoutDirection,
) {
    val docHeight = maxScrollY + height
    val docWidth = maxScrollX + width

    when (orientation) {
        Orientation.Vertical -> {
            scrollTo(scrollX, ceil((progression * docHeight)).roundToInt())
        }
        Orientation.Horizontal -> when (direction) {
            LayoutDirection.Ltr -> {
                scrollTo(ceil(progression * docWidth).roundToInt(), scrollY)
            }
            LayoutDirection.Rtl -> {
                scrollTo((ceil((1 - progression) * docWidth)).roundToInt(), scrollY)
            }
        }
    }
}

private fun RelaxedWebView.scrollToOffset(
    offset: Int,
    orientation: Orientation,
) {
    when (orientation) {
        Orientation.Vertical -> {
            scrollTo(scrollX, offset)
        }
        Orientation.Horizontal -> {
            scrollTo(offset, scrollY)
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun RelaxedWebView.startProgression(
    orientation: Orientation,
    direction: LayoutDirection,
) = when (orientation) {
    Orientation.Vertical -> scrollY / (maxScrollY + height).toDouble()
    Orientation.Horizontal -> when (direction) {
        LayoutDirection.Ltr -> scrollX / (maxScrollX + width).toDouble()
        LayoutDirection.Rtl -> 1 - scrollX / (maxScrollX + width).toDouble()
    }
}

private fun RelaxedWebView.endProgression(
    orientation: Orientation,
    direction: LayoutDirection,
): Double {
    return when (orientation) {
        Orientation.Vertical -> (scrollY + height) / (maxScrollY + height).toDouble()
        Orientation.Horizontal -> when (direction) {
            LayoutDirection.Ltr -> (scrollX + width) / (maxScrollX + width).toDouble()
            LayoutDirection.Rtl -> 1 - (scrollX + width) / (maxScrollX + width).toDouble()
        }
    }
}

private fun RelaxedWebView.scrollBy(delta: Offset): Offset {
    val coercedX =
        if (delta.x < 0) {
            delta.x.fastCoerceAtLeast(-scrollX.toFloat())
        } else {
            delta.x.fastCoerceAtMost((maxScrollX - scrollX).toFloat())
        }

    val coercedY =
        if (delta.y < 0) {
            delta.y.fastCoerceAtLeast((-scrollY.toFloat()))
        } else {
            delta.y.fastCoerceAtMost((maxScrollY - scrollY).toFloat())
        }

    val roundedX = coercedX.fastRoundToInt()

    val roundedY = coercedY.fastRoundToInt()

    scrollBy(roundedX, roundedY)
    return Offset(coercedX, coercedY)
}
