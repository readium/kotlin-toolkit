/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.data

import android.graphics.BitmapFactory
import java.nio.charset.Charset
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ImageSize
import org.readium.r2.shared.util.ReadiumImage
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.scaledToFit

/**
 * Content as plain text, decoded with the given [charset].
 */
public suspend fun ByteArray.decodeString(
    charset: Charset,
): Try<String, DecodeError> =
    decode(
        { String(it, charset = charset) },
        { DebugError("Content is not a valid $charset string.", ThrowableError(it)) }
    )

internal actual fun decodeImageBlocking(bytes: ByteArray, maxSize: ImageSize?): ReadiumImage? {
    if (maxSize == null) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?.let { ReadiumImage(it) }
    }

    // Read the image bounds without decoding it, to compute the subsampling factor.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    // Largest power of 2 keeping both dimensions equal or larger than the requested size.
    var sampleSize = 1
    while (
        bounds.outWidth / (sampleSize * 2) >= maxSize.width &&
        bounds.outHeight / (sampleSize * 2) >= maxSize.height
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?.let { ReadiumImage(it).scaledToFit(maxSize) }
}
