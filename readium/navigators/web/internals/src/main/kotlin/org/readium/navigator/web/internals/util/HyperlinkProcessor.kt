/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.web.internals.util

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.readium.navigator.common.FootnoteContext
import org.readium.navigator.common.LinkContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.decodeString
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use

@OptIn(ExperimentalReadiumApi::class)
public class HyperlinkProcessor(
    private val container: Container<Resource>,
) {

    public suspend fun computeLinkContext(originUrl: Url, outerHtml: String): LinkContext? {
        val href = tryOrNull { Jsoup.parse(outerHtml) }
            ?.select("a[epub:type=noteref]")?.first()
            ?.attr("href")
            ?.let { Url(it) }
            ?: return null

        val id = href.fragment ?: return null

        val absoluteUrl = originUrl.resolve(href)

        val absoluteUrlWithoutFragment = absoluteUrl.removeFragment()

        val aside =
            tryOrLog {
                container[absoluteUrlWithoutFragment]
                    ?.use { res ->
                        res.read()
                            .flatMap { it.decodeString() }
                            .map { Jsoup.parse(it) }
                            .getOrNull()
                    }?.select("#$id")
                    ?.first()?.html()
            }?.takeIf { it.isNotBlank() }
                ?: return null

        val safe = Jsoup.clean(aside, Safelist.relaxed())
        return FootnoteContext(
            noteContent = safe
        )
    }
}
