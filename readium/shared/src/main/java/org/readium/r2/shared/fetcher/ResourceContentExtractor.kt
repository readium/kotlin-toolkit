/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.Search
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.readAsString
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Extracts pure content from a marked-up (e.g. HTML) or binary (e.g. PDF) resource.
 */
@ExperimentalReadiumApi
public interface ResourceContentExtractor {

    /**
     * Extracts the text content of the given [resource].
     */
    public suspend fun extractText(resource: Fetcher.Resource): ResourceTry<String> = Try.success("")

    public interface Factory {
        /**
         * Creates a [ResourceContentExtractor] instance for the given [resource].
         *
         * Return null if the resource format is not supported.
         */
        public suspend fun createExtractor(resource: Fetcher.Resource): ResourceContentExtractor?
    }
}

@ExperimentalReadiumApi
public class DefaultResourceContentExtractorFactory : ResourceContentExtractor.Factory {

    override suspend fun createExtractor(resource: Fetcher.Resource): ResourceContentExtractor? =
        when (resource.link().mediaType) {
            MediaType.HTML, MediaType.XHTML -> HtmlResourceContentExtractor()
            else -> null
        }
}

/**
 * [ResourceContentExtractor] implementation for HTML resources.
 */
@ExperimentalReadiumApi
public class HtmlResourceContentExtractor : ResourceContentExtractor {

    override suspend fun extractText(resource: Fetcher.Resource): ResourceTry<String> = withContext(Dispatchers.IO) {
        resource.readAsString().mapCatching { html ->
            val body = Jsoup.parse(html).body().text()
            // Transform HTML entities into their actual characters.
            Parser.unescapeEntities(body, false)
        }
    }
}
