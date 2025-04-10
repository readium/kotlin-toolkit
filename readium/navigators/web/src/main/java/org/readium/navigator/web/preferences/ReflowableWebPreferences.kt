/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import kotlinx.serialization.Serializable
import org.readium.navigator.common.Preferences
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Preferences for the Reflowable Web navigator.
 *
 * @param backgroundColor Default page background color.
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
 * @param horizontalMargins Factor applied to horizontal margins.
 * @param paragraphIndent Text indentation for paragraphs.
 * @param paragraphSpacing Vertical margins for paragraphs.
 * @param readingProgression Direction of the reading progression across resources.
 * @param scroll Indicates if the overflow of resources should be handled using scrolling
 *   instead of synthetic pagination.
 * @param textAlign Page text alignment.
 * @param textColor Default page text color.
 * @param textNormalization Normalize text styles to increase accessibility.
 * @param theme Reader theme.
 * @param verticalText Indicates whether the text should be laid out vertically. This is used
 *   for example with CJK languages. This setting is automatically derived from the language if
 *   no preference is given.
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
    val horizontalMargins: Double? = null,
    val paragraphIndent: Double? = null,
    val paragraphSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val textAlign: TextAlign? = null,
    val textColor: Color? = null,
    val textNormalization: Boolean? = null,
    val theme: Theme? = null,
    val verticalText: Boolean? = null,
    val wordSpacing: Double? = null,
) : Preferences<ReflowableWebPreferences> {

    init {
        require(columnCount == null || columnCount > 1)
        require(fontSize == null || fontSize >= 0)
        require(fontWeight == null || fontWeight in 0.0..2.5)
        require(letterSpacing == null || letterSpacing >= 0)
        require(horizontalMargins == null || horizontalMargins >= 0)
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
            horizontalMargins = other.horizontalMargins ?: horizontalMargins,
            paragraphIndent = other.paragraphIndent ?: paragraphIndent,
            paragraphSpacing = other.paragraphSpacing ?: paragraphSpacing,
            readingProgression = other.readingProgression ?: readingProgression,
            scroll = other.scroll ?: scroll,
            textAlign = other.textAlign ?: textAlign,
            textColor = other.textColor ?: textColor,
            textNormalization = other.textNormalization ?: textNormalization,
            theme = other.theme ?: theme,
            verticalText = other.verticalText ?: verticalText,
            wordSpacing = other.wordSpacing ?: wordSpacing
        )
}
