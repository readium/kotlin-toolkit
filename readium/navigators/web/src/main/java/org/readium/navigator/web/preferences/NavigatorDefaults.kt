package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public data class NavigatorDefaults(
    val readingProgression: ReadingProgression? = null
)
