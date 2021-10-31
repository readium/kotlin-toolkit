package org.readium.r2.navigator2.view

import android.graphics.PointF
import org.readium.r2.shared.publication.Locator

interface NavigatorListener {
    fun onTap(point: PointF): Boolean

    fun onLocationChanged(newLocation: Locator)
}