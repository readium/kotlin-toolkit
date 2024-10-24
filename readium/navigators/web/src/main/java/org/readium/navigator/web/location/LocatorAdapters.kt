package org.readium.navigator.web.location

import org.readium.navigator.common.LocatorAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.indexOfFirstWithHref

@ExperimentalReadiumApi
public class FixedWebLocatorAdapter(
    private val publication: Publication
) : LocatorAdapter<FixedWebLocation, FixedWebGoLocation> {
    public fun FixedWebGoLocation.toLocator(): Locator =
        when (this) {
            is HrefLocation -> publication.locatorFromLink(Link(href))!!
        }

    public override fun Locator.toGoLocation(): FixedWebGoLocation =
        HrefLocation(href)

    override fun FixedWebLocation.toLocator(): Locator {
        val position = publication.readingOrder.indexOfFirstWithHref(href)
        return publication.locatorFromLink(Link(href))!!
            .copyWithLocations(position = position)
    }
}

@ExperimentalReadiumApi
public class ReflowableWebLocatorAdapter(
    private val publication: Publication,
    private val allowProduceHrefLocation: Boolean = false
) : LocatorAdapter<ReflowableWebLocation, ReflowableWebGoLocation> {
    public override fun ReflowableWebLocation.toLocator(): Locator =
        publication.locatorFromLink(Link(href))!!
            .copy(
                text = Locator.Text(
                    after = textAfter,
                    before = textBefore
                )
            )
            .copyWithLocations(
                progression = progression,
                position = position,
                otherLocations = buildMap { cssSelector?.let { put("cssSelector", cssSelector) } }
            )

    public override fun Locator.toGoLocation(): ReflowableWebGoLocation {
        return when {
            text.highlight != null || text.before != null || text.after != null -> {
                TextLocation(
                    href = href,
                    textBefore = text.before,
                    textAfter = text.highlight?.let { it + text.after } ?: text.after,
                    cssSelector = locations.cssSelector
                )
            }
            locations.progression != null -> {
                ProgressionLocation(
                    href = href,
                    progression = locations.progression!!
                )
            }
            locations.position != null -> {
                PositionLocation(
                    position = locations.position!!
                )
            }
            else ->
                if (allowProduceHrefLocation) {
                    HrefLocation(href = href)
                } else {
                    throw IllegalArgumentException("No supported location found in locator.")
                }
        }
    }
}
