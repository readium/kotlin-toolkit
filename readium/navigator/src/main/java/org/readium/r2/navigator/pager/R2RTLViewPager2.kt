/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.preferences.ReadingProgression

/**
 * R2RTLViewPager2 is a wrapper around ViewPager2 that supports RTL (Right-to-Left) reading progression
 * and can be configured for both horizontal and vertical orientation.
 *
 * This class handles RTL layout direction based on ReadingProgression setting and provides
 * proper page ordering for different reading directions. It also intercepts touch events
 * when WebView cannot scroll horizontally to enable ViewPager2 swipe functionality.
 */
public open class R2RTLViewPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    // Internal ViewPager2 instance
    protected val viewPager2: ViewPager2 = ViewPager2(context)

    // Reading progression direction (LTR or RTL)
    public var readingProgression: ReadingProgression = ReadingProgression.LTR

    // Current layout direction
    private var mLayoutDirection = LAYOUT_DIRECTION_LTR

    // Touch event handling - store initial state for consistent behavior
    private var initialX = 0f
    private var initialY = 0f
    private var initialCanScrollLeft = false
    private var initialCanScrollRight = false
    private var initialCanScrollUp = false
    private var initialCanScrollDown = false

    init {
        viewPager2.offscreenPageLimit = 1
        // Add ViewPager2 to this container
        addView(viewPager2, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    // Delegate properties and methods to internal ViewPager2
    public var adapter: RecyclerView.Adapter<*>?
        get() = viewPager2.adapter
        set(value) {
            viewPager2.adapter = value
        }

    @ViewPager2.Orientation
    private var orientation: Int
        get() = viewPager2.orientation
        set(value) {
            if (value == ViewPager2.ORIENTATION_VERTICAL || value == ViewPager2.ORIENTATION_HORIZONTAL) {
                viewPager2.orientation = value
                optimizeForOrientation(value)
            }
        }

    public val currentItem: Int
        get() = viewPager2.currentItem

    @SuppressLint("NotifyDataSetChanged")
    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)

        // Override with reading progression setting
        val finalLayoutDirection = if (readingProgression == ReadingProgression.RTL) {
            LAYOUT_DIRECTION_RTL
        } else {
            LAYOUT_DIRECTION_LTR
        }

        if (finalLayoutDirection != mLayoutDirection) {
            val currentPosition = currentItem
            mLayoutDirection = finalLayoutDirection

            // Update ViewPager2's layout direction
            viewPager2.layoutDirection = finalLayoutDirection

            // Notify adapter of data change and restore position
            adapter?.notifyDataSetChanged()
            setCurrentItem(currentPosition, false)
        }
    }

    public fun isRtl(): Boolean {
        return mLayoutDirection == LAYOUT_DIRECTION_RTL
    }

    public fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        val position = item
        viewPager2.setCurrentItem(position, smoothScroll)
    }

    public fun setCurrentItem(item: Int) {
        setCurrentItem(item, true)
    }

    public fun registerOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        viewPager2.registerOnPageChangeCallback(callback)
    }

    public fun unregisterOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        viewPager2.unregisterOnPageChangeCallback(callback)
    }

    /**
     * State saving and restoration
     */
    public class SavedState : BaseSavedState {
        private val layoutDirection: Int

        public constructor(superState: Parcelable?, layoutDirection: Int) : super(superState) {
            this.layoutDirection = layoutDirection
        }

        private constructor(parcel: Parcel) : super(parcel) {
            layoutDirection = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeInt(layoutDirection)
        }

        public companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

        public fun getLayoutDirection(): Int = layoutDirection
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState, mLayoutDirection)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        mLayoutDirection = state.getLayoutDirection()
    }

    /**
     * Configure ViewPager2 for vertical scrolling
     */
    public fun configureForVerticalScrolling() {
        orientation = ViewPager2.ORIENTATION_VERTICAL
    }

    /**
     * Configure ViewPager2 for horizontal paging
     */
    public fun configureForHorizontalPaging() {
        orientation = ViewPager2.ORIENTATION_HORIZONTAL
    }

    /**
     * Optimize ViewPager2 settings based on orientation
     */
    private fun optimizeForOrientation(@ViewPager2.Orientation orientation: Int) {
        (viewPager2.getChildAt(0) as? RecyclerView)?.let { recyclerView ->
            when (orientation) {
                ViewPager2.ORIENTATION_VERTICAL -> {
                    // Optimize for vertical scrolling
                    recyclerView.layoutManager?.isItemPrefetchEnabled = false
                }

                ViewPager2.ORIENTATION_HORIZONTAL -> {
                    // Standard settings for horizontal scrolling
                    recyclerView.layoutManager?.isItemPrefetchEnabled = true
                }
            }
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y

                // Get real-time scroll capability from current WebView
                val currentWebView = findCurrentWebView()
                initialCanScrollLeft = currentWebView?.canScrollHorizontally(-1) ?: false
                initialCanScrollRight = currentWebView?.canScrollHorizontally(1) ?: false
                initialCanScrollUp = currentWebView?.canScrollVertically(-1) ?: false
                initialCanScrollDown = currentWebView?.canScrollVertically(1) ?: false
            }

            MotionEvent.ACTION_MOVE -> {
                val currentX = event.x
                val currentY = event.y
                val diffX = abs(currentX - initialX)
                val diffY = abs(currentY - initialY)

                when (orientation) {
                    ViewPager2.ORIENTATION_HORIZONTAL -> {
                        // Handle horizontal swipes for horizontal orientation
                        if (diffX > diffY && diffX > 10) {
                            val isSwipeLeft = currentX < initialX

                            val shouldIntercept = if (isSwipeLeft) {
                                // Swiping left - intercept only if WebView cannot scroll right
                                !initialCanScrollRight
                            } else {
                                // Swiping right - intercept only if WebView cannot scroll left
                                !initialCanScrollLeft
                            }

                            viewPager2.isUserInputEnabled = shouldIntercept
                        }
                    }

                    ViewPager2.ORIENTATION_VERTICAL -> {
                        // For vertical orientation, allow ViewPager2 to handle vertical swipes
                        // but let WebView handle horizontal scrolls if needed
                        if (diffY > diffX && diffY > 10) {
                            val isScrollUp = currentY < initialY

                            val shouldIntercept = if (isScrollUp) {
                                // Scrolling up - intercept only if WebView cannot scroll down
                                !initialCanScrollDown
                            } else {
                                // Scrolling down - intercept only if WebView cannot scroll up
                                !initialCanScrollUp
                            }

                            viewPager2.isUserInputEnabled = shouldIntercept
                        }
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    /**
     * Attempts to find the currently active WebView inside the current visible ViewPager2 page.
     */
    private fun findCurrentWebView(): WebView? {
        val recyclerView = viewPager2.getChildAt(0) as? RecyclerView ?: return null
        val layoutManager = recyclerView.layoutManager ?: return null

        // Get current ViewPager2 position
        val currentPosition = viewPager2.currentItem

        // Try to find view by position using LayoutManager
        val currentView = layoutManager.findViewByPosition(currentPosition)
        if (currentView != null) {
            val webView = findWebViewInView(currentView)
            if (webView != null) {
                return webView
            }
        }

        // Fallback: iterate through child views and match adapter position
        for (i in 0 until recyclerView.childCount) {
            val childView = recyclerView.getChildAt(i)
            val viewHolder = recyclerView.getChildViewHolder(childView)
            val adapterPosition = viewHolder.bindingAdapterPosition

            if (adapterPosition == currentPosition) {
                val webView = findWebViewInView(childView)
                if (webView != null) {
                    return webView
                }
            }
        }

        return null
    }

    /**
     * Recursively search for a WebView (or subclass) in a view hierarchy.
     */
    private fun findWebViewInView(view: View): WebView? {
        if (view is R2BasicWebView) return view
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val found = findWebViewInView(child)
                if (found != null) return found
            }
        }
        return null
    }
}
