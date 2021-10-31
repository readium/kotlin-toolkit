package org.readium.r2.navigator2.view.image

import android.view.View
import org.readium.r2.navigator2.view.ResourceAdapter
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.toLocator

class ImageResourceAdapter(
    private val link: Link,
    private val view: View
    ) : ResourceAdapter {

    override val currentLocation: Locator
        get() = link.toLocator()
}