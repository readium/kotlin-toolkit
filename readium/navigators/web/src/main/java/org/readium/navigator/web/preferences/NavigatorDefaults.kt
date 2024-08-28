package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public data class NavigatorDefaults(
    val fit: Fit? = Fit.CONTAIN,
    val readingProgression: ReadingProgression? = null,
    val spreads: Boolean? = false
)
