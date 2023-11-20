/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.Url

/**
 * Routes requests to child containers, depending on a provided predicate.
 *
 * This can be used for example to serve a publication containing both local and remote resources,
 * and more generally to concatenate different content sources.
 *
 * The [containers] will be tested in the given order.
 */
public class CompositeContainer<E : Blob>(
    private val containers: List<Container<E>>
) : Container<E> {

    public constructor(vararg containers: Container<E>) :
        this(containers.toList())

    override val entries: Set<Url> =
        containers.fold(emptySet()) { acc, container -> acc + container.entries }

    override fun get(url: Url): E? =
        containers.firstNotNullOfOrNull { it[url] }

    override suspend fun close() {
        containers.forEach { it.close() }
    }
}
