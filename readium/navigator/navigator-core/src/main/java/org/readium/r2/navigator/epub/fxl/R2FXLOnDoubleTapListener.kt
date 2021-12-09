/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.epub.fxl

class R2FXLOnDoubleTapListener(private var threeStep: Boolean) : R2FXLLayout.OnDoubleTapListener {

    override fun onDoubleTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
        try {
            if (threeStep) {
                threeStep(view, info.x, info.y)
            } else {
                twoStep(view, info.x, info.y)
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            // Can sometimes happen when getX() and getY() is called
        }

        return true
    }

    private fun twoStep(view: R2FXLLayout, x: Float, y: Float) {
        if (view.scale > view.minScale) {
            view.setScale(view.minScale, true)
        } else {
            view.setScale(view.maxScale, x, y, true)
        }
    }

    private fun threeStep(view: R2FXLLayout, x: Float, y: Float) {
        val scale = view.scale
        val medium = view.minScale + (view.maxScale - view.minScale) * 0.3f
        if (scale < medium) {
            view.setScale(medium, x, y, true)
        } else if (scale >= medium && scale < view.maxScale) {
            view.setScale(view.maxScale, x, y, true)
        } else {
            view.setScale(view.minScale, true)
        }
    }

}
