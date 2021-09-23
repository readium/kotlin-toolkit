/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import android.graphics.Bitmap
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min


/**
 * Resizes a bitmap to fit [maxSize] with bilinear filtering.
 */
internal fun Bitmap.scaleToFit(maxSize: Size): Bitmap {
    if (width <= maxSize.width && height <= maxSize.height)
        return this

    val ratio = min(
        maxSize.width / width.toFloat(),
        maxSize.height / height.toFloat()
    )

    val newWidth = (ratio * width).toInt()
    val newHeight = (ratio * height).toInt()

    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

internal val Bitmap.size get() = Size(width, height)

suspend fun Bitmap.toPng(quality: Int = 100): ByteArray? = withContext(Dispatchers.Default) {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, quality, stream).let{
        if (it) stream.toByteArray() else null
    }
}
