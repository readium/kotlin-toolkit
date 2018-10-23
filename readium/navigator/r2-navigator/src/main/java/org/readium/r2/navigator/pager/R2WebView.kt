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
import android.view.View
import android.webkit.WebView
import org.readium.r2.navigator.R2EpubActivity
import timber.log.Timber
import android.view.GestureDetector
import android.view.MotionEvent
import org.readium.r2.navigator.SCROLL_REF


/**
 * Created by Aferdita Muriqi on 12/2/17.
 */

class R2WebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {

    lateinit var activity: R2EpubActivity

    private var gestureDetector: GestureDetector? = null
    var progression: Double = 0.0
    private var mIsScrolling = false
    private var scrollRight = false

    /*
     * @see android.webkit.WebView#onScrollChanged(int, int, int, int)
     */
    override fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
        val scrollMode = activity.preferences.getBoolean(SCROLL_REF, false)
        if (!scrollMode) {
            if (Math.abs(x - oldX) > 1) {
                mIsScrolling = true
                if (x - oldX > 1) {
                    scrollRight = true
                } else if (oldX - x > 1) {
                    scrollRight = false
                }
            } else {
                mIsScrolling = false
                if (scrollRight) {
                    scrollRight()
                } else {
                    scrollLeft()
                }
            }
        }
    }

    /*
     * @see android.webkit.WebView#onTouchEvent(android.view.MotionEvent)
     */
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector!!.onTouchEvent(ev) || super.onTouchEvent(ev)
    }


    fun setGestureDetector(gestureDetector: GestureDetector) {
        this.gestureDetector = gestureDetector
    }

    @android.webkit.JavascriptInterface
    fun scrollRight() {
        activity.runOnUiThread {
            if (activity.supportActionBar!!.isShowing) {
                activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            val scrollMode = activity.preferences.getBoolean(SCROLL_REF, false)
            if (scrollMode) {
                if (activity.publication.metadata.direction == "rtl") {
                    this.evaluateJavascript("scrollRightRTL();") { result ->
                        if (result.contains("edge")) {
                            activity.previousResource()
                        }
                    }
                } else {
                    activity.nextResource()
                }
            } else {
                if (!this.canScrollHorizontally(1)) {
                    activity.nextResource()
                }
                this.evaluateJavascript("scrollRight();", null)
            }
        }
    }

    @android.webkit.JavascriptInterface
    fun scrollLeft() {
        activity.runOnUiThread {
            if (activity.supportActionBar!!.isShowing) {
                activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            val scrollMode = activity.preferences.getBoolean(SCROLL_REF, false)
            if (scrollMode) {
                if (activity.publication.metadata.direction == "rtl") {
                    this.evaluateJavascript("scrollLeftRTL();") { result ->
                        if (result.contains("edge")) {
                            activity.nextResource()
                        }
                    }
                } else {
                    activity.previousResource()
                }
            } else {
                // fix this for when vertical scrolling is enabled
                if (!this.canScrollHorizontally(-1)) {
                    activity.previousResource()
                }
                this.evaluateJavascript("scrollLeft();", null)
            }
        }
    }

    @android.webkit.JavascriptInterface
    fun scrollToPosition(progression: Double) {
        this.evaluateJavascript("scrollToPosition(\"$progression\", \"${activity.publication.metadata.direction}\");", null)
    }


    @android.webkit.JavascriptInterface
    fun progressionDidChange(body: String) {
        progression = body.toDouble()
        Timber.d("progression: $progression")
        activity.storeProgression(progression)
    }

    @android.webkit.JavascriptInterface
    fun CenterTapped() {
        activity.toggleActionBar()
    }

    fun hide() {
        activity.runOnUiThread {
            activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
        }
    }

    fun show() {
        activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    }

    @android.webkit.JavascriptInterface
    fun setProperty(key: String, value: String) {
        activity.runOnUiThread {
            this.evaluateJavascript("setProperty(\"$key\", \"$value\");", null)
        }
    }

    @android.webkit.JavascriptInterface
    fun removeProperty(key: String) {
        activity.runOnUiThread {
            this.evaluateJavascript("removeProperty(\"$key\");", null)
        }
    }

}