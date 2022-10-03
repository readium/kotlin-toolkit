/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.extensions.isCjk
import org.readium.r2.navigator.epub.extensions.isRtl
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Language

/**
 * A policy which computes EPUB settings values from sets of metadata and preferences.
 *
 * If you implement a custom [EpubSettingsPolicy], be sure that all values from settings
 * that will be active at the same time are compatible.
 */
@ExperimentalReadiumApi
interface EpubSettingsPolicy {

    fun reflowableSettings(metadata: Metadata, preferences: Preferences): EpubSettingsValues.Reflowable {
        val (language, readingProgression) = resolveReadingProgression(metadata, preferences)

        val verticalPref = preferences[EpubSettings.VERTICAL_TEXT]
        val verticalText = resolveVerticalText(verticalPref, language, readingProgression)

        val theme = preferences[EpubSettings.THEME] ?: Theme.LIGHT
        val backgroundColor = preferences[EpubSettings.BACKGROUND_COLOR] ?: Color(theme.backgroundColor)
        val textColor = preferences[EpubSettings.TEXT_COLOR] ?: Color(theme.contentColor)

        return EpubSettingsValues.Reflowable(
            language = language,
            readingProgression = readingProgression,
            verticalText = verticalText,
            theme = theme,
            backgroundColor = backgroundColor,
            textColor = textColor,
            columnCount = preferences[EpubSettings.COLUMN_COUNT] ?: ColumnCount.AUTO,
            fontFamily = preferences[EpubSettings.FONT_FAMILY],
            fontSize = preferences[EpubSettings.FONT_SIZE] ?: 1.0,
            hyphens = preferences[EpubSettings.HYPHENS] ?: true,
            imageFilter = preferences[EpubSettings.IMAGE_FILTER] ?: ImageFilter.NONE,
            letterSpacing = preferences[EpubSettings.LETTER_SPACING] ?: 0.0,
            ligatures = preferences[EpubSettings.LIGATURES] ?: true,
            lineHeight = preferences[EpubSettings.LINE_HEIGHT] ?: 1.2,
            pageMargins = preferences[EpubSettings.PAGE_MARGINS] ?: 1.0,
            paragraphIndent = preferences[EpubSettings.PARAGRAPH_INDENT] ?: 0.0,
            paragraphSpacing = preferences[EpubSettings.PARAGRAPH_SPACING] ?: 0.0,
            publisherStyles = preferences[EpubSettings.PUBLISHER_STYLES] ?: true,
            scroll = preferences[EpubSettings.SCROLL] ?: false,
            textAlign = preferences[EpubSettings.TEXT_ALIGN] ?: TextAlign.START,
            textNormalization = preferences[EpubSettings.TEXT_NORMALIZATION] ?: TextNormalization.NONE,
            typeScale = preferences[EpubSettings.TYPE_SCALE] ?: 1.2,
            wordSpacing = preferences[EpubSettings.WORD_SPACING] ?: 0.0,
        )
    }

    fun fixedLayoutSettings(metadata: Metadata, preferences: Preferences): EpubSettingsValues.FixedLayout {
        val (language, readingProgression) = resolveReadingProgression(metadata, preferences)

        return EpubSettingsValues.FixedLayout(
            language = language,
            readingProgression = readingProgression,
            spread = Presentation.Spread.NONE
        )
    }

    private fun resolveReadingProgression(metadata: Metadata, preferences: Preferences): Pair<Language?, ReadingProgression> {
        val rpPref = preferences[EpubSettings.READING_PROGRESSION]
        val langPref = preferences[EpubSettings.LANGUAGE]
        val metadataLanguage = metadata.language

        // Compute language according to the following rule:
        // preference value > metadata value > default value > null
        val language = langPref
            ?: metadataLanguage

        // Compute readingProgression according to the following rule:
        // preference value > value inferred from language preference > metadata value
        // value inferred from metadata languages > default value >
        // value inferred from language default > LTR
        val readingProgression = when {
            rpPref != null ->
                rpPref
            langPref != null ->
                if (langPref.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            metadata.readingProgression.isHorizontal == true ->
                metadata.readingProgression
            metadataLanguage != null ->
                if (metadataLanguage.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            else ->
                ReadingProgression.LTR
        }

        return Pair(language, readingProgression)
    }

    // Compute verticalText according to the following rule:
    // preference value > value computed from language > false
    private fun resolveVerticalText(verticalPreference: Boolean?, language: Language?, readingProgression: ReadingProgression) =
        when {
            verticalPreference != null -> verticalPreference
            language != null -> language.isCjk && readingProgression == ReadingProgression.RTL
            else -> false
        }

    companion object {

        internal operator fun invoke(): EpubSettingsPolicy =
            object  : EpubSettingsPolicy {}
    }
}
