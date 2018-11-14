package org.readium.r2.navigator.extensions

import android.app.Activity
import android.support.v4.view.ViewCompat

/** returns true if the resolved layout direction of the content view in this
 * activity is ViewCompat.LAYOUT_DIRECTION_RTL. Otherwise false. */
fun Activity.layoutDirectionIsRTL(): Boolean {
    return ViewCompat.getLayoutDirection(findViewById(android.R.id.content)) == ViewCompat.LAYOUT_DIRECTION_RTL
}