package org.readium.navigator.pdf

import org.readium.navigator.common.Location
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url

public data class PdfNavigatorLocation(
    val locator: Locator
) : Location {
    override val href: Url = locator.href
}
