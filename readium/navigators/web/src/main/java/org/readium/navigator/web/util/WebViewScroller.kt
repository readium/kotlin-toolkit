/*
package org.readium.navigator.web.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.webkit.WebView
import android.widget.OverScroller

internal class WebViewScroller(
    private val context: Context,
    private val webview: WebView
) : GestureDetector.Listener {

    private val scroller = OverScroller(context)

    override fun onDown() {
        // Initiates the decay phase of any active edge effects.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            releaseEdgeEffects()
        }

        // Aborts any active scroll animations and invalidates.
        scroller.forceFinished(true)
        webview.postInvalidateOnAnimation()

        return false
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        fling((-velocityX).toInt(), (-velocityY).toInt())
    }

    private fun fling(velocityX: Int, velocityY: Int) {
        // Initiates the decay phase of any active edge effects.
        // On Android 12 and later, the edge effect (stretch) must
        // continue.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            releaseEdgeEffects()
        }
        // Flings use math in pixels, as opposed to math based on the viewport.
        val surfaceSize: Point = computeScrollSurfaceSize()
        val (startX: Int, startY: Int) = scrollerStartViewport.run {
            set(currentViewport)
            (surfaceSize.x * (left - AXIS_X_MIN) / (AXIS_X_MAX - AXIS_X_MIN)).toInt() to
                (surfaceSize.y * (AXIS_Y_MAX - bottom) / (AXIS_Y_MAX - AXIS_Y_MIN)).toInt()
        }
        // Before flinging, stops the current animation.
        scroller.forceFinished(true)
        // Begins the animation.
        scroller.fling(
            // Current scroll position.
            startX,
            startY,
            velocityX,
            velocityY,
            */
/*
             * Minimum and maximum scroll positions. The minimum scroll
             * position is generally 0 and the maximum scroll position
             * is generally the content size less the screen size. So if the
             * content width is 1000 pixels and the screen width is 200
             * pixels, the maximum scroll offset is 800 pixels.
             *//*

            0, surfaceSize.x - contentRect.width(),
            0, surfaceSize.y - contentRect.height(),
            // The edges of the content. This comes into play when using
            // the EdgeEffect class to draw "glow" overlays.
            contentRect.width() / 2,
            contentRect.height() / 2
        )
        // Invalidates to trigger computeScroll().
        webview.postInvalidateOnAnimation()

    override fun onScroll(distanceX: Float, distanceY: Float) {
        TODO("Not yet implemented")
    }

}
*/
