package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public data class PrepaginatedWebNavigatorSettings(
    val fit: Fit,
    val readingProgression: ReadingProgression,
    val spreads: Boolean
) : Configurable.Settings
