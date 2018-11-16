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
import android.view.*
import android.webkit.WebView
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Locations


/**
 * Created by Aferdita Muriqi on 12/2/17.
 */

open class R2BasicWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {

    lateinit var activity: R2EpubActivity
    var progression: Double = 0.0

    @android.webkit.JavascriptInterface
    open fun scrollRight() {
        activity.runOnUiThread {
            if (activity.supportActionBar!!.isShowing) {
                activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            val scrollMode = activity.preferences.getBoolean("scroll", false)
            if (scrollMode) {
                if (activity.publication.metadata.direction == "rtl") {
                    this.evaluateJavascript("scrollRightRTL();") { result ->
                        if (result.contains("edge")) {
                            activity.previousResource(false)
                        }
                    }
                } else {
                    activity.nextResource(false)
                }
            } else {
                if (!this.canScrollHorizontally(1)) {
                    activity.nextResource(false)
                }
                this.evaluateJavascript("scrollRight();", null)
            }
        }
    }

    @android.webkit.JavascriptInterface
    open fun scrollLeft() {
        activity.runOnUiThread {
            if (activity.supportActionBar!!.isShowing) {
                activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            val scrollMode = activity.preferences.getBoolean("scroll", false)
            if (scrollMode) {
                if (activity.publication.metadata.direction == "rtl") {
                    this.evaluateJavascript("scrollLeftRTL();") { result ->
                        if (result.contains("edge")) {
                            activity.nextResource(false)
                        }
                    }
                } else {
                    activity.previousResource(false)
                }
            } else {
                // fix this for when vertical scrolling is enabled
                if (!this.canScrollHorizontally(-1)) {
                    activity.previousResource(false)
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
    fun progressionDidChange(positionString: String) {
        progression = positionString.toDouble()
        activity.storeProgression(Locations(progression = progression))
    }

    @android.webkit.JavascriptInterface
    fun centerTapped() {
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