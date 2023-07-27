/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Resource

/**
 * Implements the transformation of a Resource. It can be used, for example, to decrypt,
 * deobfuscate, inject CSS or JavaScript, correct content – e.g. adding a missing dir="rtl" in an
 * HTML document, pre-process – e.g. before indexing a publication's content, etc.
 *
 * If the transformation doesn't apply, simply return resource unchanged.
 */
public typealias ResourceTransformer = (Resource) -> Resource

/**
 * Transforms the resources' content of a child fetcher using a list of [ResourceTransformer]
 * functions.
 */
public class TransformingFetcher(
    private val fetcher: Fetcher,
    private val transformers: List<ResourceTransformer>
) : Fetcher {

    public constructor(fetcher: Fetcher, transformer: ResourceTransformer) :
        this(fetcher, listOf(transformer))

    override suspend fun links(): List<Link> = fetcher.links()

    override fun get(link: Link): Resource {
        val resource = fetcher.get(link)
        return transformers.fold(resource) { acc, transformer ->
            transformer(acc)
        }
    }

    override suspend fun close() {
        fetcher.close()
    }
}
