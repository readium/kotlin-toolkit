/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class, ExperimentalReadiumApi::class)

package org.readium.r2.navigator

import android.graphics.RectF
import org.json.JSONObject
import org.readium.r2.navigator.extensions.optRectF
import org.readium.r2.navigator.input.TargetElement
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Metadata about the element under a tap, produced by `extractTargetElement()` in gestures.js.
 *
 * @param tag Lowercased tag name of the image element (e.g. `img`, `svg`).
 * @param html Raw outer HTML, only present for inline SVGs without a resolvable source.
 * @param src Absolute URL of the image source, resolved against the document base URI.
 * @param rect On-screen frame of the element, in device pixels.
 * @param accessibilityLabel Accessibility label extracted from the `aria-label` attribute.
 * @param caption Caption extracted from `alt`, `title`, SVG `<title>`/`<desc>` or `<figcaption>`.
 * @param cssSelector CSS selector targeting the element in the resource.
 */
internal data class TargetElementData(
    val tag: String,
    val html: String?,
    val src: String?,
    val rect: RectF,
    val accessibilityLabel: String?,
    val caption: String?,
    val cssSelector: String?,
) {
    companion object {
        fun fromJSONObject(obj: JSONObject?): TargetElementData? {
            obj ?: return null

            val tag = obj.optNullableString("tag") ?: return null
            val rect = obj.optRectF("rect") ?: return null

            return TargetElementData(
                tag = tag,
                html = obj.optNullableString("html"),
                src = obj.optNullableString("src"),
                rect = rect,
                accessibilityLabel = obj.optNullableString("accessibilityLabel"),
                caption = obj.optNullableString("caption"),
                cssSelector = obj.optNullableString("cssSelector")
            )
        }
    }
}

/**
 * Builds the public [TargetElement] from the raw metadata produced by gestures.js.
 *
 * @param resourceLink Link to the resource owning the web view that emitted the tap.
 * @param adjustRect Adjusts the element frame to the navigator's viewport.
 * @param internalLinkForUrl Resolves a URL to a link in the publication manifest.
 */
internal fun TargetElementData.toTargetElement(
    resourceLink: Link,
    adjustRect: (RectF) -> RectF,
    internalLinkForUrl: (AbsoluteUrl) -> Link?,
): TargetElement? {
    // Locator pointing to the element inside the resource that contains it.
    val locator = Locator(
        href = resourceLink.url(),
        mediaType = resourceLink.mediaType ?: MediaType.XHTML,
        locations = Locator.Locations(
            otherLocations = cssSelector
                ?.let { mapOf("cssSelector" to it) }
                ?: emptyMap()
        )
    )

    val content = contentElement(locator, internalLinkForUrl) ?: return null

    return TargetElement(
        rect = adjustRect(rect),
        content = content
    )
}

/**
 * Maps the target element metadata to a [Content.Element].
 */
private fun TargetElementData.contentElement(
    locator: Locator,
    internalLinkForUrl: (AbsoluteUrl) -> Link?,
): Content.Element? {
    val attributes = buildList {
        accessibilityLabel?.takeIf { it.isNotBlank() }?.let { label ->
            add(Content.Attribute(Content.AttributeKey.ACCESSIBILITY_LABEL, label))
        }
    }

    val caption = caption?.takeIf { it.isNotBlank() }

    // Look up the source in the publication manifest so the client gets full metadata (media
    // type, etc.). For resources not in the manifest (e.g. external images), we synthesize a
    // plain Link.
    val embeddedLink = (src?.let { Url(it) } as? AbsoluteUrl)
        ?.let { url -> internalLinkForUrl(url) ?: Link(href = url) }

    if (embeddedLink != null) {
        return Content.ImageElement(
            locator = locator,
            embeddedLink = embeddedLink,
            caption = caption,
            attributes = attributes
        )
    }

    // Inline SVG fallback.
    if (tag == "svg" && html != null) {
        return Content.SvgElement(
            locator = locator,
            svg = html,
            caption = caption,
            attributes = attributes
        )
    }

    return null
}
