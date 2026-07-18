/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

/**
 * A multiplatform raster image.
 *
 * On Android it wraps an `android.graphics.Bitmap`, on iOS a `UIKit.UIImage`. Use the platform
 * accessor (`bitmap` / `uiImage`) to get the underlying platform image.
 */
public expect class ReadiumImage {

    /** Width of the image, in pixels. */
    public val width: Int

    /** Height of the image, in pixels. */
    public val height: Int
}

/**
 * A size in pixels, used to constrain image dimensions.
 */
public data class ImageSize(
    val width: Int,
    val height: Int,
)

/**
 * Returns this image scaled down to fit [maxSize], preserving its aspect ratio.
 *
 * Returns the receiver unchanged when it already fits.
 */
@InternalReadiumApi
public expect fun ReadiumImage.scaledToFit(maxSize: ImageSize): ReadiumImage
