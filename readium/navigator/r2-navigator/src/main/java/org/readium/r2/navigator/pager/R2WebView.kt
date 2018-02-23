package org.readium.r2.navigator.pager

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import org.readium.r2.navigator.R2EpubActivity


/**
 * Created by aferditamuriqi on 12/2/17.
 */

class R2WebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {

    private val TAG = this::class.java.simpleName

    lateinit var activity: R2EpubActivity

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

            if (activity.userSettings.verticalScroll.equals(Scroll.On.toString())) {
                if (!this.canScrollVertically(1)) {
                    activity.nextResource()
                }
            } else {

                if (!this.canScrollHorizontally(1)) {
                    activity.nextResource()
                } else {
                    this.evaluateJavascript("scrollRight();", null)
                }
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

            if (activity.userSettings.verticalScroll.equals(Scroll.On.toString())) {
                if (!this.canScrollVertically(-1)) {
                    activity.previousResource()
                }
            } else {
                // fix this for when vertical scrolling is enabled
                if (!this.canScrollHorizontally(-1)) {
                    activity.previousResource()
                } else {
                    this.evaluateJavascript("scrollLeft();", null)
                }
            }
        }
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