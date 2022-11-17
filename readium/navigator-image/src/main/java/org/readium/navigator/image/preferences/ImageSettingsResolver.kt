package org.readium.navigator.image.preferences

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression

@ExperimentalReadiumApi
internal class ImageSettingsResolver(
    private val metadata: Metadata,
    private val defaults: ImageDefaults
) {

    fun settings(preferences: ImagePreferences): ImageSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: when (metadata.readingProgression) {
                    PublicationReadingProgression.LTR -> ReadingProgression.LTR
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    else -> null
                } ?: defaults.readingProgression
                ?: ReadingProgression.LTR

        val scroll: Boolean =
            preferences.scroll
                ?: defaults.scroll
                ?: false

        val scrollAxis: Axis =
            preferences.scrollAxis
                ?: Axis.VERTICAL

        val fit: Fit =
            preferences.fit ?: when {
                !scroll || scrollAxis == Axis.HORIZONTAL -> Fit.CONTAIN
                else -> Fit.WIDTH
            }

        return ImageSettings(
            fit = fit,
            readingProgression = readingProgression,
            scroll = scroll,
            scrollAxis = scrollAxis
        )
    }
}
