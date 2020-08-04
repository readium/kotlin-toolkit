/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import org.readium.r2.shared.extensions.scaleToFit
import org.readium.r2.shared.extensions.toPng
import org.readium.r2.shared.fetcher.BytesResource
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.LazyResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory

/**
 * Provides an easy access to a bitmap version of the publication cover.
 *
 * While at first glance, getting the cover could be seen as a helper, the implementation actually
 * depends on the publication format:

 * - Some might allow vector images or even HTML pages, in which case they need to be converted to
 *   bitmaps.
 * - Others require to render the cover from a specific file format, e.g. PDF.
 *
 * Furthermore, a reading app might want to use a custom strategy to choose the cover image, for
 * example by:
 *
 * - iterating through the images collection for a publication parsed from an OPDS 2 feed
 * - generating a bitmap from scratch using the publication's title
 * - using a cover selected by the user.
 */
interface CoverService : Publication.Service {

    /**
     * Returns the publication cover as a [Bitmap] at its maximum size.
     *
     * If the cover is not a bitmap format (e.g. SVG), it should be scaled down to fit the screen.
     */
    suspend fun cover(): Bitmap?

    /**
     *  Returns the publication cover as a [Bitmap], scaled down to fit the given [maxSize].
     */
    suspend fun coverFitting(maxSize: Size): Bitmap? = cover()?.scaleToFit(maxSize)
}

private suspend fun Publication.coverFromManifest(): Bitmap? {
    for (link in linksWithRel("cover")) {
        val data = get(link).read().getOrNull() ?: continue
        return BitmapFactory.decodeByteArray(data, 0, data.size) ?: continue
    }
    return null
}

/**
 * Returns the publication cover as a [Bitmap] at its maximum size.
 */
suspend fun Publication.cover(): Bitmap? {
        findService(CoverService::class)?.cover()?.let { return it }
        return coverFromManifest()
    }

/**
 * Returns the publication cover as a [Bitmap], scaled down to fit the given [maxSize].
 */
suspend fun Publication.coverFitting(maxSize: Size): Bitmap? {
    findService(CoverService::class)?.coverFitting(maxSize)?.let { return it }
    return coverFromManifest()?.scaleToFit(maxSize)
}

/** Factory to build a [CoverService]. */
var Publication.ServicesBuilder.coverServiceFactory: ServiceFactory?
    get() = get(CoverService::class)
    set(value) = set(CoverService::class, value)


/**
 * A [CoverService] which provides a unique cover for each Publication.
 */
abstract class GeneratedCoverService : CoverService {

    private val coverLink = Link(
        href = "/~readium/cover",
        type = "image/png",
        rels = setOf("cover")
    )

    override val links: List<Link> = listOf(coverLink)

    abstract override suspend fun cover(): Bitmap

    override fun get(link: Link): Resource? {
        if (link.href != coverLink.href)
            return null

        return LazyResource {
            val cover = cover()
            val png = cover.toPng()
            if (png == null) {
                val error = Exception("Unable to convert cover to PNG.")
                FailureResource(coverLink, error)
            } else {
                @Suppress("NAME_SHADOWING")
                val link = coverLink.copy(width = cover.width, height = cover.height)
                BytesResource(link, png)
            }

        }
    }

}

/**
 * A [CoverService] which uses a provided in-memory bitmap.
 */
class InMemoryCoverService internal constructor(private val cover: Bitmap) : GeneratedCoverService() {

    companion object {
        fun createFactory(cover: Bitmap?): ServiceFactory? = { cover?.let { InMemoryCoverService(it) } }
    }

    override suspend fun cover(): Bitmap = cover

}
