/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import android.app.Activity
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat

// Using ViewCompat and WindowInsetsCompat does not work properly in all versions of Android
@Suppress("DEPRECATION")
/** Returns `true` if fullscreen or immersive mode is not set. */
private fun Activity.isSystemUiVisible(): Boolean {
    return this.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
}

// Using ViewCompat and WindowInsetsCompat does not work properly in all versions of Android
@Suppress("DEPRECATION")
/** Enable fullscreen or immersive mode. */
fun Activity.hideSystemUi() {
    this.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
}

// Using ViewCompat and WindowInsetsCompat does not work properly in all versions of Android
@Suppress("DEPRECATION")
/** Disable fullscreen or immersive mode. */
fun Activity.showSystemUi() {
    this.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
}

/** Toggle fullscreen or immersive mode. */
fun Activity.toggleSystemUi() {
    if (this.isSystemUiVisible()) {
        this.hideSystemUi()
    } else {
        this.showSystemUi()
    }
}

/** Set padding around view so that content doesn't overlap system UI */
fun View.padSystemUi(insets: WindowInsets, activity: Activity) =
    WindowInsetsCompat.toWindowInsetsCompat(insets, this)
        .getInsets(WindowInsetsCompat.Type.statusBars()).apply {
            setPadding(
                left,
                top + (activity as AppCompatActivity).supportActionBar!!.height,
                right,
                bottom
            )
        }

/** Clear padding around view */
fun View.clearPadding() =
    setPadding(0, 0, 0, 0)
