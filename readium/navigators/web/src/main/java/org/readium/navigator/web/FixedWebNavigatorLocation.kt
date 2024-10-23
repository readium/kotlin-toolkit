package org.readium.navigator.web

import org.readium.navigator.common.Location
import org.readium.r2.shared.util.Url

public data class FixedWebNavigatorLocation(
    override val href: Url
) : Location
