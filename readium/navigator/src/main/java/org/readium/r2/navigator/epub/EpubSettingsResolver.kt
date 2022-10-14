/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.extensions.isCjk
import org.readium.r2.navigator.epub.extensions.isRtl
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

/**
 * A policy which computes EPUB settings values from sets of metadata and preferences.
 */
internal class EpubSettingsResolver(
    private val metadata: Metadata
) {

    fun settings(preferences: EpubPreferences): EpubSettings {
        val (language, readingProgression) = resolveReadingProgression(metadata, preferences)

        val verticalPref = preferences.verticalText
        val verticalText = resolveVerticalText(verticalPref, language, readingProgression)

        val theme = preferences.theme ?: Theme.LIGHT
        val backgroundColor = preferences.backgroundColor ?: Color(theme.backgroundColor)
        val textColor = preferences.textColor ?: Color(theme.contentColor)

        return EpubSettings(
            language = language,
            readingProgression = readingProgression,
            spread = preferences.spread ?: Spread.NEVER,
            verticalText = verticalText,
            theme = theme,
            backgroundColor = backgroundColor,
            textColor = textColor,
            columnCount = preferences.columnCount ?: ColumnCount.AUTO,
            fontFamily = preferences.fontFamily,
            fontSize = preferences.fontSize ?: 1.0,
            hyphens = preferences.hyphens ?: true,
            imageFilter = preferences.imageFilter ?: ImageFilter.NONE,
            letterSpacing = preferences.letterSpacing ?: 0.0,
            ligatures = preferences.ligatures ?: true,
            lineHeight = preferences.lineHeight ?: 1.2,
            pageMargins = preferences.pageMargins ?: 1.0,
            paragraphIndent = preferences.paragraphIndent ?: 0.0,
            paragraphSpacing = preferences.paragraphSpacing ?: 0.0,
            publisherStyles = preferences.publisherStyles ?: true,
            scroll = preferences.scroll ?: false,
            textAlign = preferences.textAlign ?: TextAlign.START,
            textNormalization = preferences.textNormalization ?: TextNormalization.NONE,
            typeScale = preferences.typeScale ?: 1.2,
            wordSpacing = preferences.wordSpacing ?: 0.0,
        )
    }

    private fun resolveReadingProgression(metadata: Metadata, preferences: EpubPreferences): Pair<Language?, ReadingProgression> {
        val rpPref = preferences.readingProgression
        val langPref = preferences.language
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
}
