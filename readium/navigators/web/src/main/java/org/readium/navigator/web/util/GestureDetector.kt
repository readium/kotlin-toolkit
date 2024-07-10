/*
@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.web.util

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import kotlin.math.abs
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.tryOrNull
import timber.log.Timber

internal class GestureDetectors(
    private val contextClickListener: ContextClickListener? = null,
    private val doubleTapListener: DoubleTapListener? = null,
    private val gestureListener: GestureListener? = null
) {

    interface ContextClickListener {

        fun onContextClick()
    }

    interface DoubleTapListener {

        fun onDoubleTap()

        fun onDoubleTapEvent()

        fun onSignelTapConfirmed()
    }

    interface GestureListener {

        fun onDown()

        fun onFling(velocityX: Float, velocityY: Float)

        fun onScroll(distanceX: Float, distanceY: Float)
    }

    @UnsupportedAppUsage
    private var mTouchSlopSquare = 0
    private var mDoubleTapTouchSlopSquare = 0
    private var mDoubleTapSlopSquare = 0
    private var mAmbiguousGestureMultiplier = 0f

    @UnsupportedAppUsage
    private var mMinimumFlingVelocity = 0
    private var mMaximumFlingVelocity = 0

    private var mHandler: Handler? = null

    private var mStillDown = false
    private var mDeferConfirmSingleTap = false
    private var mInLongPress = false
    private var mInContextClick = false

    @UnsupportedAppUsage
    private var mAlwaysInTapRegion = false
    private var mAlwaysInBiggerTapRegion = false
    private var mIgnoreNextUpEvent = false

    // Whether a classification has been recorded by statsd for the current event stream. Reset on
    // ACTION_DOWN.
    private var mHasRecordedClassification = false

    private var mCurrentDownEvent: MotionEvent? = null
    private var mCurrentMotionEvent: MotionEvent? = null
    private var mPreviousUpEvent: MotionEvent? = null

    */
/**
 * True when the user is still touching for the second tap (down, move, and
 * up events). Can only be true if there is a double tap listener attached.
 *//*

    private var mIsDoubleTapping = false

    private var mLastFocusX = 0f
    private var mLastFocusY = 0f
    private var mDownFocusX = 0f
    private var mDownFocusY = 0f

    */
/**
 * @return true if longpress is enabled, else false.
 *//*

    */
/**
 * Set whether longpress is enabled, if this is enabled when a user
 * presses and holds down you get a longpress event and nothing further.
 * If it's disabled the user can press and hold down and then later
 * moved their finger and you will get scroll events. By default
 * longpress is enabled.
 *
 * @param isLongpressEnabled whether longpress should be enabled.
 *//*

    var isLongpressEnabled: Boolean = false

    */
/**
 * Determines speed during touch scrolling
 *//*

    private var mVelocityTracker: VelocityTracker? = null

    */
/**
 * Consistency verifier for debugging purposes.
 *//*

    private val mInputEventConsistencyVerifier: InputEventConsistencyVerifier? =
        if (InputEventConsistencyVerifier.isInstrumentationEnabled()) InputEventConsistencyVerifier(
            this, 0
        ) else null

    private inner class GestureHandler : Handler {

        constructor(handler: Handler) : super(handler.looper)

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                android.view.GestureDetector.Companion.SHOW_PRESS -> mListener!!.onShowPress(
                    mCurrentDownEvent!!
                )

                android.view.GestureDetector.Companion.LONG_PRESS -> {
                    recordGestureClassification(msg.arg1)
                    dispatchLongPress()
                }

                android.view.GestureDetector.Companion.TAP ->                     // If the user's finger is still down, do not count it as a tap
                    if (mDoubleTapListener != null) {
                        if (!mStillDown) {
                            recordGestureClassification(
                                TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__SINGLE_TAP
                            )
                            mDoubleTapListener!!.onSingleTapConfirmed(mCurrentDownEvent!!)
                        } else {
                            mDeferConfirmSingleTap = true
                        }
                    }

                else -> throw RuntimeException("Unknown message $msg") //never

            }
        }
    }

    init {
        if (handler != null) {
            mHandler = GestureHandler(handler)
        } else {
            mHandler = GestureHandler()
        }
        mListener = listener
        if (listener is android.view.GestureDetector.OnDoubleTapListener) {
            setOnDoubleTapListener(listener as android.view.GestureDetector.OnDoubleTapListener)
        }
        if (listener is android.view.GestureDetector.OnContextClickListener) {
            setContextClickListener(listener as android.view.GestureDetector.OnContextClickListener)
        }
        init(context)
    }

    private fun init(@UiContext context: Context?) {
        if (mListener == null) {
            throw NullPointerException("OnGestureListener must not be null")
        }
        isLongpressEnabled = true

        // Fallback to support pre-donuts releases
        val touchSlop: Int
        val doubleTapSlop: Int
        val doubleTapTouchSlop: Int
        if (context == null) {
            touchSlop = ViewConfiguration.getTouchSlop()
            doubleTapTouchSlop = touchSlop // Hack rather than adding a hidden method for this
            doubleTapSlop = ViewConfiguration.getDoubleTapSlop()
            mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity()
            mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity()
            mAmbiguousGestureMultiplier = ViewConfiguration.getAmbiguousGestureMultiplier()
        } else {
            StrictMode.assertConfigurationContext(context, "GestureDetector#init")
            val configuration = ViewConfiguration.get(context)
            touchSlop = configuration.scaledTouchSlop
            doubleTapTouchSlop = configuration.getScaledDoubleTapTouchSlop()
            doubleTapSlop = configuration.scaledDoubleTapSlop
            mMinimumFlingVelocity = configuration.scaledMinimumFlingVelocity
            mMaximumFlingVelocity = configuration.scaledMaximumFlingVelocity
            mAmbiguousGestureMultiplier = configuration.scaledAmbiguousGestureMultiplier
        }
        mTouchSlopSquare = touchSlop * touchSlop
        mDoubleTapTouchSlopSquare = doubleTapTouchSlop * doubleTapTouchSlop
        mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop
    }

    */
/**
 * Analyzes the given motion event and if applicable triggers the
 * appropriate callbacks on the [OnGestureListener] supplied.
 *
 * @param ev The current motion event.
 * @return true if the [OnGestureListener] consumed the event,
 * else false.
 *//*

    fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action

        if (mCurrentMotionEvent != null) {
            mCurrentMotionEvent!!.recycle()
        }
        mCurrentMotionEvent = MotionEvent.obtain(ev)

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(ev)

        val pointerUp =
            (action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP
        val skipIndex = if (pointerUp) ev.actionIndex else -1
        val isGeneratedGesture =
            (ev.flags and MotionEvent.FLAG_IS_GENERATED_GESTURE) !== 0

        // Determine focal point
        var sumX = 0f
        var sumY = 0f
        val count = ev.pointerCount
        for (i in 0 until count) {
            if (skipIndex == i) continue
            sumX += ev.getX(i)
            sumY += ev.getY(i)
        }
        val div = if (pointerUp) count - 1 else count
        val focusX = sumX / div
        val focusY = sumY / div

        var handled = false

        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                run {
                    mLastFocusX = focusX
                    mDownFocusX = mLastFocusX
                }
                run {
                    mLastFocusY = focusY
                    mDownFocusY = mLastFocusY
                }
                // Cancel long press and taps
                cancelTaps()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                run {
                    mLastFocusX = focusX
                    mDownFocusX = mLastFocusX
                }
                run {
                    mLastFocusY = focusY
                    mDownFocusY = mLastFocusY
                }
                // Check the dot product of current velocities.
                // If the pointer that left was opposing another velocity vector, clear.
                mVelocityTracker!!.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                val upIndex = ev.actionIndex
                val id1 = ev.getPointerId(upIndex)
                val x1 = mVelocityTracker!!.getXVelocity(id1)
                val y1 = mVelocityTracker!!.getYVelocity(id1)
                var i = 0
                while (i < count) {
                    if (i == upIndex) {
                        i++
                        continue
                    }

                    val id2 = ev.getPointerId(i)
                    val x = x1 * mVelocityTracker!!.getXVelocity(id2)
                    val y = y1 * mVelocityTracker!!.getYVelocity(id2)

                    val dot = x + y
                    if (dot < 0) {
                        mVelocityTracker!!.clear()
                        break
                    }
                    i++
                }
            }

            MotionEvent.ACTION_DOWN -> {
                if (doubleTapListener != null) {
                    val hadTapMessage =
                        mHandler!!.hasMessages(TAP)
                    if (hadTapMessage) mHandler!!.removeMessages(TAP)
                    if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null)
                        && hadTapMessage
                        && isConsideredDoubleTap(mCurrentDownEvent!!, mPreviousUpEvent!!, ev)
                    ) {
                        // This is a second tap
                        mIsDoubleTapping = true
                        // Give a callback with the first tap of the double-tap
                        handled =
                            handled or doubleTapListener.onDoubleTap(mCurrentDownEvent!!)
                        // Give a callback with down event of the double-tap
                        handled = handled or  doubleTapListener.onDoubleTapEvent(ev)
                    } else {
                        // This is a first tap
                        mHandler!!.sendEmptyMessageDelayed(
                            TAP,
                            DOUBLE_TAP_TIMEOUT.toLong()
                        )
                    }
                }

                mLastFocusX = focusX
                mDownFocusX = mLastFocusX

                mLastFocusY = focusY
                mDownFocusY = mLastFocusY

                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent!!.recycle()
                }
                mCurrentDownEvent = MotionEvent.obtain(ev)
                mAlwaysInTapRegion = true
                mAlwaysInBiggerTapRegion = true
                mStillDown = true
                mInLongPress = false
                mDeferConfirmSingleTap = false
                mHasRecordedClassification = false

                if (isLongpressEnabled) {
                    mHandler!!.removeMessages(LONG_PRESS)
                    mHandler!!.sendMessageAtTime(
                        mHandler!!.obtainMessage(
                            LONG_PRESS,
                            TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__LONG_PRESS,
                            0 */
/* arg2 *//*

                        ),
                        mCurrentDownEvent.getDownTime()
                            + ViewConfiguration.getLongPressTimeout()
                    )
                }
                mHandler!!.sendEmptyMessageAtTime(
                    SHOW_PRESS,
                    mCurrentDownEvent.getDownTime() + TAP_TIMEOUT
                )
                handled = handled or mListener!!.onDown(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mInLongPress || mInContextClick) {
                    break
                }

                val motionClassification = ev.classification
                val hasPendingLongPress =
                    mHandler!!.hasMessages(LONG_PRESS)

                val scrollX = mLastFocusX - focusX
                val scrollY = mLastFocusY - focusY
                if (mIsDoubleTapping) {
                    // Give the move events of the double-tap
                    recordGestureClassification(
                        TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__DOUBLE_TAP
                    )
                    handled = handled or doubleTapListener.onDoubleTapEvent(ev)
                } else if (mAlwaysInTapRegion) {
                    val deltaX = (focusX - mDownFocusX).toInt()
                    val deltaY = (focusY - mDownFocusY).toInt()
                    val distance = (deltaX * deltaX) + (deltaY * deltaY)
                    var slopSquare = if (isGeneratedGesture) 0 else mTouchSlopSquare

                    val ambiguousGesture =
                        motionClassification == MotionEvent.CLASSIFICATION_AMBIGUOUS_GESTURE
                    val shouldInhibitDefaultAction =
                        hasPendingLongPress && ambiguousGesture
                    if (shouldInhibitDefaultAction) {
                        // Inhibit default long press
                        if (distance > slopSquare) {
                            // The default action here is to remove long press. But if the touch
                            // slop below gets increased, and we never exceed the modified touch
                            // slop while still receiving AMBIGUOUS_GESTURE, we risk that *nothing*
                            // will happen in response to user input. To prevent this,
                            // reschedule long press with a modified timeout.
                            mHandler!!.removeMessages(LONG_PRESS)
                            val longPressTimeout =
                                ViewConfiguration.getLongPressTimeout().toLong()
                            mHandler!!.sendMessageAtTime(
                                mHandler!!.obtainMessage(
                                    LONG_PRESS,
                                    TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__LONG_PRESS,
                                    0 */
/* arg2 *//*

                                ),
                                (ev.downTime
                                    + (longPressTimeout * mAmbiguousGestureMultiplier).toLong())
                            )
                        }
                        // Inhibit default scroll. If a gesture is ambiguous, we prevent scroll
                        // until the gesture is resolved.
                        // However, for safety, simply increase the touch slop in case the
                        // classification is erroneous. Since the value is squared, multiply twice.
                        slopSquare =
                            (slopSquare * (mAmbiguousGestureMultiplier * mAmbiguousGestureMultiplier)).toInt()
                    }

                    if (distance > slopSquare) {
                        recordGestureClassification(
                            TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__SCROLL
                        )
                        handled = mListener!!.onScroll(mCurrentDownEvent, ev, scrollX, scrollY)
                        mLastFocusX = focusX
                        mLastFocusY = focusY
                        mAlwaysInTapRegion = false
                        mHandler!!.removeMessages(TAP)
                        mHandler!!.removeMessages(SHOW_PRESS)
                        mHandler!!.removeMessages(LONG_PRESS)
                    }
                    val doubleTapSlopSquare =
                        if (isGeneratedGesture) 0 else mDoubleTapTouchSlopSquare
                    if (distance > doubleTapSlopSquare) {
                        mAlwaysInBiggerTapRegion = false
                    }
                } else if ((abs(scrollX.toDouble()) >= 1) || (abs(scrollY.toDouble()) >= 1)) {
                    recordGestureClassification(TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__SCROLL)
                    handled = mListener!!.onScroll(mCurrentDownEvent, ev, scrollX, scrollY)
                    mLastFocusX = focusX
                    mLastFocusY = focusY
                }
                val deepPress =
                    motionClassification == MotionEvent.CLASSIFICATION_DEEP_PRESS
                if (deepPress && hasPendingLongPress) {
                    mHandler!!.removeMessages(LONG_PRESS)
                    mHandler!!.sendMessage(
                        mHandler!!.obtainMessage(
                            LONG_PRESS,
                            TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__DEEP_PRESS,
                            0 */
/* arg2 *//*

                        )
                    )
                }
            }

            MotionEvent.ACTION_UP -> {
                mStillDown = false
                val currentUpEvent = MotionEvent.obtain(ev)
                if (mIsDoubleTapping) {
                    // Finally, give the up event of the double-tap
                    recordGestureClassification(
                        TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__DOUBLE_TAP
                    )
                    handled = handled or mDoubleTapListener!!.onDoubleTapEvent(ev)
                } else if (mInLongPress) {
                    mHandler!!.removeMessages(android.view.GestureDetector.Companion.TAP)
                    mInLongPress = false
                } else if (mAlwaysInTapRegion && !mIgnoreNextUpEvent) {
                    recordGestureClassification(
                        TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__SINGLE_TAP
                    )
                    handled = mListener!!.onSingleTapUp(ev)
                    if (mDeferConfirmSingleTap && mDoubleTapListener != null) {
                        mDoubleTapListener!!.onSingleTapConfirmed(ev)
                    }
                } else if (!mIgnoreNextUpEvent) {
                    // A fling must travel the minimum tap distance

                    val velocityTracker = mVelocityTracker
                    val pointerId = ev.getPointerId(0)
                    velocityTracker!!.computeCurrentVelocity(
                        1000,
                        mMaximumFlingVelocity.toFloat()
                    )
                    val velocityY = velocityTracker.getYVelocity(pointerId)
                    val velocityX = velocityTracker.getXVelocity(pointerId)

                    if (((abs(velocityY.toDouble()) > mMinimumFlingVelocity)
                            || (abs(velocityX.toDouble()) > mMinimumFlingVelocity))
                    ) {
                        handled =
                            mListener!!.onFling(mCurrentDownEvent, ev, velocityX, velocityY)
                    }
                }
                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent!!.recycle()
                }
                // Hold the event we obtained above - listeners may have changed the original.
                mPreviousUpEvent = currentUpEvent
                if (mVelocityTracker != null) {
                    // This may have been cleared when we called out to the
                    // application above.
                    mVelocityTracker!!.recycle()
                    mVelocityTracker = null
                }
                mIsDoubleTapping = false
                mDeferConfirmSingleTap = false
                mIgnoreNextUpEvent = false
                mHandler!!.removeMessages(android.view.GestureDetector.Companion.SHOW_PRESS)
                mHandler!!.removeMessages(android.view.GestureDetector.Companion.LONG_PRESS)
            }

            MotionEvent.ACTION_CANCEL -> cancel()
        }
        if (!handled && mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onUnhandledEvent(ev, 0)
        }
        return handled
    }

    fun onTouchStart() {

    }

    fun onTouchMove() {

    }

    fun onTouchEnd() {

    }

    */
/**
 * Analyzes the given generic motion event and if applicable triggers the
 * appropriate callbacks on the [OnGestureListener] supplied.
 *
 * @param ev The current motion event.
 * @return true if the [OnGestureListener] consumed the event,
 * else false.
 *//*

    fun onGenericMotionEvent(ev: MotionEvent): Boolean {
        if (mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onGenericMotionEvent(ev, 0)
        }

        val actionButton = ev.actionButton
        when (ev.actionMasked) {
            MotionEvent.ACTION_BUTTON_PRESS -> if (((mContextClickListener != null) && !mInContextClick && !mInLongPress
                    && ((actionButton == MotionEvent.BUTTON_STYLUS_PRIMARY
                    || actionButton == MotionEvent.BUTTON_SECONDARY)))
            ) {
                if (mContextClickListener!!.onContextClick(ev)) {
                    mInContextClick = true
                    mHandler!!.removeMessages(android.view.GestureDetector.Companion.LONG_PRESS)
                    mHandler!!.removeMessages(android.view.GestureDetector.Companion.TAP)
                    return true
                }
            }

            MotionEvent.ACTION_BUTTON_RELEASE -> if (mInContextClick && ((actionButton == MotionEvent.BUTTON_STYLUS_PRIMARY
                    || actionButton == MotionEvent.BUTTON_SECONDARY))
            ) {
                mInContextClick = false
                mIgnoreNextUpEvent = true
            }
        }
        return false
    }

    private fun cancel() {
        mHandler!!.removeMessages(android.view.GestureDetector.Companion.SHOW_PRESS)
        mHandler!!.removeMessages(android.view.GestureDetector.Companion.LONG_PRESS)
        mHandler!!.removeMessages(android.view.GestureDetector.Companion.TAP)
        mVelocityTracker!!.recycle()
        mVelocityTracker = null
        mIsDoubleTapping = false
        mStillDown = false
        mAlwaysInTapRegion = false
        mAlwaysInBiggerTapRegion = false
        mDeferConfirmSingleTap = false
        mInLongPress = false
        mInContextClick = false
        mIgnoreNextUpEvent = false
    }

    private fun cancelTaps() {
        mHandler!!.removeMessages(android.view.GestureDetector.Companion.SHOW_PRESS)
        mHandler!!.removeMessages(android.view.GestureDetector.Companion.LONG_PRESS)
        mHandler!!.removeMessages(android.view.GestureDetector.Companion.TAP)
        mIsDoubleTapping = false
        mAlwaysInTapRegion = false
        mAlwaysInBiggerTapRegion = false
        mDeferConfirmSingleTap = false
        mInLongPress = false
        mInContextClick = false
        mIgnoreNextUpEvent = false
    }

    private fun isConsideredDoubleTap(
        firstDown: MotionEvent,
        firstUp: MotionEvent, secondDown: MotionEvent
    ): Boolean {
        if (!mAlwaysInBiggerTapRegion) {
            return false
        }

        val deltaTime = secondDown.eventTime - firstUp.eventTime
        if (deltaTime > android.view.GestureDetector.Companion.DOUBLE_TAP_TIMEOUT || deltaTime < android.view.GestureDetector.Companion.DOUBLE_TAP_MIN_TIME) {
            return false
        }

        val deltaX = firstDown.x.toInt() - secondDown.x.toInt()
        val deltaY = firstDown.y.toInt() - secondDown.y.toInt()
        val isGeneratedGesture =
            (firstDown.flags and MotionEvent.FLAG_IS_GENERATED_GESTURE) !== 0
        val slopSquare = if (isGeneratedGesture) 0 else mDoubleTapSlopSquare
        return (deltaX * deltaX + deltaY * deltaY < slopSquare)
    }

    private fun dispatchLongPress() {
        mHandler!!.removeMessages(TAP)
        mDeferConfirmSingleTap = false
        mInLongPress = true
        mListener!!.onLongPress((mCurrentDownEvent)!!)
    }

    companion object {
        private val TAG: String = android.view.GestureDetector::class.java.simpleName
        private val TAP_TIMEOUT = ViewConfiguration.getTapTimeout()
        private val DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout()
        private val DOUBLE_TAP_MIN_TIME: Int = 40 // ViewConfiguration.getDoubleTapMinTime()

        // constants for Message.what used by GestureHandler below
        private val SHOW_PRESS = 1
        private val LONG_PRESS = 2
        private val TAP = 3
    }

    @Suppress("Unused_parameter")
    @android.webkit.JavascriptInterface
    fun onDragStart(eventJson: String): Boolean {
        Timber.d("onDragStart $eventJson")
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

    }

    @Suppress("Unused_parameter")
    @android.webkit.JavascriptInterface
    fun onDragMove(eventJson: String): Boolean {
        Timber.d("onDragMove $eventJson")
        */
/*val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false *//*


        return true
    }


    @Suppress("Unused_parameter")
    @android.webkit.JavascriptInterface
    fun onDragEnd(eventJson: String): Boolean {
        Timber.d("onDragEnd $eventJson")
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false


        return true

    }
}

*/
/** Produced by gestures.js *//*

private data class DragEvent(
    val defaultPrevented: Boolean,
    val startPoint: PointF,
    val currentPoint: PointF,
    val offset: PointF,
    val interactiveElement: String?
) {
    internal val isValid: Boolean get() =
        !defaultPrevented && (interactiveElement == null)

    companion object {

        fun fromJSONObject(obj: JSONObject?): DragEvent? {
            obj ?: return null

            return DragEvent(
                defaultPrevented = obj.optBoolean("defaultPrevented"),
                startPoint = PointF(
                    obj.optDouble("startX").toFloat(),
                    obj.optDouble("startY").toFloat()
                ),
                currentPoint = PointF(
                    obj.optDouble("currentX").toFloat(),
                    obj.optDouble("currentY").toFloat()
                ),
                offset = PointF(
                    obj.optDouble("offsetX").toFloat(),
                    obj.optDouble("offsetY").toFloat()
                ),
                interactiveElement = obj.optNullableString("interactiveElement")
            )
        }

        fun fromJSON(json: String): DragEvent? =
            fromJSONObject(tryOrNull { JSONObject(json) })
    }
}
*/
