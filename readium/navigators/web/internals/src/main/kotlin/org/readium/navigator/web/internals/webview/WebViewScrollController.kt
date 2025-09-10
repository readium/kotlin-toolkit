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
import kotlin.math.roundToInt
import org.readium.navigator.web.internals.gestures.DefaultScrollable2DState
import org.readium.navigator.web.internals.gestures.Scrollable2DState

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
        get() = webView.scrollX > webView.width / 2 == true

    public val canMoveRight: Boolean
        get() = webView.maxScrollX - webView.scrollX > webView.width / 2 == true

    public val canMoveTop: Boolean
        get() = webView.scrollY > webView.width / 2 == true

    public val canMoveBottom: Boolean
        get() = webView.maxScrollY - webView.scrollY > webView.width / 2 == true

    public fun moveLeft(pageExtent: Int, axisVertical: Boolean) {
        if (!axisVertical) {
            val x = (webView.scrollX - pageExtent).fastCoerceAtLeast(0)
            android.util.Log.d("WebViewScrollController", "moveLeft(H) - extent=$pageExtent, scrollX=${webView.scrollX}")
            webView.scrollTo(x, webView.scrollY)
        } else {
            // 左スワイプ＝前ページだが、縦軸では上方向へ移動
            val y = (webView.scrollY - pageExtent).fastCoerceAtLeast(0)
            android.util.Log.d("WebViewScrollController", "moveLeft(V) - extent=$pageExtent, scrollY=${webView.scrollY}")
            webView.scrollTo(webView.scrollX, y)
        }
    }

    public fun moveRight(pageExtent: Int, axisVertical: Boolean) {
        if (!axisVertical) {
            val x = webView.scrollX + pageExtent
            android.util.Log.d("WebViewScrollController", "moveRight(H) - extent=$pageExtent, scrollX=${webView.scrollX}")
            webView.scrollTo(x, webView.scrollY)
        } else {
            // 右スワイプ＝次ページだが、縦軸では下方向へ移動
            val y = webView.scrollY + pageExtent
            android.util.Log.d("WebViewScrollController", "moveRight(V) - extent=$pageExtent, scrollY=${webView.scrollY}")
            webView.scrollTo(webView.scrollX, y)
        }
    }

    public fun moveLeft() {
        webView.scrollBy(-webView.width, 0)
    }

    public fun moveRight() {
        webView.scrollBy(webView.width, 0)
    }

    public fun moveTop() {
        webView.scrollBy(0, -webView.width)
    }

    public fun moveBottom() {
        webView.scrollBy(0, webView.width)
    }

    public fun scrollByAxisAware(delta: Offset, axisVertical: Boolean): Offset {
        val (dx, dy) = if (!axisVertical) {
            delta.x.roundToInt() to delta.y.roundToInt()
        } else {
            // 横スワイプ量（-x）を縦スクロール量（+y）へマッピング
            0 to (-delta.x).roundToInt()
        }
        android.util.Log.d("WebViewScrollController", "scrollBy(axisAware=$axisVertical) - in=$delta, applied=($dx,$dy), scrollX=${webView.scrollX}, scrollY=${webView.scrollY}")
        return webView.scrollBy(Offset(dx.toFloat(), dy.toFloat()))
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

    public fun progression(orientation: Orientation, direction: LayoutDirection): Double? =
        webView.progression(orientation, direction).takeIf { it.isFinite() }

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
    when (orientation) {
        Orientation.Vertical -> {
            scrollTo(scrollX, progression.roundToInt() * maxScrollY.toInt())
        }
        Orientation.Horizontal -> when (direction) {
            LayoutDirection.Ltr -> {
                scrollTo(progression.roundToInt() * maxScrollX, scrollY)
            }
            LayoutDirection.Rtl -> {
                scrollTo((1 - progression).roundToInt() * maxScrollX, scrollY)
            }
        }
    }
}

private fun RelaxedWebView.progression(
    orientation: Orientation,
    direction: LayoutDirection,
) = when (orientation) {
    Orientation.Vertical -> scrollY / maxScrollY.toDouble()
    Orientation.Horizontal -> when (direction) {
        LayoutDirection.Ltr -> scrollX / maxScrollX.toDouble()
        LayoutDirection.Rtl -> 1 - scrollX / maxScrollX.toDouble()
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
