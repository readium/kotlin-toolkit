/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Url

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
    private val container: Container,
    private val transformers: List<ResourceTransformer>
) : Container {

    public constructor(fetcher: Container, transformer: ResourceTransformer) :
        this(fetcher, listOf(transformer))

    override suspend fun entries(): Set<Container.Entry>? =
        container.entries()

    override fun get(url: Url): Container.Entry =
        transformers
            .fold(container.get(url) as Resource) { acc, transformer ->
                transformer(acc)
            }
            .toEntry(url)

    override suspend fun close() {
        container.close()
    }
}
