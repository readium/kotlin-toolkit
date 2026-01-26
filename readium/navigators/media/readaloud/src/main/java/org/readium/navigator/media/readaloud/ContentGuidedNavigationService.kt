/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationDocument
import org.readium.r2.shared.guided.GuidedNavigationObject
import org.readium.r2.shared.guided.GuidedNavigationText
import org.readium.r2.shared.guided.GuidedNavigationTextRef
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.services.GuidedNavigationIterator
import org.readium.r2.shared.publication.services.GuidedNavigationService
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError

internal class ContentGuidedNavigationService(
    private val contentService: ContentService,
) : GuidedNavigationService {

    override fun iterator(): GuidedNavigationIterator {
        val contentIterator = contentService.content(start = null).iterator()
        return Iterator(contentIterator)
    }

    private class Iterator(
        private val contentIterator: Content.Iterator,
    ) : GuidedNavigationIterator {

        private var guidedNavigationDocument: GuidedNavigationDocument? = null

        private var ended: Boolean = false

        override suspend fun hasNext(): Boolean {
            if (ended) {
                return false
            }

            guidedNavigationDocument = createGuidedNavigationDocument()
            ended = true
            return true
        }

        override suspend fun next(): Try<GuidedNavigationDocument, ReadError> {
            val res = checkNotNull(guidedNavigationDocument)
            return Try.success(res)
        }

        private suspend fun createGuidedNavigationDocument(): GuidedNavigationDocument {
            val tree = mutableListOf<GuidedNavigationObject>()

            while (contentIterator.hasNext()) {
                val nodes = when (val element = contentIterator.next()) {
                    is Content.TextElement -> {
                        element.segments.mapNotNull { segment ->
                            if (segment.text.isBlank()) {
                                return@mapNotNull null
                            }

                            GuidedNavigationObject(
                                refs = setOfNotNull(segment.locator.toTextRef()),
                                text = GuidedNavigationText(
                                    plain = segment.text,
                                    ssml = null,
                                    language = segment.language
                                ),
                            )
                        }
                    }

                    is Content.TextualElement -> {
                        listOfNotNull(
                            element.text
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    GuidedNavigationObject(
                                        refs = setOfNotNull(element.locator.toTextRef()),
                                        text = GuidedNavigationText(it)
                                    )
                                }
                        )
                    }

                    else -> emptyList()
                }

                if (nodes.isNotEmpty()) {
                    tree.add(
                        GuidedNavigationObject(
                            children = nodes
                        )
                    )
                }
            }

            return GuidedNavigationDocument(guided = tree)
        }

        private fun Locator.toTextRef(): GuidedNavigationTextRef? {
            val htmlId = locations.cssSelector
                ?.takeIf { it.startsWith("#") }
                .orEmpty()
            val url = Url("$href$htmlId")
            return url?.let { GuidedNavigationTextRef(it) }
        }
    }
}
