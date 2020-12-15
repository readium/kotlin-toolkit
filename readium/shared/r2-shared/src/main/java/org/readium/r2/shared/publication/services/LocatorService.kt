/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.ServiceFactory

/**
 * Locates the destination of various sources (e.g. locators, progression, etc.) in the
 * publication.
 *
 * This service can be used to implement a variety of features, such as:
 *   - Jumping to a given position or total progression, by converting it first to a [Locator].
 *   - Converting a [Locator] which was created from an alternate manifest with a different reading
 *     order. For example, when downloading a streamed manifest or offloading a package.
 */
interface LocatorService : Publication.Service {

    /** Locates the target of the given [locator]. */
    fun locate(locator: Locator): Locator?

    /** Locates the target at the given [progression] relative to the whole publication. */
    fun locate(progression: Double): Locator?

}


/** Locates the target of the given [locator]. */
fun Publication.locate(locator: Locator): Locator? =
    findService(LocatorService::class)?.locate(locator)

/** Locates the target at the given [progression] relative to the whole publication. */
fun Publication.locate(progression: Double): Locator? =
    findService(LocatorService::class)?.locate(progression)


/** Factory to build a [LocatorService] */
var Publication.ServicesBuilder.locatorServiceFactory: ServiceFactory?
    get() = get(LocatorService::class)
    set(value) = set(LocatorService::class, value)


open class DefaultLocatorService(val readingOrder: List<Link>) : LocatorService {

    override fun locate(locator: Locator): Locator? =
        locator.takeIf { readingOrder.firstWithHref(locator.href) != null }

    override fun locate(progression: Double): Locator? = null

}
