/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Preferences for the EPUB navigator.
 *
 * @param backgroundColor Default page background color.
 * @param columnCount Number of reflowable columns to display (one-page view or two-page spread).
 * @param fontFamily Default typeface for the text.
 * @param fontSize Base text font size.
 * @param fontWeight Default boldness for the text.
 * @param hyphens Enable hyphenation.
 * @param imageFilter Filter applied to images in dark theme.
 * @param language Language of the publication content.
 * @param letterSpacing Space between letters.
 * @param ligatures Enable ligatures in Arabic.
 * @param lineHeight Leading line height.
 * @param pageMargins Factor applied to horizontal margins.
 * @param paragraphIndent Text indentation for paragraphs.
 * @param paragraphSpacing Vertical margins for paragraphs.
 * @param publisherStyles Indicates whether the original publisher styles should be observed.
 *   Many settings require this to be off.
 * @param readingProgression Direction of the reading progression across resources.
 * @param scroll Indicates if the overflow of resources should be handled using scrolling
 *   instead of synthetic pagination.
 * @param spread Indicates if the fixed-layout publication should be rendered with a
 *   synthetic spread (dual-page).
 * @param textAlign Page text alignment.
 * @param textColor Default page text color.
 * @param textNormalization Normalize text styles to increase accessibility.
 * @param theme Reader theme.
 * @param typeScale Scale applied to all element font sizes.
 * @param verticalText Indicates whether the text should be laid out vertically. This is used
 *   for example with CJK languages. This setting is automatically derived from the language if
 *   no preference is given.
 * @param wordSpacing Space between words.
 */
@Serializable
public data class EpubPreferences @ExperimentalReadiumApi constructor(
    val backgroundColor: Color? = null,
    val columnCount: ColumnCount? = null,
    val fontFamily: FontFamily? = null,
    val fontSize: Double? = null,
    val fontWeight: Double? = null,
    val hyphens: Boolean? = null,
    val imageFilter: ImageFilter? = null,
    val language: Language? = null,
    val letterSpacing: Double? = null,
    val ligatures: Boolean? = null,
    val lineHeight: Double? = null,
    val pageMargins: Double? = null,
    val paragraphIndent: Double? = null,
    val paragraphSpacing: Double? = null,
    val publisherStyles: Boolean? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val spread: Spread? = null,
    val textAlign: TextAlign? = null,
    val textColor: Color? = null,
    val textNormalization: Boolean? = null,
    val theme: Theme? = null,
    val typeScale: Double? = null,
    val verticalText: Boolean? = null,
    val wordSpacing: Double? = null,
) : Configurable.Preferences<EpubPreferences> {

    init {
        require(fontSize == null || fontSize >= 0)
        require(fontWeight == null || fontWeight in 0.0..2.5)
        require(letterSpacing == null || letterSpacing >= 0)
        require(pageMargins == null || pageMargins >= 0)
        require(paragraphSpacing == null || paragraphSpacing >= 0)
        require(spread in listOf(null, Spread.NEVER, Spread.ALWAYS))
        require(typeScale == null || typeScale >= 0)
        require(wordSpacing == null || wordSpacing >= 0)
    }

    @OptIn(ExperimentalReadiumApi::class)
    override operator fun plus(other: EpubPreferences): EpubPreferences =
        EpubPreferences(
            backgroundColor = other.backgroundColor ?: backgroundColor,
            columnCount = other.columnCount ?: columnCount,
            fontFamily = other.fontFamily ?: fontFamily,
            fontWeight = other.fontWeight ?: fontWeight,
            fontSize = other.fontSize ?: fontSize,
            hyphens = other.hyphens ?: hyphens,
            imageFilter = other.imageFilter ?: imageFilter,
            language = other.language ?: language,
            letterSpacing = other.letterSpacing ?: letterSpacing,
            ligatures = other.ligatures ?: ligatures,
            lineHeight = other.lineHeight ?: lineHeight,
            pageMargins = other.pageMargins ?: pageMargins,
            paragraphIndent = other.paragraphIndent ?: paragraphIndent,
            paragraphSpacing = other.paragraphSpacing ?: paragraphSpacing,
            publisherStyles = other.publisherStyles ?: publisherStyles,
            readingProgression = other.readingProgression ?: readingProgression,
            scroll = other.scroll ?: scroll,
            spread = other.spread ?: spread,
            textAlign = other.textAlign ?: textAlign,
            textColor = other.textColor ?: textColor,
            textNormalization = other.textNormalization ?: textNormalization,
            theme = other.theme ?: theme,
            typeScale = other.typeScale ?: typeScale,
            verticalText = other.verticalText ?: verticalText,
            wordSpacing = other.wordSpacing ?: wordSpacing
        )

    public companion object {

        /**
         * Loads the preferences from the legacy EPUB settings stored in the [SharedPreferences] with
         * given [sharedPreferencesName].
         *
         * This can be used to migrate the legacy settings to the new [EpubPreferences] format.
         *
         * If you changed the `fontFamilyValues` in the original Test App `UserSettings`, pass it to
         * [fontFamilies] to migrate the font family properly.
         */
        @ExperimentalReadiumApi
        public fun fromLegacyEpubSettings(
            context: Context,
            sharedPreferencesName: String = "org.readium.r2.settings",
            fontFamilies: List<String> = listOf(
                "Original",
                "PT Serif",
                "Roboto",
                "Source Sans Pro",
                "Vollkorn",
                "OpenDyslexic",
                "AccessibleDfA",
                "IA Writer Duospace"
            ),
        ): EpubPreferences {
            val sp: SharedPreferences =
                context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)

            val fontFamily = sp
                .takeIf { it.contains("fontFamily") }
                ?.getInt("fontFamily", 0)
                ?.let { fontFamilies.getOrNull(it) }
                ?.takeUnless { it == "Original" }
                ?.let { FontFamily(it) }

            val theme = sp
                .takeIf { sp.contains("appearance") }
                ?.getInt("appearance", 0)
                ?.let {
                    when (it) {
                        0 -> Theme.LIGHT
                        1 -> Theme.SEPIA
                        2 -> Theme.DARK
                        else -> null
                    }
                }

            val scroll = sp
                .takeIf { sp.contains("scroll") }
                ?.getBoolean("scroll", false)

            val colCount = sp
                .takeIf { sp.contains("colCount") }
                ?.getInt("colCount", 0)
                ?.let {
                    when (it) {
                        0 -> ColumnCount.AUTO
                        1 -> ColumnCount.ONE
                        2 -> ColumnCount.TWO
                        else -> null
                    }
                }

            val pageMargins = sp
                .takeIf { sp.contains("pageMargins") }
                ?.getFloat("pageMargins", 1.0f)
                ?.toDouble()

            val fontSize = sp
                .takeIf { sp.contains("fontSize") }
                ?.let { sp.getFloat("fontSize", 0f) }
                ?.toDouble()
                ?.let { it / 100 }

            val textAlign = sp
                .takeIf { sp.contains("textAlign") }
                ?.getInt("textAlign", 0)
                ?.let {
                    when (it) {
                        0 -> TextAlign.JUSTIFY
                        1 -> TextAlign.START
                        else -> null
                    }
                }

            val wordSpacing = sp
                .takeIf { sp.contains("wordSpacing") }
                ?.getFloat("wordSpacing", 0f)
                ?.toDouble()

            val letterSpacing = sp
                .takeIf { sp.contains("letterSpacing") }
                ?.getFloat("letterSpacing", 0f)
                ?.toDouble()
                ?.let { it * 2 }

            val lineHeight = sp
                .takeIf { sp.contains("lineHeight") }
                ?.getFloat("lineHeight", 1.2f)
                ?.toDouble()

            // Note that in the legacy preferences storage, "advanced settings" was incorrectly synonym to
            // "publisher styles", hence we don't need to flip the value.
            val publisherStyles = sp
                .takeIf { sp.contains("advancedSettings") }
                ?.getBoolean("advancedSettings", false)

            return EpubPreferences(
                fontFamily = fontFamily,
                theme = theme,
                scroll = scroll,
                columnCount = colCount,
                pageMargins = pageMargins,
                fontSize = fontSize,
                textAlign = textAlign,
                wordSpacing = wordSpacing,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
                publisherStyles = publisherStyles
            )
        }
    }
}
