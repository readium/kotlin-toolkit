package org.readium.navigator.pdf

import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.LocatorAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public sealed interface PdfGoLocation : GoLocation

@ExperimentalReadiumApi
public data class PageLocation(
    override val href: Url,
    val page: Int
) : Location, PdfGoLocation

@ExperimentalReadiumApi
public data class PositionLocation(
    val position: Int
) : PdfGoLocation

@ExperimentalReadiumApi
public data class PdfLocation(
    override val href: Url,
    val page: Int
) : Location

@ExperimentalReadiumApi
public class PdfLocatorAdapter(
    private val publication: Publication
) : LocatorAdapter<PdfLocation, PdfGoLocation> {
    override fun Locator.toGoLocation(): PdfGoLocation =
        PositionLocation(position = locations.position!!)

    override fun PdfLocation.toLocator(): Locator =
        publication.locatorFromLink(Link(href))!!
            .copyWithLocations(position = page)
}
