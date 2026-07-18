/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import android.graphics.Bitmap
import android.util.Size
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.ImageSize

/**
 * Returns the publication cover as a [Bitmap] at its maximum size.
 */
public suspend fun Publication.coverAsBitmap(): Bitmap? =
    cover()?.bitmap

/**
 * Returns the publication cover as a [Bitmap], scaled down to fit the given [maxSize].
 */
public suspend fun Publication.coverFittingAsBitmap(maxSize: Size): Bitmap? =
    coverFitting(ImageSize(maxSize.width, maxSize.height))?.bitmap
