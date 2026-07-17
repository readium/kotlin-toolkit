/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container

/**
 * Implements the transformation of a Resource. It can be used, for example, to decrypt,
 * deobfuscate, inject CSS or JavaScript, correct content – e.g. adding a missing dir="rtl" in an
 * HTML document, pre-process – e.g. before indexing a publication's content, etc.
 *
 * If the transformation doesn't apply, simply return the resource unchanged.
 */
public typealias EntryTransformer = (Url, Resource) -> Resource

/**
 * Transforms the resources' content of a child fetcher using a list of [EntryTransformer]
 * functions.
 */
public class TransformingContainer(
    private val container: Container<Resource>,
    private val transformers: List<EntryTransformer>,
) : Container<Resource> {

    public constructor(container: Container<Resource>, transformer: EntryTransformer) :
        this(container, listOf(transformer))

    override val entries: Set<Url> =
        container.entries

    override fun get(url: Url): Resource? {
        val originalResource = container[url]
            ?: return null

        return transformers
            .fold(originalResource) { acc: Resource, transformer: EntryTransformer ->
                transformer(url, acc)
            }
    }

    override fun close() {
        container.close()
    }
}
