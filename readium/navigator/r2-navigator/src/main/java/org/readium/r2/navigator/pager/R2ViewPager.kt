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
import android.util.AttributeSet
import android.view.MotionEvent

class R2ViewPager : R2RTLViewPager {


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        super.setCurrentItem(item, smoothScroll)
    }

    override fun setCurrentItem(item: Int) {
        super.setCurrentItem(item, false)
    }


    private var disable: Boolean = true

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (disable) false else super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (disable) false else super.onTouchEvent(event)
    }

    fun disableScroll(disable: Boolean) {
        this.disable = disable
    }
}