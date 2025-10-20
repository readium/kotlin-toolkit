package org.readium.r2.navigator.extensions

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

/**
 * Extensions
 */

/** returns true if the resolved layout direction of the content view in this
 * activity is ViewCompat.LAYOUT_DIRECTION_RTL. Otherwise false. */
internal fun Activity.layoutDirectionIsRTL(): Boolean {
    return findViewById<View>(android.R.id.content).layoutDirection == View.LAYOUT_DIRECTION_RTL
}

@ColorInt
internal fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}
