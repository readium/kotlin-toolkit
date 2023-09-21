/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

/**
 * Returns a copy of the receiver after normalizing its HREFs to the link with `rel="self"`.
 */
@ExperimentalReadiumApi
public fun Manifest.normalizeHrefsToSelf(): Manifest {
    val base = linkWithRel("self")?.href?.resolve()
        ?: return this

    return normalizeHrefsToBase(base)
}

/**
 * Returns a copy of the receiver after normalizing its HREFs to the given [baseUrl].
 */
@ExperimentalReadiumApi
public fun Manifest.normalizeHrefsToBase(baseUrl: Url): Manifest {
    return copy(HrefNormalizer(baseUrl))
}

/**
 * Returns a copy of the receiver after normalizing its HREFs to the given [baseUrl].
 */
@ExperimentalReadiumApi
public fun Link.normalizeHrefsToBase(baseUrl: Url?): Link {
    baseUrl ?: return this
    return copy(HrefNormalizer(baseUrl))
}

@OptIn(ExperimentalReadiumApi::class)
private class HrefNormalizer(private val baseUrl: Url) : ManifestTransformer {
    override fun transform(href: Href): Href =
        href.resolveTo(baseUrl)
}
