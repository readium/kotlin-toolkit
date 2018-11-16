/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.content.Context
import android.graphics.Rect
import android.support.annotation.CallSuper
import android.support.v4.view.ViewCompat
import android.support.v4.view.WindowInsetsCompat
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.Interpolator
import android.widget.EdgeEffect
import android.widget.Scroller


/**
 * Created by Aferdita Muriqi on 12/2/17.
 */

class R2WebView(context: Context, attrs: AttributeSet) : R2BasicWebView(context, attrs) {

    init {
        initWebPager()
    }

    override fun scrollRight() {
        super.scrollRight()
        if (mCurItem < numPages-1) {
            mCurItem++
            activity.onPageChanged(mCurItem + 1, numPages, url)
        }
    }

    override fun scrollLeft() {
        super.scrollLeft()
        if (mCurItem > 0) {
            mCurItem--
            activity.onPageChanged(mCurItem + 1, numPages, url)
        }
    }

    private val TAG = "R2WebView"
    private val DEBUG = false

    private val USE_CACHE = false

    private val MAX_SETTLE_DURATION = 600 // ms
    private val MIN_DISTANCE_FOR_FLING = 25 // dips
    private val MIN_FLING_VELOCITY = 400 // dips


    internal fun getContentWidth(): Int {
        return this.computeHorizontalScrollRange()//working after load of page
    }

    override fun getContentHeight(): Int {
        return this.computeVerticalScrollRange() //working after load of page
    }

    internal class ItemInfo {
        var position: Int = 0
        var widthFactor: Float = 0.toFloat()
        var offset: Float = 0.toFloat()
    }

    private val sInterpolator = Interpolator { t1 ->
        var t = t1
        t -= 1.0f
        t * t * t * t * t + 1.0f
    }

    private val mTempItem = ItemInfo()

    private val mTempRect = Rect()

    internal var mCurItem: Int = 0   // Index of currently displayed page.

    private var mScroller: Scroller? = null
    private var mIsScrollStarted: Boolean = false

    private var mPageMargin: Int = 0

    private var mTopPageBounds: Int = 0
    private var mBottomPageBounds: Int = 0

    // Offsets of the first and last items, if known.
    // Set during population, used to determine if we are at the beginning
    // or end of the pager data set during touch scrolling.
    private val mFirstOffset = -java.lang.Float.MAX_VALUE
    private val mLastOffset = java.lang.Float.MAX_VALUE

    private var mScrollingCacheEnabled: Boolean = false

    private var mIsBeingDragged: Boolean = false
    private var mIsUnableToDrag: Boolean = false
    private var mGutterSize: Int = 0
    private var mTouchSlop: Int = 0
    /**
     * Position of the last motion event.
     */
    private var mLastMotionX: Float = 0.toFloat()
    private var mLastMotionY: Float = 0.toFloat()
    private var mInitialMotionX: Float = 0.toFloat()
    private var mInitialMotionY: Float = 0.toFloat()
    /**
     * Sentinel value for no current active pointer.
     * Used by [.mActivePointerId].
     */
    private val INVALID_POINTER = -1

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private var mActivePointerId = INVALID_POINTER

    /**
     * Determines speed during touch scrolling
     */
    private var mVelocityTracker: VelocityTracker? = null
    private var mMinimumVelocity: Int = 0
    private var mMaximumVelocity: Int = 0
    private var mFlingDistance: Int = 0
    private var mCloseEnough: Int = 0

    // If the pager is at least this close to its final position, complete the scroll
    // on touch down and let the user interact with the content inside instead of
    // "catching" the flinging pager.
    private val CLOSE_ENOUGH = 2 // dp

    private var mLeftEdge: EdgeEffect? = null
    private var mRightEdge: EdgeEffect? = null

    private var mFirstLayout = true

    private var mCalledSuper: Boolean = false
    private var mDecorChildCount: Int = 0

    /**
     * Indicates that the pager is in an idle, settled state. The current page
     * is fully in view and no animation is in progress.
     */
    val SCROLL_STATE_IDLE = 0

    /**
     * Indicates that the pager is currently being dragged by the user.
     */
    val SCROLL_STATE_DRAGGING = 1

    /**
     * Indicates that the pager is in the process of settling to a final position.
     */
    val SCROLL_STATE_SETTLING = 2

    private val mEndScrollRunnable = Runnable { setScrollState(SCROLL_STATE_IDLE) }

    private var mScrollState = SCROLL_STATE_IDLE

    internal fun initWebPager() {
        setWillNotDraw(false)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        isFocusable = true
        val context = context
        mScroller = Scroller(context, sInterpolator)
        val configuration = ViewConfiguration.get(context)
        val density = context.resources.displayMetrics.density

        mTouchSlop = configuration.scaledPagingTouchSlop
        mMinimumVelocity = (MIN_FLING_VELOCITY * density).toInt()
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
        mLeftEdge = EdgeEffect(context)
        mRightEdge = EdgeEffect(context)

        mFlingDistance = (MIN_DISTANCE_FOR_FLING * density).toInt()
        mCloseEnough = (CLOSE_ENOUGH * density).toInt()

        if (ViewCompat.getImportantForAccessibility(this) == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        }

        ViewCompat.setOnApplyWindowInsetsListener(this,
                object : android.support.v4.view.OnApplyWindowInsetsListener {
                    private val mTempRect = Rect()

                    override fun onApplyWindowInsets(v: View,
                                                     originalInsets: WindowInsetsCompat): WindowInsetsCompat {
                        // First let the ViewPager itself try and consume them...
                        val applied = ViewCompat.onApplyWindowInsets(v, originalInsets)
                        if (applied.isConsumed) {
                            // If the ViewPager consumed all insets, return now
                            return applied
                        }

                        // Now we'll manually dispatch the insets to our children. Since ViewPager
                        // children are always full-height, we do not want to use the standard
                        // ViewGroup dispatchApplyWindowInsets since if child 0 consumes them,
                        // the rest of the children will not receive any insets. To workaround this
                        // we manually dispatch the applied insets, not allowing children to
                        // consume them from each other. We do however keep track of any insets
                        // which are consumed, returning the union of our children's consumption
                        val res = mTempRect
                        res.left = applied.systemWindowInsetLeft
                        res.top = applied.systemWindowInsetTop
                        res.right = applied.systemWindowInsetRight
                        res.bottom = applied.systemWindowInsetBottom

                        var i = 0
                        val count = childCount
                        while (i < count) {
                            val childInsets = ViewCompat
                                    .dispatchApplyWindowInsets(getChildAt(i), applied)
                            // Now keep track of any consumed by tracking each dimension's min
                            // value
                            res.left = Math.min(childInsets.systemWindowInsetLeft,
                                    res.left)
                            res.top = Math.min(childInsets.systemWindowInsetTop,
                                    res.top)
                            res.right = Math.min(childInsets.systemWindowInsetRight,
                                    res.right)
                            res.bottom = Math.min(childInsets.systemWindowInsetBottom,
                                    res.bottom)
                            i++
                        }

                        // Now return a new WindowInsets, using the consumed window insets
                        return applied.replaceSystemWindowInsets(
                                res.left, res.top, res.right, res.bottom)
                    }
                })
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(mEndScrollRunnable)
        // To be on the safe side, abort the scroller
        if (mScroller != null && !mScroller!!.isFinished) {
            mScroller!!.abortAnimation()
        }
        super.onDetachedFromWindow()
    }

    private fun setScrollState(newState: Int) {
        if (mScrollState == newState) {
            return
        }
        mScrollState = newState
    }


    private fun getClientWidth(): Int {
        return (measuredWidth - paddingLeft - paddingRight) + 2
    }

    /**
     * Set the currently selected page.
     *
     * @param item Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    fun setCurrentItem(item: Int, smoothScroll: Boolean ) {
       setCurrentItemInternal(item, smoothScroll, false)
    }

    fun calculateCurrentItem() {
        val currentPage = numPages * progression
        mCurItem = currentPage.toInt()
    }

    internal fun setCurrentItemInternal(item: Int, smoothScroll: Boolean, always: Boolean) {
        setCurrentItemInternal(item, smoothScroll, 0)
    }

    internal fun setCurrentItemInternal(item: Int, smoothScroll: Boolean, velocity: Int) {
        if (mFirstLayout) {
            // We don't have any idea how big we are yet and shouldn't have any pages either.
            // Just set things up and let the pending layout handle things.
            mCurItem = item
            requestLayout()
        } else {
            mCurItem = item
            scrollToItem(item, smoothScroll, velocity,true)
        }
    }

    private fun scrollToItem(item: Int, smoothScroll: Boolean, velocity: Int, post: Boolean) {

        // todo double check why +2 is needed here
        val destX = (getClientWidth() * item)
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity)
        } else {
            completeScroll(false)
            scrollTo(destX, 0)
            pageScrolled(destX)
        }

        if (post) {
            activity.onPageChanged(item + 1, numPages, url)
        }


    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    internal fun distanceInfluenceForSnapDuration(f: Float): Float {
        var float = f
        float -= 0.5f // center the values about 0.
        float *= 0.3f * Math.PI.toFloat() / 2.0f
        return Math.sin(float.toDouble()).toFloat()
    }


    /**
     * Like [View.scrollBy], but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    internal fun smoothScrollTo(x: Int, y: Int, velocity: Int) {
        var v = velocity
        val sx: Int
        val wasScrolling = mScroller != null && !mScroller!!.isFinished
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.
            sx = if (mIsScrollStarted) mScroller!!.currX else mScroller!!.startX
            // And abort the current scrolling.
            mScroller!!.abortAnimation()
            setScrollingCacheEnabled(false)
        } else {
            sx = scrollX
        }
        val sy = scrollY
        val dx = x - sx
        val dy = y - sy
        if (dx == 0 && dy == 0) {
            completeScroll(false)
            setScrollState(SCROLL_STATE_IDLE)
            return
        }

        setScrollingCacheEnabled(true)
        setScrollState(SCROLL_STATE_SETTLING)

        val width = getClientWidth()
        val halfWidth = width / 2
        val distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width)
        val distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio)

        var duration: Int
        v = Math.abs(v)
        duration = if (v > 0) {
            4 * Math.round(1000 * Math.abs(distance / v))
        } else {
            //            final float pageWidth = width * mAdapter.getPageWidth(mCurItem);
            val pageDelta = Math.abs(dx).toFloat() / (width + mPageMargin)
            ((pageDelta + 1) * 100).toInt()
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION)

        // Reset the "scroll started" flag. It will be flipped to true in all places
        // where we call computeScrollOffset().
        mIsScrollStarted = false
        mScroller!!.startScroll(sx, sy, dx, dy, duration)
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun infoForPosition(position: Int): ItemInfo {

        val ii = ItemInfo()
        ii.position = position
        ii.offset = (position * (1 / numPages)).toFloat()

        return ii
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Make sure scroll position is set correctly.
        if (w != oldw) {
            recomputeScrollPosition(w, oldw, mPageMargin, mPageMargin)
        }
    }

    private fun recomputeScrollPosition(width: Int, oldWidth: Int, margin: Int, oldMargin: Int) {
        if (oldWidth > 0 /*&& !mItems.isEmpty()*/) {
            if (!mScroller!!.isFinished) {
                val currentPage = scrollX / getClientWidth()

                mScroller!!.finalX = currentPage * getClientWidth()
            } else {
                val widthWithMargin = width - paddingLeft - paddingRight + margin
                val oldWidthWithMargin = oldWidth - paddingLeft - paddingRight + oldMargin
                val xpos = scrollX
                val pageOffset = xpos.toFloat() / oldWidthWithMargin
                val newOffsetPixels = (pageOffset * widthWithMargin).toInt()

                scrollTo(newOffsetPixels, scrollY)
            }
        } else {
            val ii = infoForPosition(mCurItem)
            val scrollOffset: Float = if (ii != null) Math.min(ii.offset, mLastOffset) else 0.0F
            val scrollPos = (scrollOffset * (width - paddingLeft - paddingRight)).toInt()
            if (scrollPos != scrollX) {
                completeScroll(false)
                scrollTo(scrollPos, scrollY)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val width = r - l
        val height = b - t
        var paddingLeft = paddingLeft
        var paddingTop = paddingTop
        var paddingRight = paddingRight
        var paddingBottom = paddingBottom
        val scrollX = scrollX

        var decorCount = 0

        // First pass - decor views. We need to do this in two passes so that
        // we have the proper offsets for non-decor views later.
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as LayoutParams
                var childLeft = 0
                var childTop = 0
                if (lp.isDecor) {
                    val hgrav = lp.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                    val vgrav = lp.gravity and Gravity.VERTICAL_GRAVITY_MASK
                    when (hgrav) {
                        Gravity.START -> {
                            childLeft = paddingLeft
                            paddingLeft += child.measuredWidth
                        }
                        Gravity.CENTER_HORIZONTAL -> childLeft = Math.max((width - child.measuredWidth) / 2,
                                paddingLeft)
                        Gravity.END -> {
                            childLeft = width - paddingRight - child.measuredWidth
                            paddingRight += child.measuredWidth
                        }
                        else -> childLeft = paddingLeft
                    }
                    when (vgrav) {
                        Gravity.TOP -> {
                            childTop = paddingTop
                            paddingTop += child.measuredHeight
                        }
                        Gravity.CENTER_VERTICAL -> childTop = Math.max((height - child.measuredHeight) / 2,
                                paddingTop)
                        Gravity.BOTTOM -> {
                            childTop = height - paddingBottom - child.measuredHeight
                            paddingBottom += child.measuredHeight
                        }
                        else -> childTop = paddingTop
                    }
                    childLeft += scrollX
                    child.layout(childLeft, childTop,
                            childLeft + child.measuredWidth,
                            childTop + child.measuredHeight)
                    decorCount++
                }
            }
        }

        mTopPageBounds = paddingTop
        mBottomPageBounds = height - paddingBottom
        mDecorChildCount = decorCount

        if (mFirstLayout) {
            scrollToItem(mCurItem, false, 0, false)
        }
        mFirstLayout = false
    }

    override fun computeScroll() {
        mIsScrollStarted = true
        if (!mScroller!!.isFinished && mScroller!!.computeScrollOffset()) {
            val oldX = scrollX
            val oldY = scrollY
            val x = mScroller!!.currX
            val y = mScroller!!.currY

            if (oldX != x || oldY != y) {
                scrollTo(x, y)
                if (!pageScrolled(x)) {
                    mScroller!!.abortAnimation()
                    scrollTo(0, y)
                }
            }

            // Keep on drawing until the animation has finished.
            ViewCompat.postInvalidateOnAnimation(this)
            return
        }

        // Done with scroll, clean up state.
        completeScroll(true)
    }

    private fun pageScrolled(xpos: Int): Boolean {
        val ii = infoForCurrentScrollPosition()
        val width = getClientWidth()
        val widthWithMargin = width + mPageMargin
        val marginOffset = mPageMargin.toFloat() / width
        val currentPage = ii!!.position
        val pageOffset = (xpos.toFloat() / width - ii.offset) / (ii.widthFactor + marginOffset)
        val offsetPixels = (pageOffset * widthWithMargin).toInt()

        mCalledSuper = false
        onPageScrolled(currentPage, pageOffset, offsetPixels)
        if (!mCalledSuper) {
            throw IllegalStateException(
                    "onPageScrolled did not call superclass implementation")
        }
        return true
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     * If you override this method you must call through to the superclass implementation
     * (e.g. super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled
     * returns.
     *
     * @param position Position index of the first page currently being displayed.
     * Page position+1 will be visible if positionOffset is nonzero.
     * @param offset Value from [0, 1) indicating the offset from the page at position.
     * @param offsetPixels Value in pixels indicating the offset from position.
     */
    @CallSuper
    private fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
        // Offset any decor views if needed - keep them on-screen at all times.
        if (mDecorChildCount > 0) {
            val scrollX = scrollX
            var paddingLeft = paddingLeft
            var paddingRight = paddingRight
            val width = width
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val lp = child.layoutParams as LayoutParams
                if (!lp.isDecor) continue

                val hgrav = lp.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                var childLeft = 0
                when (hgrav) {
                    Gravity.START -> {
                        childLeft = paddingLeft
                        paddingLeft += child.width
                    }
                    Gravity.CENTER_HORIZONTAL -> childLeft = Math.max((width - child.measuredWidth) / 2,
                            paddingLeft)
                    Gravity.END -> {
                        childLeft = width - paddingRight - child.measuredWidth
                        paddingRight += child.measuredWidth
                    }
                    else -> childLeft = paddingLeft
                }
                childLeft += scrollX

                val childOffset = childLeft - child.left
                if (childOffset != 0) {
                    child.offsetLeftAndRight(childOffset)
                }
            }
        }

        mCalledSuper = true
    }


    private fun completeScroll(postEvents: Boolean) {
        val needPopulate = mScrollState == SCROLL_STATE_SETTLING
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false)
            val wasScrolling = !mScroller!!.isFinished
            if (wasScrolling) {
                mScroller!!.abortAnimation()
                val oldX = scrollX
                val oldY = scrollY
                val x = mScroller!!.currX
                val y = mScroller!!.currY
                if (oldX != x || oldY != y) {
                    scrollTo(x, y)
                    if (x != oldX) {
                        pageScrolled(x)
                    }
                }
            }
        }
        if (needPopulate) {
            if (postEvents) {
                ViewCompat.postOnAnimation(this, mEndScrollRunnable)
            } else {
                mEndScrollRunnable.run()
            }
        }
    }

    private fun isGutterDrag(x: Float, dx: Float): Boolean {
        return x < mGutterSize && dx > 0 || x > width - mGutterSize && dx < 0
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        val action = ev.action and MotionEvent.ACTION_MASK

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG) Log.v(TAG, "Intercept done!")
            return false
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!")
                return true
            }
            if (mIsUnableToDrag) {
                if (DEBUG) Log.v(TAG, "Intercept returning false!")
                return false
            }
        }

        when (action) {
            MotionEvent.ACTION_MOVE -> {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionY is set to the y value
                 * of the down event.
                 */
                val activePointerId = mActivePointerId
                val pointerIndex = ev.findPointerIndex(activePointerId)
                val x = ev.getX(pointerIndex)
                val dx = x - mLastMotionX
                val xDiff = Math.abs(dx)
                val y = ev.getY(pointerIndex)
                val yDiff = Math.abs(y - mInitialMotionY)
                if (DEBUG) Log.v(TAG, "Moved x to $x,$y diff=$xDiff,$yDiff")

                if (dx != 0f && !isGutterDrag(mLastMotionX, dx)
                        && canScroll(this, false, dx.toInt(), x.toInt(), y.toInt())) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    mLastMotionX = x
                    mLastMotionY = y
                    mIsUnableToDrag = true
                    return false
                }
                if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                    if (DEBUG) Log.v(TAG, "Starting drag!")
                    mIsBeingDragged = true
                    //                    requestParentDisallowInterceptTouchEvent(false);
                    setScrollState(SCROLL_STATE_DRAGGING)
                    mLastMotionX = if (dx > 0)
                        mInitialMotionX + mTouchSlop
                    else
                        mInitialMotionX - mTouchSlop
                    mLastMotionY = y
                    setScrollingCacheEnabled(true)
                } else if (yDiff > mTouchSlop) {
                    // The finger has moved enough in the vertical
                    // direction to be counted as a drag...  abort
                    // any attempt to drag horizontally, to work correctly
                    // with children that have scrolling containers.
                    if (DEBUG) Log.v(TAG, "Starting unable to drag!")
                    mIsUnableToDrag = true
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    if (performDrag(x)) {
                        ViewCompat.postInvalidateOnAnimation(this)
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mInitialMotionX = ev.x
                mLastMotionX = mInitialMotionX
                mInitialMotionY = ev.y
                mLastMotionY = mInitialMotionY
                mActivePointerId = ev.getPointerId(0)
                mIsUnableToDrag = false

                mIsScrollStarted = true
                mScroller!!.computeScrollOffset()
                if (mScrollState == SCROLL_STATE_SETTLING && Math.abs(mScroller!!.finalX - mScroller!!.currX) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    mScroller!!.abortAnimation()
                    mIsBeingDragged = true
                    setScrollState(SCROLL_STATE_DRAGGING)
                } else {
                    completeScroll(false)
                    mIsBeingDragged = false
                }

                if (DEBUG) {
                    Log.v(TAG, "Down at " + mLastMotionX + "," + mLastMotionY
                            + " mIsBeingDragged=" + mIsBeingDragged
                            + "mIsUnableToDrag=" + mIsUnableToDrag)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(ev)

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(ev)

        val action = ev.action
        var needsInvalidate = false
        when (action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {

                mScroller!!.abortAnimation()

                // Remember where the motion event started
                mInitialMotionX = ev.x
                mLastMotionX = mInitialMotionX
                mInitialMotionY = ev.y
                mLastMotionY = mInitialMotionY
                mActivePointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mIsBeingDragged) {
                    val pointerIndex = ev.findPointerIndex(mActivePointerId)
                    val x = ev.getX(pointerIndex)
                    val xDiff = Math.abs(x - mLastMotionX)
                    val y = ev.getY(pointerIndex)
                    val yDiff = Math.abs(y - mLastMotionY)
                    if (DEBUG) {
                        Log.v(TAG, "Moved x to $x,$y diff=$xDiff,$yDiff")
                    }
                    if (xDiff > mTouchSlop && xDiff > yDiff) {
                        if (DEBUG) Log.v(TAG, "Starting drag!")
                        mIsBeingDragged = true
                        //                        requestParentDisallowInterceptTouchEvent(false);
                        mLastMotionX = if (x - mInitialMotionX > 0)
                            mInitialMotionX + mTouchSlop
                        else
                            mInitialMotionX - mTouchSlop
                        mLastMotionY = y
                        setScrollState(SCROLL_STATE_DRAGGING)
                        setScrollingCacheEnabled(true)
                    }
                }
                // Not else! Note that mIsBeingDragged can be set above.
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                    val x = ev.getX(activePointerIndex)
                    needsInvalidate = needsInvalidate or performDrag(x)
                }
            }
            MotionEvent.ACTION_UP -> if (mIsBeingDragged) {
                val velocityTracker = mVelocityTracker
                velocityTracker!!.computeCurrentVelocity(2000, mMaximumVelocity.toFloat())
                val initialVelocity = velocityTracker.getXVelocity(mActivePointerId).toInt()

                val currentPage = scrollX / getClientWidth()
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                val x = ev.getX(activePointerIndex)
                val totalDelta = (x - mInitialMotionX).toInt()
                val nextPage = determineTargetPage(currentPage, 0f, initialVelocity, totalDelta)

                setCurrentItemInternal(nextPage, true, initialVelocity)
            } else {
                val scrollMode = activity.preferences.getBoolean("scroll", false)
                val position = (ev.x % getClientWidth()) / getClientWidth()
                if (!scrollMode) {
                    when {
                        position <= 0.2 -> scrollLeft()
                        position >= 0.8 -> scrollRight()
                        else -> centerTapped()
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> if (mIsBeingDragged) {
                scrollToItem(mCurItem, true, 0, false)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                val x = ev.getX(index)
                mLastMotionX = x
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId))
            }
        }
        val scrollMode = activity.preferences.getBoolean("scroll", false)
        if (scrollMode) {
            return super.onTouchEvent(ev)
        }
//        super.onTouchEvent(ev);
        return true
    }

    private fun performDrag(x: Float): Boolean {
        var needsInvalidate = false

        val deltaX = mLastMotionX - x
        mLastMotionX = x

        val oldScrollX = scrollX.toFloat()
        var scrollX = oldScrollX + deltaX
        val width = getClientWidth()

        var leftBound = width * mFirstOffset
        var rightBound = width * mLastOffset
        var leftAbsolute = true
        var rightAbsolute = true

        val firstItem = ItemInfo()
        firstItem.position = 0
        firstItem.offset = 0f
        val lastItem = ItemInfo()
        lastItem.position = numPages - 1
        lastItem.offset = 1f

        if (firstItem.position != 0) {
            leftAbsolute = false
            leftBound = firstItem.offset * width
        }
        if (lastItem.position != numPages - 1) {
            rightAbsolute = false
            rightBound = lastItem.offset * width
        }

        if (scrollX < leftBound) {
            if (leftAbsolute) {
                val over = leftBound - scrollX
                mLeftEdge!!.onPull(Math.abs(over) / width)
                needsInvalidate = true
            }
            scrollX = leftBound
        } else if (scrollX > rightBound) {
            if (rightAbsolute) {
                val over = scrollX - rightBound
                mRightEdge!!.onPull(Math.abs(over) / width)
                needsInvalidate = true
            }
            scrollX = rightBound
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - scrollX.toInt()
        scrollTo(scrollX.toInt(), scrollY)
        pageScrolled(scrollX.toInt())

        return needsInvalidate
    }

    /**
     * @return Info about the page at the current scroll position.
     * This can be synthetic for a missing middle page; the 'object' field can be null.
     */
    private fun infoForCurrentScrollPosition(): ItemInfo? {
        val width = getClientWidth()
        val scrollOffset: Float = if (width > 0) scrollX.toFloat() / width else 0F
        val marginOffset: Float = if (width > 0) mPageMargin.toFloat() / width else 0F
        var lastPos = -1
        var lastOffset = 0f
        var lastWidth = 0f
        var first = true

        var lastItem: ItemInfo? = null
        var i = 0
        while (i < numPages/*mItems.size()*/) {
            //            ItemInfo ii = mItems.get(i);
            var ii = ItemInfo()
            //            ii.position = i;
            //            ii.offset = i * (1 / numPages);
            val offset: Float
            if (!first && ii.position != lastPos + 1) {
                // Create a synthetic item for a missing page.
                ii = mTempItem
                ii.offset = lastOffset + lastWidth + marginOffset
                ii.position = lastPos + 1
                ii.widthFactor = getClientWidth().toFloat()/*mAdapter.getPageWidth(ii.position)*/
                i--
            }
            offset = ii.offset

            val rightBound = offset + ii.widthFactor + marginOffset
            if (first || scrollOffset >= offset) {
                if (scrollOffset < rightBound || i == numPages/*mItems.size()*/ - 1) {
                    return ii
                }
            } else {
                return lastItem
            }
            first = false
            lastPos = ii.position
            lastOffset = offset
            lastWidth = ii.widthFactor
            lastItem = ii
            i++
        }

        return lastItem
    }

    private fun determineTargetPage(currentPage: Int, pageOffset: Float, velocity: Int, deltaX: Int): Int {
        val targetPage: Int
        val d = Math.abs(deltaX)
        val v = Math.abs(velocity)

        targetPage = if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            if (velocity > 0) currentPage else currentPage + 1
        } else {
            val truncator = if (currentPage >= mCurItem) 0.4f else 0.6f
            currentPage + (pageOffset + truncator).toInt()
        }

        return targetPage
    }


    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionX = ev.getX(newPointerIndex)
            mActivePointerId = ev.getPointerId(newPointerIndex)
            if (mVelocityTracker != null) {
                mVelocityTracker!!.clear()
            }
        }
    }

    private fun setScrollingCacheEnabled(enabled: Boolean) {
        if (mScrollingCacheEnabled != enabled) {
            mScrollingCacheEnabled = enabled
            if (USE_CACHE) {
                val size = childCount
                for (i in 0 until size) {
                    val child = getChildAt(i)
                    if (child.visibility != View.GONE) {
                        child.isDrawingCacheEnabled = enabled
                    }
                }
            }
        }
    }


    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     * or just its children (false).
     * @param dx Delta scrolled in pixels
     * @param x X coordinate of the active touch point
     * @param y Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    private fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        if (v is ViewGroup) {
            val scrollX = v.getScrollX()
            val scrollY = v.getScrollY()
            val count = v.childCount
            // Count backwards - let topmost views consume scroll distance first.
            for (i in count - 1 downTo 0) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                val child = v.getChildAt(i)
                if (x + scrollX >= child.left && x + scrollX < child.right
                        && y + scrollY >= child.top && y + scrollY < child.bottom
                        && canScroll(child, true, dx, x + scrollX - child.left,
                                y + scrollY - child.top)) {
                    return true
                }
            }
        }

        return checkV && v.canScrollHorizontally(-dx)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event)
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    fun executeKeyEvent(event: KeyEvent): Boolean {
        var handled = false
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> handled = if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                    pageLeft()
                } else {
                    arrowScroll(View.FOCUS_LEFT)
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> handled = if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                    pageRight()
                } else {
                    arrowScroll(View.FOCUS_RIGHT)
                }
                KeyEvent.KEYCODE_TAB -> if (event.hasNoModifiers()) {
                    handled = arrowScroll(View.FOCUS_FORWARD)
                } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                    handled = arrowScroll(View.FOCUS_BACKWARD)
                }
            }
        }
        return handled
    }

    /**
     * Handle scrolling in response to a left or right arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was pressed. It should be
     * either [View.FOCUS_LEFT] or [View.FOCUS_RIGHT].
     * @return Whether the scrolling was handled successfully.
     */
    fun arrowScroll(direction: Int): Boolean {
        var currentFocused: View? = findFocus()
        if (currentFocused === this) {
            currentFocused = null
        } else if (currentFocused != null) {
            var isChild = false
            run {
                var parent = currentFocused!!.parent
                while (parent is ViewGroup) {
                    if (parent === this) {
                        isChild = true
                        break
                    }
                    parent = parent.parent
                }
            }
            if (!isChild) {
                // This would cause the focus search down below to fail in fun ways.
                val sb = StringBuilder()
                sb.append(currentFocused.javaClass.simpleName)
                var parent = currentFocused.parent
                while (parent is ViewGroup) {
                    sb.append(" => ").append(parent.javaClass.simpleName)
                    parent = parent.parent
                }
                Log.e(TAG, "arrowScroll tried to find focus based on non-child "
                        + "current focused view " + sb.toString())
                currentFocused = null
            }
        }

        var handled = false

        val nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
                direction)
        if (nextFocused != null && nextFocused !== currentFocused) {
            if (direction == View.FOCUS_LEFT) {
                // If there is nothing to the left, or this is causing us to
                // jump to the right, then what we really want to do is page left.
                val nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left
                val currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left
                handled = if (currentFocused != null && nextLeft >= currLeft) {
                    pageLeft()
                } else {
                    nextFocused.requestFocus()
                }
            } else if (direction == View.FOCUS_RIGHT) {
                // If there is nothing to the right, or this is causing us to
                // jump to the left, then what we really want to do is page right.
                val nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left
                val currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left
                handled = if (currentFocused != null && nextLeft <= currLeft) {
                    pageRight()
                } else {
                    nextFocused.requestFocus()
                }
            }
        } else if (direction == View.FOCUS_LEFT || direction == View.FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = pageLeft()
        } else if (direction == View.FOCUS_RIGHT || direction == View.FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = pageRight()
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction))
        }
        return handled
    }

    private fun getChildRectInPagerCoordinates(outRect1: Rect?, child: View?): Rect {
        var outRect = outRect1
        if (outRect == null) {
            outRect = Rect()
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0)
            return outRect
        }
        outRect.left = child.left
        outRect.right = child.right
        outRect.top = child.top
        outRect.bottom = child.bottom

        var parent = child.parent
        while (parent is ViewGroup && parent !== this) {
            val group = parent
            outRect.left += group.left
            outRect.right += group.right
            outRect.top += group.top
            outRect.bottom += group.bottom

            parent = group.parent
        }
        return outRect
    }

    internal fun pageLeft(): Boolean {
        if (mCurItem > 0) {
            setCurrentItem(mCurItem - 1, true)
            return true
        }
        return false
    }

    internal fun pageRight(): Boolean {
        if (mCurItem < numPages) {
            setCurrentItem(mCurItem + 1, true)
            return true
        }
        return false
    }

    internal val numPages: Int
        get() {
            var numPages: Int = 0
            try {
                numPages = getContentWidth() / (getClientWidth() - 2)
            } finally {
                if (numPages == 0) {
                    numPages = 1
                }
            }
            return numPages
        }


    /**
     * Layout parameters that should be supplied for views added to a
     * ViewPager.
     */
    class LayoutParams : ViewGroup.LayoutParams {
        /**
         * true if this view is a decoration on the pager itself and not
         * a view supplied by the adapter.
         */
        var isDecor: Boolean = false

        val LAYOUT_ATTRS = intArrayOf(android.R.attr.layout_gravity)

        /**
         * Gravity setting for use on decor views only:
         * Where to position the view page within the overall ViewPager
         * container; constants are defined in [android.view.Gravity].
         */
        var gravity: Int = 0

        /**
         * Width as a 0-1 multiplier of the measured pager width
         */
        internal var widthFactor = 0f

        /**
         * true if this view was added during layout and needs to be measured
         * before being positioned.
         */
        internal var needsMeasure: Boolean = false

        /**
         * Adapter position this view is for if !isDecor
         */
        internal var position: Int = 0

        /**
         * Current child index within the ViewPager that this view occupies
         */
        internal var childIndex: Int = 0

        constructor() : super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {}

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

            val a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS)
            gravity = a.getInteger(0, Gravity.TOP)
            a.recycle()
        }
    }
}

interface PageCallback {
    fun onPageChanged(pageIndex: Int, totalPages: Int, url: String)
}