/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.graphics.Bitmap
import android.util.Size
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.scaleToFit

/**
 * A multiplatform raster image, wrapping an [android.graphics.Bitmap].
 */
public actual class ReadiumImage(
    /** The underlying platform image. */
    public val bitmap: Bitmap,
) {

    /** Width of the image, in pixels. */
    public actual val width: Int get() = bitmap.width

    /** Height of the image, in pixels. */
    public actual val height: Int get() = bitmap.height
}

@InternalReadiumApi
public actual fun ReadiumImage.scaledToFit(maxSize: ImageSize): ReadiumImage {
    val scaled = bitmap.scaleToFit(Size(maxSize.width, maxSize.height))
    return if (scaled === bitmap) this else ReadiumImage(scaled)
}
