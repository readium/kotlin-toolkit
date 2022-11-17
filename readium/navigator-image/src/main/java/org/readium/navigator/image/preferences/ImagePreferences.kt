package org.readium.navigator.image.preferences

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@Serializable
@ExperimentalReadiumApi
data class ImagePreferences(
    val fit: Fit? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val scrollAxis: Axis? = null
) : Configurable.Preferences {

    operator fun plus(other: ImagePreferences) =
        ImagePreferences(
            fit = other.fit ?: fit,
            readingProgression = other.readingProgression ?: readingProgression,
            scroll = other.scroll ?: scroll,
            scrollAxis = other.scrollAxis ?: scrollAxis
        )
}
