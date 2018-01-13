package org.readium.r2.navigator.pager

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class R2ViewPager : ViewPager {

    private val TAG = this::class.java.simpleName

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}


    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        super.setCurrentItem(item, false)
    }

    override fun setCurrentItem(item: Int) {
        super.setCurrentItem(item, false)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }

}