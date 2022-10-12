/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
import java.text.NumberFormat

@ExperimentalReadiumApi
class PsPdfKitPreferencesEditor(
    private val currentSettings: PsPdfKitSettingsValues,
    initialPreferences: PsPdfKitPreferences,
    private val pubMetadata: Metadata,
    defaults: PsPdfKitSettingsDefaults,
    override val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
    private val pageSpacingProgression: ProgressionStrategy<Double> = DoubleIncrement(5.0),

    private val onPreferencesEdited: (PsPdfKitPreferences) -> Unit,
) : ReadingProgressionEditor, ScrollEditor, ScrollAxisEditor,
    SpreadEditor, OffsetEditor, FitEditor, PageSpacingEditor {

    private val settingsPolicy: PsPdfKitSettingsPolicy =
        PsPdfKitSettingsPolicy(defaults)

    private val newPreferences: PsPdfKitPreferences =
        initialPreferences.copy()

    override var readingProgression: ReadingProgression?
        get() = newPreferences.readingProgression
        set(value) {
            require(readingProgression in supportedReadingProgressionValues)
            newPreferences.readingProgression = value
        }

    override val isReadingProgressionPreferenceActive: Boolean =
        true

    override val supportedReadingProgressionValues: List<ReadingProgression> =
        listOf(ReadingProgression.LTR, ReadingProgression.RTL)

    override var scroll: Boolean?
        get() = newPreferences.scroll
        set(value) {
            newPreferences.scroll = value
        }

    override val isScrollPreferenceActive: Boolean =
        true

    override fun toggleScroll() {
        scroll = !(scroll ?: currentSettings.scroll)
    }

    override var scrollAxis: Axis?
        get() = newPreferences.scrollAxis
        set(value) {
            newPreferences.scrollAxis = value
        }

    override val isScrollAxisPreferenceActive: Boolean
       get() = settingsPolicy.settings(pubMetadata, newPreferences).scroll

    override val supportedScrollAxes: List<Axis> =
        listOf(Axis.VERTICAL, Axis.HORIZONTAL)

    override var spread: Spread?
        get() = newPreferences.spread
        set(value) {
            newPreferences.spread = value
        }

    override val isSpreadPreferenceActive: Boolean =
        true

    override val supportedSpreadValues: List<Spread> =
        listOf(Spread.AUTO, Spread.NEVER, Spread.PREFERRED)

    override var fit: Fit?
        get() = newPreferences.fit
        set(value) {
            require(value in supportedFitValues)
            newPreferences.fit
        }

    override val isFitPreferenceActive: Boolean =
        true

    override val supportedFitValues: List<Fit> =
        listOf(Fit.CONTAIN, Fit.WIDTH)

    override var offset: Boolean?
        get() = newPreferences.offset
        set(value) {
            newPreferences.offset = value
        }

    override val isOffsetPreferenceActive: Boolean
        get() = settingsPolicy.settings(pubMetadata, newPreferences).spread != Spread.NEVER

    override fun toggleOffset() {
        newPreferences.offset = newPreferences.offset ?: currentSettings.offset
    }

    override var pageSpacing: Double?
        get() = newPreferences.pageSpacing
        set(value) {
            newPreferences.pageSpacing = value
        }

    override val isPageSpacingPreferenceActive: Boolean
        get() = settingsPolicy.settings(pubMetadata, newPreferences).scroll

    override fun incrementPageSpacing() {
        newPreferences.pageSpacing = pageSpacingProgression
            .increment(newPreferences.pageSpacing ?: currentSettings.pageSpacing)
            .coerceIn(pageSpacingRange)
    }

    override fun decrementPageSpacing() {
        newPreferences.pageSpacing = pageSpacingProgression
            .decrement(newPreferences.pageSpacing ?: currentSettings.pageSpacing)
            .coerceIn(pageSpacingRange)
    }

    override fun formatPageSpacing(value: Double): String =
        NumberFormat.getNumberInstance().run {
            maximumFractionDigits = 1
            format(value)
        }

    fun toPreferences(): PsPdfKitPreferences =
        newPreferences
}

@ExperimentalReadiumApi
@Serializable
data class PsPdfKitPreferences(
    var readingProgression: ReadingProgression? = null,
    var scroll: Boolean? = null,
    var scrollAxis: Axis? = null,
    var fit: Fit? = null,
    var spread: Spread? = null,
    var pageSpacing: Double? = null,
    var offset: Boolean?
) : Configurable.Preferences {

    fun filterPublicationPreferences(): PsPdfKitPreferences =
        copy(scroll = null, fit = null, spread = null, pageSpacing = null)

    fun filterNavigatorPreferences(): PsPdfKitPreferences =
        copy(readingProgression = null, scrollAxis = null, offset = null)

    companion object {

        fun merge(vararg preferences: PsPdfKitPreferences): PsPdfKitPreferences =
            PsPdfKitPreferences(
                readingProgression = preferences.firstNotNullOfOrNull { it.readingProgression },
                scroll = preferences.firstNotNullOfOrNull { it.scroll },
                scrollAxis = preferences.firstNotNullOfOrNull { it.scrollAxis },
                fit = preferences.firstNotNullOfOrNull { it.fit },
                spread = preferences.firstNotNullOfOrNull { it.spread },
                pageSpacing = preferences.firstNotNullOfOrNull { it.pageSpacing },
                offset = preferences.firstNotNullOfOrNull { it.offset }
            )
    }
}
