/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationObject
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

internal class ReadAloudPublication(
    val guidedNavigationTree: GuidedNavigationObject,
    val resources: List<Item>,
) {
    data class Item(
        val href: Url,
        val mediaType: MediaType?,
    )

    val mediaTypes = resources
        .mapNotNull { item -> item.mediaType?.let { item.href to it } }
        .associate { it }

    fun itemWithHref(href: Url): Item? =
        resources.firstOrNull { it.href == href }
}
