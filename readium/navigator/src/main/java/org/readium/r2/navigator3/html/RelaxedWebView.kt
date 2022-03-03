package org.readium.r2.navigator3.html

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import timber.log.Timber

internal class RelaxedWebView(context: Context) : WebView(context)  {

    interface OnScrollChangeListener {

        fun onScrollChange(view: View, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int)
    }

    interface OnSizeChangeListener {

        fun onSizeChanged (view: View, w: Int, h: Int, ow: Int, oh: Int)
    }

    val verticalScrollExtent: Int
        get() = computeVerticalScrollExtent()

    val verticalScrollRange: Int
        get() = computeVerticalScrollRange()

    val verticalScrollOffset: Int
        get() = computeVerticalScrollOffset()

    val horizontalScrollExtent: Int
        get() = computeHorizontalScrollExtent()

    val horizontalScrollRange: Int
        get() = computeHorizontalScrollRange()

    val horizontalScrollOffset: Int
        get() = computeHorizontalScrollOffset()

    val canScrollLeft: Boolean
        get() = canScrollHorizontally(-1)

    val canScrollRight: Boolean
        get() = canScrollHorizontally(1)

    val canScrollTop: Boolean
        get() = canScrollVertically(-1)

    val canScrollBottom: Boolean
        get() = canScrollVertically(1)

    var onScrollChangeListener: OnScrollChangeListener? = null

    var onSizeChangeListener: OnSizeChangeListener? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        Timber.v("onScrollChanged $l $t $oldl $oldt")
        onScrollChangeListener?.onScrollChange(this, l, t, oldl, oldt)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        Timber.v("onSizeChanged $w $h $ow $oh")
        onSizeChangeListener?.onSizeChanged(this, w, h, ow, oh)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
            .also { Timber.v("onTouchEvent $event $it") }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return super.onInterceptTouchEvent(ev)
            .also { Timber.v("onInterceptTouchEvent $ev $it") }
    }
}