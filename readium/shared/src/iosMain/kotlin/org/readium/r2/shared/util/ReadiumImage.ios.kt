/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.readium.r2.shared.InternalReadiumApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage

/**
 * A multiplatform raster image, wrapping a [platform.UIKit.UIImage].
 */
public actual class ReadiumImage(
    /** The underlying platform image. */
    public val uiImage: UIImage,
) {

    // The dimensions are derived from `UIImage.size` (visual space, i.e. with the EXIF
    // orientation applied) instead of the underlying `CGImage` (storage space, pre-rotation),
    // so that scaling an EXIF-rotated photo preserves its visual aspect ratio.

    /** Width of the image, in pixels. */
    @OptIn(ExperimentalForeignApi::class)
    public actual val width: Int get() =
        uiImage.size.useContents { (width * uiImage.scale).roundToInt() }

    /** Height of the image, in pixels. */
    @OptIn(ExperimentalForeignApi::class)
    public actual val height: Int get() =
        uiImage.size.useContents { (height * uiImage.scale).roundToInt() }
}

@OptIn(ExperimentalForeignApi::class)
@InternalReadiumApi
public actual fun ReadiumImage.scaledToFit(maxSize: ImageSize): ReadiumImage {
    if (width <= maxSize.width && height <= maxSize.height) {
        return this
    }

    val ratio = min(
        maxSize.width / width.toDouble(),
        maxSize.height / height.toDouble()
    )
    val newWidth = (ratio * width).toInt().coerceAtLeast(1).toDouble()
    val newHeight = (ratio * height).toInt().coerceAtLeast(1).toDouble()

    val format = UIGraphicsImageRendererFormat.defaultFormat()
        .apply { scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(
        size = CGSizeMake(newWidth, newHeight),
        format = format
    )
    val image = renderer.imageWithActions {
        uiImage.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
    }
    return ReadiumImage(image)
}
