/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.preferences

import androidx.core.graphics.toColorInt
import kotlinx.serialization.Serializable
import org.readium.navigator.common.Preferences
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Preferences for the Reflowable Web navigator.
 *
 * @param backgroundColor Page background color.
 * @param columnCount Number of reflowable columns to display.
 * @param fontFamily Default typeface for the text.
 * @param fontSize Base text font size.
 * @param fontWeight Default boldness for the text.
 * @param hyphens Enable hyphenation.
 * @param imageFilter Filter applied to images in dark theme.
 * @param language Language of the publication content.
 * @param letterSpacing Space between letters.
 * @param ligatures Enable ligatures in Arabic.
 * @param lineHeight Leading line height.
 * @param linkColor Link color.
 * @param maximalLineLength Optional maximal line length
 * @param minimalLineLength Optional minimal line length
 * @param minMargins Factor applied to horizontal margins.
 * @param optimalLineLength Optimal line length
 * @param overridePublisherColors If color preferences should be applied only as a fallback or
 *   override publisher colors.
 * @param paragraphIndent Text indentation for paragraphs.
 * @param paragraphSpacing Vertical margins for paragraphs.
 * @param readingProgression Direction of the reading progression across resources.
 * @param scroll Indicates if the overflow of resources should be handled using scrolling
 *   instead of synthetic pagination.
 * @param textAlign Page text alignment.
 * @param textColor Page text color.
 * @param textNormalization Normalize text styles to increase accessibility.
 * @param verticalText Indicates whether the text should be laid out vertically. This is used
 *   for example with CJK languages. This setting is automatically derived from the language if
 *   no preference is given.
 *   @param visitedColor Color for visited links.
 * @param wordSpacing Space between words.
 */
@Serializable
@ExperimentalReadiumApi
public data class ReflowableWebPreferences(
    val backgroundColor: Color? = null,
    val columnCount: Int? = null,
    val fontFamily: FontFamily? = null,
    val fontSize: Double? = null,
    val fontWeight: Double? = null,
    val hyphens: Boolean? = null,
    val imageFilter: ImageFilter? = null,
    val language: Language? = null,
    val letterSpacing: Double? = null,
    val ligatures: Boolean? = null,
    val lineHeight: Double? = null,
    val linkColor: Color? = null,
    val maximalLineLength: Double? = null,
    val minimalLineLength: Double? = null,
    val minMargins: Double? = null,
    val optimalLineLength: Double? = null,
    val overridePublisherColors: Boolean? = null,
    val paragraphIndent: Double? = null,
    val paragraphSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val textAlign: TextAlign? = null,
    val textColor: Color? = null,
    val textNormalization: Boolean? = null,
    val verticalText: Boolean? = null,
    val visitedColor: Color? = null,
    val wordSpacing: Double? = null,
) : Preferences<ReflowableWebPreferences> {

    init {
        require(columnCount == null || columnCount >= 1)
        require(fontSize == null || fontSize >= 0)
        require(fontWeight == null || fontWeight in 0.0..2.5)
        require(letterSpacing == null || letterSpacing >= 0)
        require(maximalLineLength == null || maximalLineLength > 0)
        require(minimalLineLength == null || minimalLineLength >= 0)
        require(optimalLineLength == null || optimalLineLength > 0)
        require(minMargins == null || minMargins >= 0)
        require(paragraphSpacing == null || paragraphSpacing >= 0)
        require(wordSpacing == null || wordSpacing >= 0)
    }

    @OptIn(ExperimentalReadiumApi::class)
    override operator fun plus(other: ReflowableWebPreferences): ReflowableWebPreferences =
        ReflowableWebPreferences(
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
            linkColor = other.linkColor ?: linkColor,
            maximalLineLength = other.maximalLineLength ?: maximalLineLength,
            minimalLineLength = other.minimalLineLength ?: minimalLineLength,
            minMargins = other.minMargins ?: minMargins,
            optimalLineLength = other.optimalLineLength ?: optimalLineLength,
            overridePublisherColors = other.overridePublisherColors ?: overridePublisherColors,
            paragraphIndent = other.paragraphIndent ?: paragraphIndent,
            paragraphSpacing = other.paragraphSpacing ?: paragraphSpacing,
            readingProgression = other.readingProgression ?: readingProgression,
            scroll = other.scroll ?: scroll,
            textAlign = other.textAlign ?: textAlign,
            textColor = other.textColor ?: textColor,
            textNormalization = other.textNormalization ?: textNormalization,
            verticalText = other.verticalText ?: verticalText,
            visitedColor = other.visitedColor ?: visitedColor,
            wordSpacing = other.wordSpacing ?: wordSpacing
        )

    public companion object {

        // https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-day_mode.css
        public val LightTheme: ReflowableWebPreferences = ReflowableWebPreferences(
            textColor = Color("#121212".toColorInt()),
            backgroundColor = Color("#FFFFFF".toColorInt()),
            linkColor = Color("#0000EE".toColorInt()),
            visitedColor = Color("#551A8B".toColorInt()),
            overridePublisherColors = false
        )

        // https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-sepia_mode.css
        public val SepiaTheme: ReflowableWebPreferences = ReflowableWebPreferences(
            textColor = Color("#121212".toColorInt()),
            backgroundColor = Color("#faf4e8".toColorInt()),
            linkColor = Color("#0000EE".toColorInt()),
            visitedColor = Color("#551A8B".toColorInt()),
            overridePublisherColors = true
        )

        // https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-night_mode.css
        public val DarkTheme: ReflowableWebPreferences = ReflowableWebPreferences(
            textColor = Color("#FEFEFE".toColorInt()),
            backgroundColor = Color("#000000".toColorInt()),
            linkColor = Color("#63caff".toColorInt()),
            visitedColor = Color("#0099E5".toColorInt()),
            overridePublisherColors = true
        )
    }
}
