/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.extensions.isCjk
import org.readium.r2.navigator.epub.extensions.isRtl
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
internal class EpubSettingsResolver(
    private val metadata: Metadata,
    private val defaults: EpubDefaults,
) {

    fun settings(preferences: EpubPreferences): EpubSettings {
        val (language, readingProgression) = resolveReadingProgression(metadata, preferences)

        val verticalPref = preferences.verticalText
        val verticalText = resolveVerticalText(verticalPref, language, readingProgression)

        val theme = preferences.theme ?: Theme.LIGHT

        var scroll = preferences.scroll ?: defaults.scroll ?: false

        // / We disable pagination with vertical text, because CSS columns don't support it properly.
        // / See https://github.com/readium/swift-toolkit/discussions/370
        if (verticalText) {
            scroll = true
        }

        return EpubSettings(
            backgroundColor = preferences.backgroundColor,
            columnCount = preferences.columnCount ?: defaults.columnCount ?: ColumnCount.AUTO,
            fontFamily = preferences.fontFamily,
            fontSize = preferences.fontSize ?: defaults.fontSize ?: 1.0,
            fontWeight = preferences.fontWeight ?: defaults.fontWeight,
            hyphens = preferences.hyphens ?: defaults.hyphens,
            imageFilter = preferences.imageFilter ?: defaults.imageFilter,
            language = language,
            letterSpacing = preferences.letterSpacing ?: defaults.letterSpacing,
            ligatures = preferences.ligatures ?: defaults.ligatures,
            lineHeight = preferences.lineHeight ?: defaults.lineHeight,
            pageMargins = preferences.pageMargins ?: defaults.pageMargins ?: 1.0,
            paragraphIndent = preferences.paragraphIndent ?: defaults.paragraphIndent,
            paragraphSpacing = preferences.paragraphSpacing ?: defaults.paragraphSpacing,
            publisherStyles = preferences.publisherStyles ?: defaults.publisherStyles ?: true,
            readingProgression = readingProgression,
            scroll = scroll,
            spread = preferences.spread ?: defaults.spread ?: Spread.NEVER,
            textAlign = preferences.textAlign ?: defaults.textAlign,
            textColor = preferences.textColor,
            textNormalization = preferences.textNormalization ?: defaults.textNormalization ?: false,
            theme = theme,
            typeScale = preferences.typeScale ?: defaults.typeScale,
            verticalText = verticalText,
            wordSpacing = preferences.wordSpacing ?: defaults.wordSpacing
        )
    }

    private fun resolveReadingProgression(
        metadata: Metadata,
        preferences: EpubPreferences,
    ): Pair<Language?, ReadingProgression> {
        val rpPref = preferences.readingProgression
        val langPref = preferences.language
        val metadataLanguage = metadata.language
        val metadataReadingProgression = metadata.readingProgression

        // Compute language according to the following rule:
        // preference value > metadata value > default value > null
        val language = langPref
            ?: metadataLanguage
            ?: defaults.language

        // Compute readingProgression according to the following rule:
        // preference value > value inferred from language preference > metadata value
        // value inferred from metadata languages > default value >
        // value inferred from default language > LTR
        val readingProgression = when {
            rpPref != null ->
                rpPref
            langPref != null ->
                if (langPref.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            metadataReadingProgression != null ->
                when (metadataReadingProgression) {
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    PublicationReadingProgression.LTR -> ReadingProgression.LTR
                }
            metadataLanguage != null ->
                if (metadataLanguage.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            defaults.readingProgression != null ->
                defaults.readingProgression
            defaults.language != null ->
                if (defaults.language.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            else ->
                ReadingProgression.LTR
        }

        return Pair(language, readingProgression)
    }

    // Compute verticalText according to the following rule:
    // preference value > value computed from resolved language > false
    private fun resolveVerticalText(
        verticalPreference: Boolean?,
        language: Language?,
        readingProgression: ReadingProgression,
    ) = when {
        verticalPreference != null -> verticalPreference
        language != null -> language.isCjk && readingProgression == ReadingProgression.RTL
        else -> false
    }
}
