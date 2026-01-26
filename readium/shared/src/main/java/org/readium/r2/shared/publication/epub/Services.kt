/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.epub

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.services.GuidedNavigationIterator
import org.readium.r2.shared.publication.services.GuidedNavigationService

/**
 * Provides a list of guided navigation documents mimicking media overlays available in Publication.
 */
@ExperimentalReadiumApi
public interface MediaOverlaysService : GuidedNavigationService

/**
 * Returns an iterator providing access to all the guided navigation documents mimicking media
 * overlays of the publication.
 */
@ExperimentalReadiumApi
public fun Publication.mediaOverlaysIterator(): GuidedNavigationIterator? =
    mediaOverlaysService?.iterator()

@ExperimentalReadiumApi
private val PublicationServicesHolder.mediaOverlaysService: GuidedNavigationService?
    get() {
        findService(MediaOverlaysService::class)?.let { return it }
        return null
    }
