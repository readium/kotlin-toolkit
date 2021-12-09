package org.readium.r2.navigator.extensions

import android.app.Activity
import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat

/**
 * Extensions
 */


/** returns true if the resolved layout direction of the content view in this
 * activity is ViewCompat.LAYOUT_DIRECTION_RTL. Otherwise false. */
fun Activity.layoutDirectionIsRTL(): Boolean {
    return ViewCompat.getLayoutDirection(findViewById(android.R.id.content)) == ViewCompat.LAYOUT_DIRECTION_RTL
}


@ColorInt
fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}