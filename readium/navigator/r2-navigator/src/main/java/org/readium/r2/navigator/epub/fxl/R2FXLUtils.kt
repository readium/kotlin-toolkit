/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.epub.fxl

import android.graphics.Rect
import android.graphics.RectF

object R2FXLUtils {

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param array the array to read the values from
     */
    fun setRect(rect: Rect, array: FloatArray) {
        setRect(rect, array[0], array[1], array[2], array[3])
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param array the array to read the values from
     */
    fun setRect(rect: RectF, array: FloatArray) {
        setRect(rect, array[0], array[1], array[2], array[3])
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param left left
     * @param top top
     * @param right right
     * @param bottom bottom
     */
    fun setRect(rect: RectF, left: Float, top: Float, right: Float, bottom: Float) {
        rect.set(Math.round(left).toFloat(), Math.round(top).toFloat(), Math.round(right).toFloat(), Math.round(bottom).toFloat())
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param left left
     * @param top top
     * @param right right
     * @param bottom bottom
     */
    private fun setRect(rect: Rect, left: Float, top: Float, right: Float, bottom: Float) {
        rect.set(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom))
    }

    fun setArray(array: FloatArray, rect: Rect) {
        array[0] = rect.left.toFloat()
        array[1] = rect.top.toFloat()
        array[2] = rect.right.toFloat()
        array[3] = rect.bottom.toFloat()
    }

    fun setArray(array: FloatArray, rect: RectF) {
        array[0] = rect.left
        array[1] = rect.top
        array[2] = rect.right
        array[3] = rect.bottom
    }

}
