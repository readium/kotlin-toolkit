package org.readium.navigator.image.preferences

import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
data class ImageDefaults(
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null
)
