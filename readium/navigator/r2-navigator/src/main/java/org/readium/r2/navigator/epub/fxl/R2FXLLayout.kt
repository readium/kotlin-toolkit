/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.epub.fxl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.shopgun.android.utils.NumberUtils
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class R2FXLLayout : FrameLayout {

    private var scaleDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var gestureListener: GestureListener? = null
    private var simpleOnGlobalLayoutChangedListener: SimpleOnGlobalLayoutChangedListener? = null

    private val scaleMatrix = Matrix()
    private val scaleMatrixInverse = Matrix()
    private val translateMatrix = Matrix()
    private val translateMatrixInverse = Matrix()

    private val matrixValues = FloatArray(9)

    private var focusY: Float = 0.toFloat()
    private var focusX: Float = 0.toFloat()

    private var array = FloatArray(6)

    private var isAllowOverScale = true

    internal var drawRect = RectF()
    internal var viewPortRect = RectF()

    private var flingRunnable: FlingRunnable? = null
    private var animatedZoomRunnable: AnimatedZoomRunnable? = null
    private var animationInterpolator: Interpolator = DecelerateInterpolator()
    var zoomDuration = DEF_ZOOM_DURATION
        set(zoomDuration) {
            field = if (zoomDuration < 0) DEF_ZOOM_DURATION else zoomDuration
        }

    private var isScrollingAllowed = false

    // allow parent views to intercept any touch events that we do not consume
    var isAllowParentInterceptOnEdge = true
    // allow parent views to intercept any touch events that we do not consume even if we are in a scaled state
    var isAllowParentInterceptOnScaled = false
    // minimum scale of the content
    var minScale = 1.0f
        set(minScale) {
            field = minScale
            if (this.minScale > maxScale) {
                maxScale = this.minScale
            }
        }
    // maximum scale of the content
    var maxScale = 3.0f
        set(maxScale) {
            field = maxScale
            if (this.maxScale < minScale) {
                minScale = maxScale
            }
        }

    private var isAllowZoom = true

    // Listeners
    private val zoomDispatcher = ZoomDispatcher()
    private val panDispatcher = PanDispatcher()
    private var onZoomListeners: MutableList<OnZoomListener>? = null
    private var onPanListeners: MutableList<OnPanListener>? = null
    private var onTouchListeners: MutableList<OnTouchListener>? = null
    private var onTapListeners: MutableList<OnTapListener>? = null
    private var mOnDoubleTapListeners: MutableList<OnDoubleTapListener>? = null
    private var onLongTapListeners: MutableList<OnLongTapListener>? = null

    var scale: Float
        get() = getMatrixValue(scaleMatrix, Matrix.MSCALE_X)
        set(scale) = setScale(scale, true)

    val isScaled: Boolean
        get() = !NumberUtils.isEqual(scale, 1.0f, 0.05f)

    private val translateDeltaBounds: RectF
        get() {
            val r = RectF()
            val maxDeltaX = drawRect.width() - viewPortRect.width()
            if (maxDeltaX < 0) {
                val leftEdge = ((viewPortRect.width() - drawRect.width()) / 2).roundToLong().toFloat()
                if (leftEdge > drawRect.left) {
                    r.left = 0f
                    r.right = leftEdge - drawRect.left
                } else {
                    r.left = leftEdge - drawRect.left
                    r.right = 0f
                }
            } else {
                r.left = drawRect.left - viewPortRect.left
                r.right = r.left + maxDeltaX
            }

            val maxDeltaY = drawRect.height() - viewPortRect.height()
            if (maxDeltaY < 0) {
                val topEdge = ((viewPortRect.height() - drawRect.height()) / 2f).roundToLong().toFloat()
                if (topEdge > drawRect.top) {
                    r.top = drawRect.top - topEdge
                    r.bottom = 0f
                } else {
                    r.top = topEdge - drawRect.top
                    r.bottom = 0f
                }
            } else {
                r.top = drawRect.top - viewPortRect.top
                r.bottom = r.top + maxDeltaY
            }

            return r
        }

    /**
     * Gets the closest valid translation point, to the current [x][.getPosX] and [y][.getPosY] coordinates.
     * @return the closest point
     */
    private val closestValidTranslationPoint: PointF
        get() {
            val p = PointF(posX, posY)
            when {
                drawRect.width() < viewPortRect.width() -> p.x += drawRect.centerX() - viewPortRect.centerX()
                drawRect.right < viewPortRect.right -> p.x += drawRect.right - viewPortRect.right
                drawRect.left > viewPortRect.left -> p.x += drawRect.left - viewPortRect.left
            }
            when {
                drawRect.height() < viewPortRect.height() -> p.y += drawRect.centerY() - viewPortRect.centerY()
                drawRect.bottom < viewPortRect.bottom -> p.y += drawRect.bottom - viewPortRect.bottom
                drawRect.top > viewPortRect.top -> p.y += drawRect.top - viewPortRect.top
            }
            return p
        }

    /**
     * Get the current x-translation
     */
    val posX: Float
        get() = -getMatrixValue(translateMatrix, Matrix.MTRANS_X)

    /**
     * Get the current y-translation
     */
    val posY: Float
        get() = -getMatrixValue(translateMatrix, Matrix.MTRANS_Y)

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        gestureListener = GestureListener()
        scaleDetector = ScaleGestureDetector(context, gestureListener)

        scaleDetector!!.isQuickScaleEnabled = false
        gestureDetector = GestureDetector(context, gestureListener)
        simpleOnGlobalLayoutChangedListener = SimpleOnGlobalLayoutChangedListener()
        viewTreeObserver.addOnGlobalLayoutListener(simpleOnGlobalLayoutChangedListener)
    }

    override fun onDetachedFromWindow() {
        removeGlobal(this, simpleOnGlobalLayoutChangedListener)
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(-posX, -posY)
        val scale = scale
        canvas.scale(scale, scale, focusX, focusY)
        try {
            super.dispatchDraw(canvas)
        } catch (e: Exception) {
            // ignore
        }
        canvas.restore()
    }

    private fun scaledPointsToScreenPoints(rect: RectF) {
        R2FXLUtils.setArray(array, rect)
        array = scaledPointsToScreenPoints(array)
        R2FXLUtils.setRect(rect, array)
    }

    private fun scaledPointsToScreenPoints(a: FloatArray): FloatArray {
        scaleMatrix.mapPoints(a)
        translateMatrix.mapPoints(a)
        return a
    }

    private fun screenPointsToScaledPoints(a: FloatArray): FloatArray {
        translateMatrixInverse.mapPoints(a)
        scaleMatrixInverse.mapPoints(a)
        return a
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        array[0] = ev.x
        array[1] = ev.y
        screenPointsToScaledPoints(array)
        ev.setLocation(array[0], array[1])
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (isScrollingAllowed) {
            super.onInterceptTouchEvent(ev)
        } else isAllowZoom
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        array[0] = ev.x
        array[1] = ev.y
        scaledPointsToScreenPoints(array)
        ev.setLocation(array[0], array[1])

        if (!isAllowZoom) {
            return false
        }

        val action = ev.action and MotionEvent.ACTION_MASK
        dispatchOnTouch(action, ev)

        var consumed = scaleDetector!!.onTouchEvent(ev)
        consumed = gestureDetector!!.onTouchEvent(ev) || consumed
        if (action == MotionEvent.ACTION_UP) {
            // manually call up
            consumed = gestureListener!!.onUp(ev) || consumed
        }
        return consumed
    }

    internal inner class GestureListener : ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

        private var mScaleOnActionDown: Float = 0.toFloat()
        private var mScrolling = false

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return false
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            dispatchOnTab(e)
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return false
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP ->
                    dispatchOnDoubleTap(e)
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            if (!scaleDetector!!.isInProgress) {
                dispatchOnLongTap(e)
            }
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            var consumed = false
            if (e2.pointerCount == 1 && !scaleDetector!!.isInProgress) {
                // only drag if we have one pointer and aren't already scaling
                if (!mScrolling) {
                    panDispatcher.onPanBegin()
                    mScrolling = true
                }
                consumed = internalMoveBy(distanceX, distanceY, true)
                if (consumed) {
                    panDispatcher.onPan()
                }
                if (isAllowParentInterceptOnEdge && !consumed && (!isScaled || isAllowParentInterceptOnScaled)) {
                    requestDisallowInterceptTouchEvent(false)
                }
            }
            return consumed
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val scale = scale
            val newScale = NumberUtils.clamp(minScale, scale, maxScale)
            if (NumberUtils.isEqual(newScale, scale)) {
                // only fling if no scale is needed - scale will happen on ACTION_UP
                flingRunnable = FlingRunnable(context)
                flingRunnable!!.fling(velocityX.toInt(), velocityY.toInt())
                ViewCompat.postOnAnimation(this@R2FXLLayout, flingRunnable)
                return true
            }
            return false
        }

        override fun onShowPress(e: MotionEvent) {

        }

        override fun onDown(e: MotionEvent): Boolean {
            mScaleOnActionDown = scale
            requestDisallowInterceptTouchEvent(true)
            cancelFling()
            cancelZoom()
            return false
        }

        fun onUp(e: MotionEvent): Boolean {
            var consumed = false
            if (mScrolling) {
                panDispatcher.onPanEnd()
                mScrolling = false
                consumed = true
            }
            if (animatedZoomRunnable == null || animatedZoomRunnable!!.mFinished) {
                animatedZoomRunnable = AnimatedZoomRunnable()
                consumed = animatedZoomRunnable!!.runValidation() || consumed
            }
            return consumed
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            zoomDispatcher.onZoomBegin(scale)
            fixFocusPoint(detector.focusX, detector.focusY)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = scale * detector.scaleFactor
            val scaleFactor = detector.scaleFactor
            if (java.lang.Float.isNaN(scaleFactor) || java.lang.Float.isInfinite(scaleFactor))
                return false

            internalScale(scale, focusX, focusY)
            zoomDispatcher.onZoom(scale)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            animatedZoomRunnable = AnimatedZoomRunnable()
            animatedZoomRunnable!!.runValidation()
            zoomDispatcher.onZoomEnd(scale)
        }
    }

    /**
     * When setting a new focus point, the translations on scale-matrix will change,
     * to counter that we'll first read old translation values, then apply the new focus-point
     * (with the old scale), then read the new translation values. Lastly we'll ensureTranslation
     * out ensureTranslation-matrix by the delta given by the scale-matrix translations.
     * @param focusX focus-focusX in screen coordinate
     * @param focusY focus-focusY in screen coordinate
     */
    private fun fixFocusPoint(focusX: Float, focusY: Float) {
        array[0] = focusX
        array[1] = focusY
        screenPointsToScaledPoints(array)
        // The first scale event translates the content, so we'll counter that ensureTranslation
        val x1 = getMatrixValue(scaleMatrix, Matrix.MTRANS_X)
        val y1 = getMatrixValue(scaleMatrix, Matrix.MTRANS_Y)
        internalScale(scale, array[0], array[1])
        val dX = getMatrixValue(scaleMatrix, Matrix.MTRANS_X) - x1
        val dY = getMatrixValue(scaleMatrix, Matrix.MTRANS_Y) - y1
        internalMove(dX + posX, dY + posY, false)
    }

    private fun cancelFling() {
        if (flingRunnable != null) {
            flingRunnable!!.cancelFling()
            flingRunnable = null
        }
    }

    private fun cancelZoom() {
        if (animatedZoomRunnable != null) {
            animatedZoomRunnable!!.cancel()
            animatedZoomRunnable = null
        }
    }

    fun setScale(scale: Float, animate: Boolean) {
        setScale(scale, (right / 2).toFloat(), (bottom / 2).toFloat(), animate)
    }

    fun setScale(scale_in: Float, focusX: Float, focusY: Float, animate: Boolean) {
        var newScale = scale_in
        if (!isAllowZoom) {
            return
        }
        fixFocusPoint(focusX, focusY)
        if (!isAllowOverScale) {
            newScale = NumberUtils.clamp(minScale, newScale, maxScale)
        }
        if (animate) {
            animatedZoomRunnable = AnimatedZoomRunnable()
            animatedZoomRunnable!!.scale(scale, newScale, this.focusX, this.focusY, true)
            ViewCompat.postOnAnimation(this@R2FXLLayout, animatedZoomRunnable)
        } else {
            zoomDispatcher.onZoomBegin(newScale)
            internalScale(newScale, this.focusX, this.focusY)
            zoomDispatcher.onZoom(newScale)
            zoomDispatcher.onZoomEnd(newScale)
        }
    }

    private fun internalMoveBy(dx: Float, dy: Float, clamp: Boolean): Boolean {
        var tdx = dx
        var tdy = dy
        if (clamp) {
            val bounds = translateDeltaBounds
            tdx = NumberUtils.clamp(bounds.left, dx, bounds.right)
            tdy = NumberUtils.clamp(bounds.top, dy, bounds.bottom)
        }
        val destPosX = tdx + posX
        val destPosY = tdy + posY
        if (!NumberUtils.isEqual(destPosX, posX) || !NumberUtils.isEqual(destPosY, posY)) {
            translateMatrix.setTranslate(-destPosX, -destPosY)
            matrixUpdated()
            invalidate()
            return true
        }
        return false
    }

    private fun internalMove(destPosX: Float, destPosY: Float, clamp: Boolean): Boolean {
        return internalMoveBy(destPosX - posX, destPosY - posY, clamp)
    }

    private fun internalScale(scale: Float, focusX: Float, focusY: Float) {
        this.focusX = focusX
        this.focusY = focusY
        scaleMatrix.setScale(scale, scale, this.focusX, this.focusY)
        matrixUpdated()
        requestLayout()
        invalidate()
    }

    /**
     * Update all variables that rely on the Matrix'es.
     */
    private fun matrixUpdated() {
        // First inverse matrixes
        scaleMatrix.invert(scaleMatrixInverse)
        translateMatrix.invert(translateMatrixInverse)
        // Update DrawRect - maybe this should be viewPort.left instead of 0?
        R2FXLUtils.setRect(viewPortRect, 0f, 0f, width.toFloat(), height.toFloat())

        val child = getChildAt(0)
        if (child != null) {
            R2FXLUtils.setRect(drawRect, child.left.toFloat(), child.top.toFloat(), child.right.toFloat(), child.bottom.toFloat())
            scaledPointsToScreenPoints(drawRect)
        } else {
            // If no child is added, then center the drawrect, and let it be empty
            val x = viewPortRect.centerX()
            val y = viewPortRect.centerY()
            drawRect.set(x, y, x, y)
        }
    }

    /**
     * Read a specific value from a given matrix
     * @param matrix The Matrix to read a value from
     * @param value The value-position to read
     * @return The value at a given position
     */
    private fun getMatrixValue(matrix: Matrix, value: Int): Float {
        matrix.getValues(matrixValues)
        return matrixValues[value]
    }

    private inner class AnimatedZoomRunnable internal constructor() : Runnable {

        internal var mCancelled = false
        internal var mFinished = false

        private val mStartTime: Long = System.currentTimeMillis()
        private var mZoomStart: Float = 0.toFloat()
        private var mZoomEnd: Float = 0.toFloat()
        private var mFocalX: Float = 0.toFloat()
        private var mFocalY: Float = 0.toFloat()
        private var mStartX: Float = 0.toFloat()
        private var mStartY: Float = 0.toFloat()
        private var mTargetX: Float = 0.toFloat()
        private var mTargetY: Float = 0.toFloat()

        internal fun doScale(): Boolean {
            return !NumberUtils.isEqual(mZoomStart, mZoomEnd)
        }

        internal fun doTranslate(): Boolean {
            return !NumberUtils.isEqual(mStartX, mTargetX) || !NumberUtils.isEqual(mStartY, mTargetY)
        }

        internal fun runValidation(): Boolean {
            val scale = scale
            val newScale = NumberUtils.clamp(minScale, scale, maxScale)
            scale(scale, newScale, focusX, focusY, true)
            if (animatedZoomRunnable!!.doScale() || animatedZoomRunnable!!.doTranslate()) {
                ViewCompat.postOnAnimation(this@R2FXLLayout, animatedZoomRunnable)
                return true
            }
            return false
        }

        internal fun scale(currentZoom: Float, targetZoom: Float, focalX: Float, focalY: Float, ensureTranslations: Boolean): AnimatedZoomRunnable {
            mFocalX = focalX
            mFocalY = focalY
            mZoomStart = currentZoom
            mZoomEnd = targetZoom
            if (doScale()) {
                zoomDispatcher.onZoomBegin(scale)
            }
            if (ensureTranslations) {
                mStartX = posX
                mStartY = posY
                val scale = doScale()
                if (scale) {
                    scaleMatrix.setScale(mZoomEnd, mZoomEnd, mFocalX, mFocalY)
                    matrixUpdated()
                }
                val p = closestValidTranslationPoint
                mTargetX = p.x
                mTargetY = p.y
                if (scale) {
                    scaleMatrix.setScale(mZoomStart, mZoomStart, this@R2FXLLayout.focusX, this@R2FXLLayout.focusY)
                    matrixUpdated()
                }
                if (doTranslate()) {
                    panDispatcher.onPanBegin()
                }
            }
            return this
        }

        internal fun cancel() {
            mCancelled = true
            finish()
        }

        private fun finish() {
            if (!mFinished) {
                if (doScale()) {
                    zoomDispatcher.onZoomEnd(scale)
                }
                if (doTranslate()) {
                    panDispatcher.onPanEnd()
                }
            }
            mFinished = true
        }

        override fun run() {

            if (mCancelled || !doScale() && !doTranslate()) {
                return
            }

            val t = interpolate()
            if (doScale()) {
                val newScale = mZoomStart + t * (mZoomEnd - mZoomStart)
                internalScale(newScale, mFocalX, mFocalY)
                zoomDispatcher.onZoom(newScale)
            }
            if (doTranslate()) {
                val x = mStartX + t * (mTargetX - mStartX)
                val y = mStartY + t * (mTargetY - mStartY)
                internalMove(x, y, false)
                panDispatcher.onPan()
            }

            if (t < 1f) {
                ViewCompat.postOnAnimation(this@R2FXLLayout, this)
            } else {
                finish()
            }
        }

        private fun interpolate(): Float {
            var t = 1f * (System.currentTimeMillis() - mStartTime) / zoomDuration
            t = min(1f, t)
            return animationInterpolator.getInterpolation(t)
        }

    }

    private inner class FlingRunnable internal constructor(context: Context) : Runnable {

        private val mScroller: R2FXLScroller = R2FXLScroller.getScroller(context)
        private var mCurrentX: Int = 0
        private var mCurrentY: Int = 0
        private var mFinished = false

        internal fun fling(velocityX: Int, velocityY: Int) {

            val startX = viewPortRect.left.roundToInt()
            val minX: Int
            val maxX: Int
            if (viewPortRect.width() < drawRect.width()) {
                minX = drawRect.left.roundToInt()
                maxX = (drawRect.width() - viewPortRect.width()).roundToInt()
            } else {
                maxX = startX
                minX = maxX
            }

            val startY = viewPortRect.top.roundToInt()
            val minY: Int
            val maxY: Int
            if (viewPortRect.height() < drawRect.height()) {
                minY = drawRect.top.roundToInt()
                maxY = (drawRect.bottom - viewPortRect.bottom).roundToInt()
            } else {
                maxY = startY
                minY = maxY
            }

            mCurrentX = startX
            mCurrentY = startY

            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0)
                panDispatcher.onPanBegin()
            } else {
                mFinished = true
            }

        }

        internal fun cancelFling() {
            mScroller.forceFinished(true)
            finish()
        }

        private fun finish() {
            if (!mFinished) {
                panDispatcher.onPanEnd()
            }
            mFinished = true
        }

        override fun run() {
            if (!mScroller.isFinished && mScroller.computeScrollOffset()) {

                val newX = mScroller.currX
                val newY = mScroller.currY

                if (internalMoveBy((mCurrentX - newX).toFloat(), (mCurrentY - newY).toFloat(), true)) {
                    panDispatcher.onPan()
                }

                mCurrentX = newX
                mCurrentY = newY

                ViewCompat.postOnAnimation(this@R2FXLLayout, this@FlingRunnable)
            } else {
                finish()
            }
        }
    }

    private fun dispatchOnTouch(action: Int, ev: MotionEvent) {
        if (onTouchListeners != null) {
            var i = 0
            val z = onTouchListeners!!.size
            while (i < z) {
                val listener = onTouchListeners!![i]
                listener.onTouch(this@R2FXLLayout, action, TapInfo(this@R2FXLLayout, ev))
                i++
            }
        }
    }

    fun addOnTapListener(l: OnTapListener) {
        if (onTapListeners == null) {
            onTapListeners = ArrayList()
        }
        onTapListeners!!.add(l)
    }

    private fun dispatchOnTab(ev: MotionEvent) {
        if (onTapListeners != null) {
            var i = 0
            val z = onTapListeners!!.size
            while (i < z) {
                val listener = onTapListeners!![i]
                listener.onTap(this@R2FXLLayout, TapInfo(this@R2FXLLayout, ev))
                i++
            }
        }
    }

    fun addOnDoubleTapListener(l: OnDoubleTapListener) {
        if (mOnDoubleTapListeners == null) {
            mOnDoubleTapListeners = ArrayList()
        }
        mOnDoubleTapListeners!!.add(l)
    }

    private fun dispatchOnDoubleTap(ev: MotionEvent) {
        if (mOnDoubleTapListeners != null) {
            var i = 0
            val z = mOnDoubleTapListeners!!.size
            while (i < z) {
                val listener = mOnDoubleTapListeners!![i]
                listener.onDoubleTap(this@R2FXLLayout, TapInfo(this@R2FXLLayout, ev))
                i++
            }
        }
    }

    private fun dispatchOnLongTap(ev: MotionEvent) {
        if (onLongTapListeners != null) {
            var i = 0
            val z = onLongTapListeners!!.size
            while (i < z) {
                val listener = onLongTapListeners!![i]
                listener.onLongTap(this@R2FXLLayout, TapInfo(this@R2FXLLayout, ev))
                i++
            }
        }
    }

    interface OnZoomListener {
        fun onZoomBegin(view: R2FXLLayout, scale: Float)
        fun onZoom(view: R2FXLLayout, scale: Float)
        fun onZoomEnd(view: R2FXLLayout, scale: Float)
    }

    interface OnPanListener {
        fun onPanBegin(view: R2FXLLayout)
        fun onPan(view: R2FXLLayout)
        fun onPanEnd(view: R2FXLLayout)
    }

    interface OnTouchListener {
        fun onTouch(view: R2FXLLayout, action: Int, info: TapInfo): Boolean
    }

    interface OnTapListener {
        fun onTap(view: R2FXLLayout, info: TapInfo): Boolean
    }

    interface OnDoubleTapListener {
        fun onDoubleTap(view: R2FXLLayout, info: TapInfo): Boolean
    }

    interface OnLongTapListener {
        fun onLongTap(view: R2FXLLayout, info: TapInfo)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        throw IllegalStateException("Cannot set OnClickListener, please use OnTapListener.")
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        throw IllegalStateException("Cannot set OnLongClickListener, please use OnLongTabListener.")
    }

    override fun setOnTouchListener(l: View.OnTouchListener) {
        throw IllegalStateException("Cannot set OnTouchListener.")
    }

    class TapInfo(r2FXLLayout: R2FXLLayout, e: MotionEvent) {

        var view: View = r2FXLLayout
            internal set
        var x: Float = 0.toFloat()
            internal set
        var y: Float = 0.toFloat()
            internal set
        private var relativeX: Float = 0.toFloat()
        private var relativeY: Float = 0.toFloat()
        private var percentX: Float = 0.toFloat()
        private var percentY: Float = 0.toFloat()
        private var isContentClicked: Boolean = false

        init {
            x = e.x
            y = e.y
            r2FXLLayout.array[0] = x
            r2FXLLayout.array[1] = y
            r2FXLLayout.screenPointsToScaledPoints(r2FXLLayout.array)
            val v = r2FXLLayout.getChildAt(0)
            relativeX = r2FXLLayout.array[0] - v.left
            relativeY = r2FXLLayout.array[1] - v.top
            percentX = relativeX / v.width.toFloat()
            percentY = relativeY / v.height.toFloat()
            isContentClicked = r2FXLLayout.drawRect.contains(x, y)
        }

        override fun toString(): String {
            return String.format(Locale.US, STRING_FORMAT,
                    x, y, relativeX, relativeY, percentX, percentY, isContentClicked)
        }

        companion object {

            private const val STRING_FORMAT = "TapInfo[ absX:%.0f, absY:%.0f, relX:%.0f, relY:%.0f, percentX:%.2f, percentY:%.2f, contentClicked:%s ]"
        }
    }

    private inner class ZoomDispatcher {

        internal var mCount = 0

        internal fun onZoomBegin(scale: Float) {
            if (mCount++ == 0) {
                if (onZoomListeners != null) {
                    var i = 0
                    val z = onZoomListeners!!.size
                    while (i < z) {
                        val listener = onZoomListeners!![i]
                        listener.onZoomBegin(this@R2FXLLayout, scale)
                        i++
                    }
                }
            }
        }

        internal fun onZoom(scale: Float) {
            if (onZoomListeners != null) {
                var i = 0
                val z = onZoomListeners!!.size
                while (i < z) {
                    val listener = onZoomListeners!![i]
                    listener.onZoom(this@R2FXLLayout, scale)
                    i++
                }
            }
        }

        internal fun onZoomEnd(scale: Float) {
            if (--mCount == 0) {
                if (onZoomListeners != null) {
                    var i = 0
                    val z = onZoomListeners!!.size
                    while (i < z) {
                        val listener = onZoomListeners!![i]
                        listener.onZoomEnd(this@R2FXLLayout, scale)
                        i++
                    }
                }
            }
        }
    }

    private inner class PanDispatcher {

        internal var mCount = 0

        internal fun onPanBegin() {
            if (mCount++ == 0) {
                if (onPanListeners != null) {
                    var i = 0
                    val z = onPanListeners!!.size
                    while (i < z) {
                        val listener = onPanListeners!![i]
                        listener.onPanBegin(this@R2FXLLayout)
                        i++
                    }
                }
            }
        }

        internal fun onPan() {
            if (onPanListeners != null) {
                var i = 0
                val z = onPanListeners!!.size
                while (i < z) {
                    val listener = onPanListeners!![i]
                    listener.onPan(this@R2FXLLayout)
                    i++
                }
            }
        }

        internal fun onPanEnd() {
            if (--mCount == 0) {
                if (onPanListeners != null) {
                    var i = 0
                    val z = onPanListeners!!.size
                    while (i < z) {
                        val listener = onPanListeners!![i]
                        listener.onPanEnd(this@R2FXLLayout)
                        i++
                    }
                }
            }
        }
    }

    private inner class SimpleOnGlobalLayoutChangedListener : ViewTreeObserver.OnGlobalLayoutListener {

        private var mLeft: Int = 0
        private var mTop: Int = 0
        private var mRight: Int = 0
        private var mBottom: Int = 0

        override fun onGlobalLayout() {
            val oldL = mLeft
            val oldT = mTop
            val oldR = mRight
            val oldB = mBottom
            mLeft = left
            mTop = top
            mRight = right
            mBottom = bottom
            val changed = oldL != mLeft || oldT != mTop || oldR != mRight || oldB != mBottom
            if (changed) {
                matrixUpdated()
                val p = closestValidTranslationPoint
                internalMove(p.x, p.y, false)
            }
        }

    }

    companion object {

        private const val DEF_ZOOM_DURATION = 250

        fun removeGlobal(v: View, listener: ViewTreeObserver.OnGlobalLayoutListener?) {
            val obs = v.viewTreeObserver
            obs.removeOnGlobalLayoutListener(listener)
        }
    }

}
