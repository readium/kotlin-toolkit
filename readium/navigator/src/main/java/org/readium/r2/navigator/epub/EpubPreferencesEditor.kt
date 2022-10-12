/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
sealed class EpubPreferencesEditor: ReadingProgressionEditor {

    class Reflowable(
        private val currentSettings: EpubSettingsValues.Reflowable,
        initialPreferences: EpubPreferences.Reflowable
    ) : EpubPreferencesEditor() {

        private val newProperties = initialPreferences.copy()

        override var readingProgression: ReadingProgression?
            get() = newProperties.readingProgression
            set(value) {
                newProperties.readingProgression = value
            }

        override val isReadingProgressionPreferenceActive: Boolean =
            true

        override val supportedReadingProgressionValues: List<ReadingProgression> =
            listOf(ReadingProgression.LTR, ReadingProgression.RTL)

    }

    class FixedLayout(
        private val currentSettings: EpubSettingsValues.FixedLayout,
        initialPreferences: EpubPreferences.FixedLayout
    ): EpubPreferencesEditor() {

        private val newProperties = initialPreferences.copy()

        override var readingProgression: ReadingProgression?
            get() = newProperties.readingProgression
            set(value) {
                newProperties.readingProgression = value
            }

        override val isReadingProgressionPreferenceActive: Boolean =
            true

        override val supportedReadingProgressionValues: List<ReadingProgression> =
            listOf(ReadingProgression.LTR, ReadingProgression.RTL)

    }
}

@ExperimentalReadiumApi
@Serializable
sealed class EpubPreferences: Configurable.Preferences {

    abstract var readingProgression: ReadingProgression?
    abstract var language: Language?

    data class Reflowable(
        var backgroundColor: Color? = null,
        var columnCount: ColumnCount? = null,
        var fontFamily: FontFamily? = null,
        var fontSize: Double? = null,
        var hyphens: Boolean? = null,
        var imageFilter: ImageFilter? = null,
        override var language: Language? = null,
        var letterSpacing: Double? = null,
        var ligatures: Boolean? = null,
        var lineHeight: Double? = null,
        var pageMargins: Double? = null,
        var paragraphIndent: Double? = null,
        var paragraphSpacing: Double? = null,
        var publisherStyles: Boolean? = null,
        override var readingProgression: ReadingProgression? = null,
        var scroll: Boolean? = null,
        var textAlign: TextAlign? = null,
        var textColor: Color? = null,
        var textNormalization: TextNormalization? = null,
        var theme: Theme? = null,
        var typeScale: Double? = null,
        var verticalText: Boolean? = null,
        var wordSpacing: Double? = null
    ) : EpubPreferences() {

        fun filterNavigatorPreferences() =
            copy(
                language = null,
                readingProgression = null,
                verticalText = null
            )

        fun filterPublicationPreferences() =
            Reflowable(
                language = language,
                readingProgression = readingProgression,
                verticalText = verticalText
            )

        companion object {

            fun merge(vararg preferences: Reflowable) =
                Reflowable(
                    backgroundColor = preferences.firstNotNullOfOrNull { it.backgroundColor },
                    columnCount = preferences.firstNotNullOfOrNull { it.columnCount },
                    fontFamily = preferences.firstNotNullOfOrNull { it.fontFamily },
                    fontSize = preferences.firstNotNullOfOrNull { it.fontSize },
                    hyphens = preferences.firstNotNullOfOrNull { it.hyphens },
                    imageFilter = preferences.firstNotNullOfOrNull { it.imageFilter },
                    language = preferences.firstNotNullOfOrNull { it.language },
                    letterSpacing = preferences.firstNotNullOfOrNull { it.letterSpacing },
                    ligatures = preferences.firstNotNullOfOrNull { it.ligatures },
                    lineHeight = preferences.firstNotNullOfOrNull { it.lineHeight },
                    pageMargins = preferences.firstNotNullOfOrNull { it.pageMargins },
                    paragraphIndent = preferences.firstNotNullOfOrNull { it.paragraphIndent },
                    paragraphSpacing = preferences.firstNotNullOfOrNull { it.paragraphSpacing },
                    publisherStyles = preferences.firstNotNullOfOrNull { it.publisherStyles },
                    readingProgression = preferences.firstNotNullOfOrNull { it.readingProgression },
                    scroll = preferences.firstNotNullOfOrNull { it.scroll },
                    textAlign = preferences.firstNotNullOfOrNull { it.textAlign },
                    textColor = preferences.firstNotNullOfOrNull { it.textColor },
                    textNormalization = preferences.firstNotNullOfOrNull { it.textNormalization },
                    theme = preferences.firstNotNullOfOrNull { it.theme },
                    typeScale = preferences.firstNotNullOfOrNull { it.typeScale },
                    verticalText = preferences.firstNotNullOfOrNull { it.verticalText },
                    wordSpacing = preferences.firstNotNullOfOrNull { it.wordSpacing },
                )
        }
    }

    data class FixedLayout(
        override var language: Language? = null,
        override var readingProgression: ReadingProgression? = null,
        val spread: Spread? = null
    ) : EpubPreferences() {

        fun filterNavigatorPreferences() =
            FixedLayout(spread = spread)


        fun filterPublicationPreferences() =
            copy(spread = null)

        companion object {

            fun merge(vararg preferences: EpubPreferences.FixedLayout) =
                FixedLayout(
                    language = preferences.firstNotNullOfOrNull { it.language },
                    readingProgression = preferences.firstNotNullOfOrNull { it.readingProgression },
                    spread = preferences.firstNotNullOfOrNull { it.spread }
                )
        }
    }
}
