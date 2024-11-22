/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.epub.fxl

import android.content.Context
import android.widget.OverScroller

internal abstract class R2FXLScroller {

    abstract val isFinished: Boolean
    abstract val currX: Int
    abstract val currY: Int
    abstract fun computeScrollOffset(): Boolean
    abstract fun fling(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        overX: Int,
        overY: Int,
    )
    abstract fun forceFinished(finished: Boolean)

    private class Scroller internal constructor(context: Context) : R2FXLScroller() {

        internal var scroller: OverScroller = OverScroller(context)

        override val isFinished: Boolean
            get() = scroller.isFinished

        override val currX: Int
            get() = scroller.currX

        override val currY: Int
            get() = scroller.currY

        override fun computeScrollOffset(): Boolean {
            return scroller.computeScrollOffset()
        }

        override fun fling(
            startX: Int,
            startY: Int,
            velocityX: Int,
            velocityY: Int,
            minX: Int,
            maxX: Int,
            minY: Int,
            maxY: Int,
            overX: Int,
            overY: Int,
        ) {
            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        }

        override fun forceFinished(finished: Boolean) {
            scroller.forceFinished(finished)
        }
    }

    companion object {
        fun getScroller(context: Context): R2FXLScroller {
            return Scroller(context)
        }
    }
}
