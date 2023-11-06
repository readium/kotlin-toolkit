/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ClosedContainer

/**
 * Implements the transformation of a Resource. It can be used, for example, to decrypt,
 * deobfuscate, inject CSS or JavaScript, correct content – e.g. adding a missing dir="rtl" in an
 * HTML document, pre-process – e.g. before indexing a publication's content, etc.
 *
 * If the transformation doesn't apply, simply return the resource unchanged.
 */
public typealias ResourceTransformer = (Resource) -> Resource

/**
 * Transforms the resources' content of a child fetcher using a list of [ResourceTransformer]
 * functions.
 */
public class TransformingContainer(
    private val container: ClosedContainer<ResourceEntry>,
    private val transformers: List<ResourceTransformer>
) : ClosedContainer<ResourceEntry> {

    public constructor(container: ClosedContainer<ResourceEntry>, transformer: ResourceTransformer) :
        this(container, listOf(transformer))

    override suspend fun entries(): Set<Url> =
        container.entries()

    override fun get(url: Url): ResourceEntry? {
        val originalResource = container.get(url)
            ?: return null

        return transformers
            .fold(originalResource) { acc: Resource, transformer: ResourceTransformer ->
                transformer(acc)
            }.toResourceEntry(url)
    }

    override suspend fun close() {
        container.close()
    }
}
