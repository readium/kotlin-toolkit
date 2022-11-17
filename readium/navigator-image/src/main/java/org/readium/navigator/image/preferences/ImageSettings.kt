package org.readium.navigator.image.preferences

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
data class ImageSettings(
    val fit: Fit,
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val scrollAxis: Axis
): Configurable.Settings
