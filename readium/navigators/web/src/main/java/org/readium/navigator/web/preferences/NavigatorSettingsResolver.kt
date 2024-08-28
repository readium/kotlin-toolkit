package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression

@ExperimentalReadiumApi
internal class NavigatorSettingsResolver(
    private val metadata: Metadata,
    private val defaults: NavigatorDefaults
) {

    fun settings(preferences: NavigatorPreferences): NavigatorSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: when (metadata.readingProgression) {
                    PublicationReadingProgression.LTR -> ReadingProgression.LTR
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    else -> null
                } ?: defaults.readingProgression
                ?: ReadingProgression.LTR

        val fit = preferences.fit ?: defaults.fit ?: Fit.CONTAIN

        val spreads = preferences.spreads ?: defaults.spreads ?: false

        return NavigatorSettings(
            fit = fit,
            readingProgression = readingProgression,
            spreads = spreads
        )
    }
}
