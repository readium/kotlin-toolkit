package org.readium.r2.navigator2.view

import android.graphics.PointF
import android.view.View
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

interface SpreadAdapter {

    val links: List<Link>

    fun bind(view: View)

    fun unbind(view: View)

    fun scrollForLocations(locations: Locator.Locations, view: View): PointF

    fun applySettings() {}

    fun resourceAdapters(view: View): List<ResourceAdapter>
}