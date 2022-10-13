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
    private val publicationMetadata: Metadata,
    defaults: PsPdfKitSettingsDefaults,
    override val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
    private val pageSpacingProgression: ProgressionStrategy<Double> = DoubleIncrement(5.0),

    private val onPreferencesEdited: (PsPdfKitPreferences) -> Unit,
) : PreferencesEditor, ReadingProgressionEditor, ScrollEditor, ScrollAxisEditor,
    SpreadEditor, OffsetEditor, FitEditor, PageSpacingEditor {

    private val settingsPolicy: PsPdfKitSettingsPolicy =
        PsPdfKitSettingsPolicy(defaults)

    private var newPreferences: PsPdfKitPreferences = initialPreferences.copy()
        set(value) {
            field = value
            onPreferencesEdited(value)
        }

    override fun clearPreferences() {
        newPreferences = PsPdfKitPreferences()
    }

    override var readingProgression: ReadingProgression?
        get() = newPreferences.readingProgression
        set(value) {
            require(readingProgression in supportedReadingProgressionValues)
            newPreferences = newPreferences.copy(readingProgression = value)
        }

    override val isReadingProgressionPreferenceActive: Boolean =
        true

    override val supportedReadingProgressionValues: List<ReadingProgression> =
        listOf(ReadingProgression.LTR, ReadingProgression.RTL)

    override var scroll: Boolean?
        get() = newPreferences.scroll
        set(value) {
            newPreferences = newPreferences.copy(scroll = value)
        }

    override val isScrollPreferenceActive: Boolean =
        true

    override fun toggleScroll() {
        scroll = !(scroll ?: currentSettings.scroll)
    }

    override var scrollAxis: Axis?
        get() = newPreferences.scrollAxis
        set(value) {
            newPreferences = newPreferences.copy(scrollAxis = value)
        }

    override val isScrollAxisPreferenceActive: Boolean
       get() = settingsPolicy.settings(publicationMetadata, newPreferences).scroll

    override val supportedScrollAxes: List<Axis> =
        listOf(Axis.VERTICAL, Axis.HORIZONTAL)

    override var spread: Spread?
        get() = newPreferences.spread
        set(value) {
            newPreferences = newPreferences.copy(spread = value)
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
            newPreferences = newPreferences.copy(offset = value)
        }

    override val isOffsetPreferenceActive: Boolean
        get() = settingsPolicy.settings(publicationMetadata, newPreferences).spread != Spread.NEVER

    override fun toggleOffset() {
        newPreferences = newPreferences.copy(offset = newPreferences.offset ?: currentSettings.offset)
    }

    override var pageSpacing: Double?
        get() = newPreferences.pageSpacing
        set(value) {
            newPreferences = newPreferences.copy(pageSpacing = value)
        }

    override val isPageSpacingPreferenceActive: Boolean
        get() = settingsPolicy.settings(publicationMetadata, newPreferences).scroll

    override fun incrementPageSpacing() {
        val pageSpacing = pageSpacingProgression
            .increment(newPreferences.pageSpacing ?: currentSettings.pageSpacing)
            .coerceIn(pageSpacingRange)
        newPreferences = newPreferences.copy(pageSpacing = pageSpacing)
    }

    override fun decrementPageSpacing() {
        val pageSpacing = pageSpacingProgression
            .decrement(newPreferences.pageSpacing ?: currentSettings.pageSpacing)
            .coerceIn(pageSpacingRange)
        newPreferences = newPreferences.copy(pageSpacing = pageSpacing)
    }

    override fun formatPageSpacing(value: Double): String =
        NumberFormat.getNumberInstance().run {
            maximumFractionDigits = 1
            format(value)
        }
}

@ExperimentalReadiumApi
@Serializable
data class PsPdfKitPreferences(
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val scrollAxis: Axis? = null,
    val fit: Fit? = null,
    val spread: Spread? = null,
    val pageSpacing: Double? = null,
    val offset: Boolean? = null
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
