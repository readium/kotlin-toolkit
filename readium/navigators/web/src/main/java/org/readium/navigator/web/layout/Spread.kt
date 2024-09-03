package org.readium.navigator.web.layout

import org.readium.r2.shared.util.Url

internal sealed class Spread {

    data class Single(
        val page: Url
    ) : Spread()

    data class Double(
        val leftPage: Url?,
        val rightPage: Url?
    ) : Spread()
}
