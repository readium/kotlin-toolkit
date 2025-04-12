package org.readium.navigator.web.webview

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.ceil
import kotlin.math.floor
import timber.log.Timber

internal class WebViewScrollController(
    private val webView: RelaxedWebView,
) {
    val scrollX: Int
        get() = webView.scrollX

    val scrollY: Int
        get() = webView.scrollY

    val maxScrollX: Int
        get() = webView.maxScrollX

    val maxScrollY: Int
        get() = webView.maxScrollY

    val canMoveLeft: Boolean
        get() = webView.scrollX > webView.width / 2 == true

    val canMoveRight: Boolean
        get() = webView.maxScrollX - webView.scrollX > webView.width / 2 == true

    val canMoveTop: Boolean
        get() = webView.scrollY > webView.width / 2 == true

    val canMoveBottom: Boolean
        get() = webView.maxScrollY - webView.scrollY > webView.width / 2 == true

    fun moveLeft() {
        webView.scrollBy(-webView.width, 0)
    }

    fun moveRight() {
        webView.scrollBy(webView.width, 0)
    }

    fun moveTop() {
        webView.scrollBy(0, -webView.width)
    }

    fun moveBottom() {
        webView.scrollBy(0, webView.width)
    }

    fun scrollBy(delta: Offset): Offset {
        val coercedX =
            if (delta.x < 0) {
                delta.x.fastCoerceAtLeast(-webView.scrollX.toFloat())
            } else {
                delta.x.fastCoerceAtMost((webView.maxScrollX - webView.scrollX).toFloat())
            }

        val coercedY =
            if (delta.y < 0) {
                delta.y.fastCoerceAtLeast((-webView.scrollY.toFloat()))
            } else {
                delta.y.fastCoerceAtMost((webView.maxScrollY - webView.scrollY).toFloat())
            }

        val roundedX = coercedX.fastRoundToInt()

        val roundedY = coercedY.fastRoundToInt()

        webView.scrollBy(roundedX, roundedY)
        return Offset(coercedX, coercedY)
    }

    fun scrollToEnd(scrollOrientation: Orientation): Int {
        return when (scrollOrientation) {
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

    fun progression(scrollOrientation: Orientation) =
        webView.progression(scrollOrientation)

    fun moveToProgression(
        progression: Double,
        scroll: Boolean,
        orientation: Orientation,
    ) {
        Timber.d("moveToProgression $progression")
        when (scroll) {
            true -> {
                webView.scrollToProgression(
                    progression = progression,
                    scrollOrientation = orientation
                )
            }
            false -> {
                webView.scrollToProgression(progression, Orientation.Horizontal)
                val offset = webView.scrollX % webView.width
                Timber.d("moveToProgression scrollBy $offset ${webView.scrollX} ${webView.width}")
                webView.scrollBy(-offset, 0)
            }
        }
    }
}

private fun RelaxedWebView.scrollToProgression(progression: Double, scrollOrientation: Orientation) {
    if (scrollOrientation == Orientation.Horizontal) {
        scrollTo(floor(progression * maxScrollX).toInt(), 0)
    } else {
        scrollTo(0, ceil(progression * maxScrollY).toInt())
    }
}

private fun RelaxedWebView.progression(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> scrollY / maxScrollY.toDouble()
    Orientation.Horizontal -> scrollX / maxScrollX.toDouble()
}
