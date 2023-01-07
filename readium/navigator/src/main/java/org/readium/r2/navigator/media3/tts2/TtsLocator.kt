/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.indexOfFirstWithHref

@ExperimentalReadiumApi
@Serializable
data class TtsLocator(
    val resourceIndex: Int,
    val text: String,
    val textBefore: String?,
    val textAfter: String?,
    val cssSelector: String?,
) : MediaNavigatorInternal.Locator

@ExperimentalReadiumApi
internal fun TtsLocator.toLocator(publication: Publication): Locator {
    return publication
        .locatorFromLink(publication.readingOrder[resourceIndex])!!
        .copyWithLocations(
            progression = null,
            otherLocations = buildMap {
                cssSelector?.let { put("cssSelector", it) }
            }
        ).copy(
            text =
            Locator.Text(
                highlight = text,
                before = textBefore,
                after = textAfter
            )
        )
}

@ExperimentalReadiumApi
internal fun Locator.toTtsLocator(publication: Publication): TtsLocator? {
    val resourceIndex = publication.readingOrder.indexOfFirstWithHref(href)
        ?: return null

    val contentText = text.highlight
        ?: return null

    return TtsLocator(
        resourceIndex = resourceIndex,
        text = contentText,
        textBefore = text.before,
        textAfter = text.after,
        cssSelector = locations.cssSelector,
    )
}

@ExperimentalReadiumApi
internal fun TtsLocator.substring(range: IntRange): TtsLocator {
    return copy(
        textBefore = textBefore.orEmpty() + text.substring(0, range.first),
        text = text.substring(range),
        textAfter = text.substring(range.last) + textAfter.orEmpty()
    )
}
