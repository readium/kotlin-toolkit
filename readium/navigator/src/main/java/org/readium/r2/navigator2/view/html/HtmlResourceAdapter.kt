package org.readium.r2.navigator2.view.html

import org.readium.r2.navigator2.view.ResourceAdapter
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.toLocator

class HtmlResourceAdapter(
    val link: Link
) : ResourceAdapter {

    override val currentLocation: Locator
        get() = link.toLocator()
}