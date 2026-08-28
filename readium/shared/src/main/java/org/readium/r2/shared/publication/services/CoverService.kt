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
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.scaleToFit
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.publication.firstWithRel
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.decodeBitmap
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.resource.Resource

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
public interface CoverService : Publication.Service {

    /**
     * Returns the publication cover as a [Bitmap] at its maximum size.
     *
     * If the cover is not a bitmap format (e.g. SVG), it should be scaled down to fit the screen.
     */
    public suspend fun cover(): Bitmap?

    /**
     *  Returns the publication cover as a [Bitmap], scaled down to fit the given [maxSize].
     */
    public suspend fun coverFitting(maxSize: Size): Bitmap? = cover()?.scaleToFit(maxSize)
}

/**
 * Returns the publication cover as a [Bitmap] at its maximum size.
 */
public suspend fun Publication.cover(): Bitmap? =
    findService(CoverService::class)?.cover()?.let { return it }

/**
 * Returns the publication cover as a [Bitmap], scaled down to fit the given [maxSize].
 */
public suspend fun Publication.coverFitting(maxSize: Size): Bitmap? =
    findService(CoverService::class)?.coverFitting(maxSize)?.let { return it }

/** Factory to build a [CoverService]. */
public var Publication.ServicesBuilder.coverServiceFactory: ServiceFactory?
    get() = get(CoverService::class)
    set(value) = set(CoverService::class, value)

internal class ResourceCoverService(
    private val coverUrl: Url,
    private val container: Container<Resource>,
) : CoverService {

    override suspend fun cover(): Bitmap? =
        withContext(Dispatchers.IO) {
            val resource = container[coverUrl]
                ?: return@withContext null

            return@withContext resource
                .read()
                .flatMap { it.decodeBitmap() }
                .getOrNull()
        }

    companion object {

        fun createFactory(): (Publication.Service.Context) -> ResourceCoverService? = {
            val publicationContent: List<Link> =
                it.manifest.resources + it.manifest.readingOrder

            publicationContent
                .firstWithRel("cover")
                ?.url()
                ?.let { url -> ResourceCoverService(url, it.container) }
        }
    }
}

/**
 * A [CoverService] which uses a provided in-memory bitmap.
 */
public class InMemoryCoverService internal constructor(private val cover: Bitmap) : CoverService {

    public companion object {
        public fun createFactory(cover: Bitmap?): ServiceFactory = {
            cover?.let { InMemoryCoverService(it) }
        }
    }

    public override suspend fun cover(): Bitmap = cover
}
