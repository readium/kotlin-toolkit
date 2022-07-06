/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content.iterators

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.SuspendingCloseable

/**
 * Iterates through a list of [Content] items asynchronously.
 */
@ExperimentalReadiumApi
interface ContentIterator : SuspendingCloseable {

    /**
     * Goes back to the previous item and returns it, or null if we reached the beginning.
     */
    suspend fun previous(): Content?

    /**
     * Advances to the next item and returns it, or null if we reached the end.
     */
    suspend fun next(): Content?
}
