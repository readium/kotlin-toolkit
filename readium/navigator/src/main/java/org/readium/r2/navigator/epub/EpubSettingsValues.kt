/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
sealed class EpubSettingsValues {
    /** Language of the publication content. */

    abstract val language: Language?

    /** Direction of the reading progression across resources. */
    abstract val readingProgression: ReadingProgression

    /**
     * EPUB navigator settings for fixed-layout publications.
     *
     * @param language Language of the publication content.
     * @param readingProgression Direction of the reading progression across resources.
     * @param spread Indicates the condition to be met for the publication to be rendered with a
     * synthetic spread (dual-page).
     */
    @ExperimentalReadiumApi
    data class FixedLayout internal constructor(
        override val language: Language?,
        override val readingProgression: ReadingProgression,
        val spread: Presentation.Spread,
    ) : EpubSettingsValues()

    /**
     * EPUB navigator settings for reflowable publications.
     *
     * @param backgroundColor Default page background color.
     * @param columnCount Number of columns to display (one-page view or two-page spread).
     * @param fontFamily Default typeface for the text.
     * @param fontSize Base text font size.
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
     * Many settings require this to be off.
     * @param readingProgression Direction of the reading progression across resources.
     * @param scroll Indicates if the overflow of resources should be handled using scrolling
     * instead of synthetic pagination.
     * @param textAlign Page text alignment.
     * @param textColor Default page text color.
     * @param textNormalization Normalize font style, weight and variants using a specific strategy.
     * @param theme Reader theme.
     * @param typeScale Scale applied to all element font sizes.
     * @param verticalText Indicates whether the text should be laid out vertically. This is used
     * for example with CJK languages. This setting is automatically derived from the language if
     * no preference is given.
     * @param wordSpacing Space between words.
     */
    @ExperimentalReadiumApi
    data class Reflowable internal constructor(
        val backgroundColor: Color,
        val columnCount: ColumnCount,
        val fontFamily: FontFamily?,
        val fontSize: Double,
        val hyphens: Boolean,
        val imageFilter: ImageFilter,
        override val language: Language?,
        val letterSpacing: Double,
        val ligatures: Boolean,
        val lineHeight: Double,
        val pageMargins: Double,
        val paragraphIndent: Double,
        val paragraphSpacing: Double,
        val publisherStyles: Boolean,
        override val readingProgression: ReadingProgression,
        val scroll: Boolean,
        val textAlign: TextAlign,
        val textColor: Color,
        val textNormalization: TextNormalization,
        val theme: Theme,
        val typeScale: Double,
        val verticalText: Boolean,
        val wordSpacing: Double
    ) : EpubSettingsValues()
}
