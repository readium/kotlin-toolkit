/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
internal class LayoutResolver(
    private val metadata: Metadata,
    defaults: Preferences = Preferences(),
) {
    private val defaultSettings: EpubSettings.Reflowable =
        EpubSettings.Reflowable()
    private val rpDefault: ReadingProgression? =
        defaultSettings.readingProgression.firstValidValue(defaults)
    private val langDefault: Language? =
        defaultSettings.language.firstValidValue(defaults)
    private val verticalDefault: Boolean? =
        defaultSettings.verticalText.firstValidValue(defaults)

    fun resolve(preferences: Preferences = Preferences()): Layout {
        val rpPref = defaultSettings.readingProgression.firstValidValue(preferences)
        val langPref = defaultSettings.language.firstValidValue(preferences)
        val verticalPref = defaultSettings.verticalText.firstValidValue(preferences)
        val metadataLanguage = metadata.language

        // Compute language according to the following rule:
        // preference value > metadata value > default value > null
        val language = langPref
            ?: metadataLanguage
            ?: langDefault

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
            rpDefault != null ->
                rpDefault
            langDefault != null ->
                if (langDefault.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            else ->
                ReadingProgression.LTR
        }

        // Compute verticalText according to the following rule:
        // preference value > value computed from language > default value > false
        val verticalText = when {
            verticalPref != null -> verticalPref
            language != null -> language.isCjk && readingProgression == ReadingProgression.RTL
            verticalDefault != null -> verticalDefault
            else -> false
        }

        val stylesheets: Layout.Stylesheets = computeStyleSheets(verticalText, language, readingProgression)
        return Layout(language, stylesheets, readingProgression)
    }

    private fun computeStyleSheets(verticalText: Boolean, language: Language?, readingProgression: ReadingProgression) =
        when {
            verticalText ->
                Layout.Stylesheets.CjkVertical
            language?.isCjk == true ->
                Layout.Stylesheets.CjkHorizontal
            readingProgression == ReadingProgression.RTL ->
                Layout.Stylesheets.Rtl
            else -> {
                Layout.Stylesheets.Default
            }
        }

    private val Language.isRtl: Boolean get() {
        val c = code.lowercase()
        return c == "ar"
            || c == "fa"
            || c == "he"
            || c == "zh-hant"
            || c == "zh-tw"
    }

    private val Language.isCjk: Boolean get() {
        val c = code.lowercase()
        return c == "ja"
            || c == "ko"
            || removeRegion().code.lowercase() == "zh"
    }
}
