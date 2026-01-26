/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationDocument
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError

/**
 * Provides a list of Guided Navigation documents for a Publication.
 */
@ExperimentalReadiumApi
public interface GuidedNavigationService : Publication.Service {

    public fun iterator(): GuidedNavigationIterator
}

/**
 * Iterator providing access to all guided navigation documents of a Publication.
 */
@ExperimentalReadiumApi
public interface GuidedNavigationIterator : Closeable {

    /**
     * Prepares the next guided navigation document for retrieval by the invocation of next.
     *
     * Does nothing if the the end has been reached.
     */
    public suspend operator fun hasNext(): Boolean

    /**
     * Retrieves the next guided navigation document, prepared by the preceding call to [hasNext],
     * or throws an IllegalStateException if hasNext was not invoked.
     */
    public suspend operator fun next(): Try<GuidedNavigationDocument, ReadError>

    /**
     * Closes any resources allocated by the iterator.
     */
    override fun close() {}
}

/**
 * Returns an iterator providing access to all the guided navigation documents of the publication.
 */
@ExperimentalReadiumApi
public fun Publication.guidedNavigationIterator(): GuidedNavigationIterator? =
    guidedNavigationService?.iterator()

@OptIn(ExperimentalReadiumApi::class)
private val PublicationServicesHolder.guidedNavigationService: GuidedNavigationService?
    get() {
        findService(GuidedNavigationService::class)?.let { return it }
        return null
    }
