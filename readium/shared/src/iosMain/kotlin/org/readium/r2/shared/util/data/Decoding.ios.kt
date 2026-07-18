/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import kotlin.math.max
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.ImageSize
import org.readium.r2.shared.util.ReadiumImage
import org.readium.r2.shared.util.scaledToFit
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreGraphics.CGImageRelease
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class, InternalReadiumApi::class)
internal actual fun decodeImageBlocking(bytes: ByteArray, maxSize: ImageSize?): ReadiumImage? {
    if (bytes.isEmpty()) {
        return null
    }

    if (maxSize == null) {
        val data = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        return UIImage.imageWithData(data)?.let { ReadiumImage(it) }
    }

    // Decode a downscaled image directly with an ImageIO thumbnail, bounded by the largest
    // dimension of the box, then fit it exactly.
    val cgImage = bytes.usePinned { pinned ->
        val cfData = CFDataCreate(
            kCFAllocatorDefault,
            pinned.addressOf(0).reinterpret(),
            bytes.size.toLong()
        ) ?: return null

        try {
            val source = CGImageSourceCreateWithData(cfData, null)
                ?: return@usePinned null

            try {
                memScoped {
                    val options = CFDictionaryCreateMutable(
                        kCFAllocatorDefault,
                        3,
                        kCFTypeDictionaryKeyCallBacks.ptr,
                        kCFTypeDictionaryValueCallBacks.ptr
                    ) ?: return@memScoped null

                    try {
                        val maxPixelSizeVar = alloc<IntVar>()
                        maxPixelSizeVar.value = max(maxSize.width, maxSize.height)
                        val maxPixelSize = CFNumberCreate(
                            kCFAllocatorDefault,
                            kCFNumberIntType,
                            maxPixelSizeVar.ptr
                        ) ?: return@memScoped null

                        try {
                            CFDictionarySetValue(
                                options,
                                kCGImageSourceCreateThumbnailFromImageAlways,
                                kCFBooleanTrue
                            )
                            CFDictionarySetValue(
                                options,
                                kCGImageSourceCreateThumbnailWithTransform,
                                kCFBooleanTrue
                            )
                            CFDictionarySetValue(
                                options,
                                kCGImageSourceThumbnailMaxPixelSize,
                                maxPixelSize
                            )

                            CGImageSourceCreateThumbnailAtIndex(source, 0u, options)
                        } finally {
                            CFRelease(maxPixelSize)
                        }
                    } finally {
                        CFRelease(options)
                    }
                }
            } finally {
                CFRelease(source)
            }
        } finally {
            CFRelease(cfData)
        }
    } ?: return null

    val image = try {
        UIImage.imageWithCGImage(cgImage)
    } finally {
        CGImageRelease(cgImage)
    }

    return ReadiumImage(image).scaledToFit(maxSize)
}
