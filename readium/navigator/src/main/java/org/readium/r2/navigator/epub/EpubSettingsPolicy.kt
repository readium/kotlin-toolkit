package org.readium.r2.navigator.epub

import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@OptIn(ExperimentalReadiumApi::class)
interface EpubSettingsPolicy {

    @ExperimentalReadiumApi
    fun reflowableSettings(metadata: Metadata, preferences: Preferences): EpubSettingsValues.Reflowable =
        EpubSettingsDefaultPolicy.reflowableSettings(metadata, preferences)

    @ExperimentalReadiumApi
    fun fixedLayoutSettings(metadata: Metadata, preferences: Preferences): EpubSettingsValues.FixedLayout =
        EpubSettingsDefaultPolicy.fixedLayoutSettings(metadata, preferences)

    companion object {

        val defaultPolicy: EpubSettingsPolicy = EpubSettingsDefaultPolicy
    }
}
