/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.Color
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language
import org.readium.r2.navigator.epub.css.Color as CssColor
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign

@ExperimentalReadiumApi
sealed class EpubSettings : Configurable.Settings {

    internal abstract fun update(preferences: Preferences, defaults: Preferences = Preferences()): EpubSettings

    @ExperimentalReadiumApi
    data class Reflowable(
        val backgroundColor: ColorSetting = BACKGROUND_COLOR,
        val columnCount: EnumSetting<ColumnCount>? = COLUMN_COUNT,
        val font: EnumSetting<Font> = FONT,
        val fontSize: PercentSetting = FONT_SIZE,
        val hyphens: ToggleSetting = HYPHENS,
        val imageFilter: EnumSetting<ImageFilter>? = null, // requires Dark theme
        val language: ValueSetting<Language?> = LANGUAGE,
        val letterSpacing: PercentSetting = LETTER_SPACING,
        val ligatures: ToggleSetting = LIGATURES,
        val lineHeight: RangeSetting<Double> = LINE_HEIGHT,
        val normalizedText: ToggleSetting = NORMALIZED_TEXT,
        val overflow: EnumSetting<Overflow> = OVERFLOW,
        val pageMargins: RangeSetting<Double> = PAGE_MARGINS,
        val paragraphIndent: PercentSetting = PARAGRAPH_INDENT,
        val paragraphSpacing: PercentSetting = PARAGRAPH_SPACING,
        val publisherStyles: ToggleSetting = PUBLISHER_STYLES,
        val readingProgression: EnumSetting<ReadingProgression> = READING_PROGRESSION,
        val textAlign: EnumSetting<TextAlign> = TEXT_ALIGN,
        val textColor: ColorSetting = TEXT_COLOR,
        val theme: EnumSetting<Theme> = THEME,
        val typeScale: RangeSetting<Double> = TYPE_SCALE,
        val wordSpacing: PercentSetting = WORD_SPACING,
    ) : EpubSettings() {
        constructor(
            fonts: List<Font> = emptyList(),
            namedColors: Map<String, Int> = emptyMap()
        ) : this(
            backgroundColor = BACKGROUND_COLOR.copy(
                coder = Color.Coder(namedColors)
            ),
            font = FONT.copy(
                coder = Font.Coder(listOf(Font.ORIGINAL) + fonts),
                values = listOf(Font.ORIGINAL) + fonts
            ),
            textColor = TEXT_COLOR.copy(
                coder = Color.Coder(namedColors)
            ),
        )

        companion object {

            val BACKGROUND_COLOR: ColorSetting = ColorSetting(
                key = Setting.BACKGROUND_COLOR,
                value = Color.AUTO
            )

            val COLUMN_COUNT: EnumSetting<ColumnCount> = EnumSetting(
                key = Setting.COLUMN_COUNT,
                value = ColumnCount.AUTO,
                values = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
            )

            val FONT: EnumSetting<Font> = EnumSetting(
                key = Setting.FONT,
                coder = Font.Coder(listOf(Font.ORIGINAL)),
                value = Font.ORIGINAL,
                values = listOf(Font.ORIGINAL),
                label = { it.name }
            )

            val FONT_SIZE: PercentSetting = PercentSetting(
                key = Setting.FONT_SIZE,
                value = 1.0,
                range = 0.4..5.0
            )

            val HYPHENS: ToggleSetting = ToggleSetting(
                key = Setting.HYPHENS,
                value = true,
                activator = RequiresPublisherStylesDisabled
            )

            val IMAGE_FILTER: EnumSetting<ImageFilter> = EnumSetting(
                key = Setting.IMAGE_FILTER,
                value = ImageFilter.NONE,
                values = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT)
            )

            val LANGUAGE: ValueSetting<Language?> = ValueSetting(
                key = Setting.LANGUAGE,
                value = null,
            )

            val LETTER_SPACING: PercentSetting = PercentSetting(
                key = Setting.LETTER_SPACING,
                value = 0.0,
                activator = RequiresPublisherStylesDisabled
            )

            val LIGATURES: ToggleSetting = ToggleSetting(
                key = Setting.LIGATURES,
                value = true,
                activator = RequiresPublisherStylesDisabled
            )

            val LINE_HEIGHT: RangeSetting<Double> = RangeSetting(
                key = Setting.LINE_HEIGHT,
                value = 1.2,
                range = 1.0..2.0,
                activator = RequiresPublisherStylesDisabled
            )

            val NORMALIZED_TEXT: ToggleSetting = ToggleSetting(
                key = Setting.NORMALIZED_TEXT,
                value = false,
            )

            val OVERFLOW: EnumSetting<Overflow> = EnumSetting(
                key = Setting.OVERFLOW,
                value = Overflow.PAGINATED,
                values = listOf(Overflow.PAGINATED, Overflow.SCROLLED)
            )

            val PAGE_MARGINS: RangeSetting<Double> = RangeSetting(
                key = Setting.PAGE_MARGINS,
                value = 1.0,
                range = 0.5..2.0
            )

            val PARAGRAPH_INDENT: PercentSetting = PercentSetting(
                key = Setting.PARAGRAPH_INDENT,
                value = 0.0,
                range = 0.0..3.0,
                suggestedIncrement = 0.2,
                activator = RequiresPublisherStylesDisabled
            )

            val PARAGRAPH_SPACING: PercentSetting = PercentSetting(
                key = Setting.PARAGRAPH_SPACING,
                value = 0.0,
                range = 0.0..2.0,
                activator = RequiresPublisherStylesDisabled
            )

            val PUBLISHER_STYLES: ToggleSetting = ToggleSetting(
                key = Setting.PUBLISHER_STYLES,
                value = true,
            )

            val READING_PROGRESSION: EnumSetting<ReadingProgression> = EnumSetting(
                key = Setting.READING_PROGRESSION,
                value = ReadingProgression.AUTO,
                values = listOf(ReadingProgression.AUTO, ReadingProgression.LTR, ReadingProgression.RTL)
            )

            val TEXT_ALIGN: EnumSetting<TextAlign> = EnumSetting(
                key = Setting.TEXT_ALIGN,
                value = TextAlign.START,
                values = listOf(
                    TextAlign.START,
                    TextAlign.LEFT,
                    TextAlign.RIGHT,
                    TextAlign.JUSTIFY
                ),
                activator = RequiresPublisherStylesDisabled
            )

            val TEXT_COLOR: ColorSetting = ColorSetting(
                key = Setting.TEXT_COLOR,
                value = Color.AUTO
            )

            val THEME: EnumSetting<Theme> = EnumSetting(
                key = Setting.THEME,
                value = Theme.LIGHT,
                values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
            )

            // https://readium.org/readium-css/docs/CSS19-api.html#typography
            val TYPE_SCALE: RangeSetting<Double> = RangeSetting(
                key = Setting.TYPE_SCALE,
                value = 1.2,
                range = 1.0..2.0,
                suggestedSteps = listOf(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
                activator = RequiresPublisherStylesDisabled
            )

            val WORD_SPACING: PercentSetting = PercentSetting(
                key = Setting.WORD_SPACING,
                value = 0.0,
                activator = RequiresPublisherStylesDisabled
            )

            private object RequiresPublisherStylesDisabled : SettingActivator {
                override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                    preferences[PUBLISHER_STYLES] == false

                override fun activateInPreferences(preferences: MutablePreferences) {
                    preferences[PUBLISHER_STYLES] = false
                }
            }
        }

        override fun update(preferences: Preferences, defaults: Preferences): Reflowable =
            copy(
                backgroundColor = backgroundColor.copyFirstValidValueFrom(preferences, defaults, fallback = BACKGROUND_COLOR),
                columnCount = if (preferences[overflow] == Overflow.SCROLLED) null
                    else (columnCount ?: COLUMN_COUNT).copyFirstValidValueFrom(preferences, defaults, fallback = COLUMN_COUNT),
                font = font.copyFirstValidValueFrom(preferences, defaults, fallback = FONT),
                fontSize = fontSize.copyFirstValidValueFrom(preferences, defaults, fallback = FONT_SIZE),
                hyphens = hyphens.copyFirstValidValueFrom(preferences, defaults, fallback = HYPHENS),
                imageFilter = if (preferences[theme] != Theme.DARK) null
                    else (imageFilter ?: IMAGE_FILTER).copyFirstValidValueFrom(preferences, defaults, fallback = IMAGE_FILTER),
                language = language.copyFirstValidValueFrom(preferences, defaults, fallback = LANGUAGE),
                letterSpacing = letterSpacing.copyFirstValidValueFrom(preferences, defaults, fallback = LETTER_SPACING),
                ligatures = ligatures.copyFirstValidValueFrom(preferences, defaults, fallback = LIGATURES),
                lineHeight = lineHeight.copyFirstValidValueFrom(preferences, defaults, fallback = LINE_HEIGHT),
                normalizedText = normalizedText.copyFirstValidValueFrom(preferences, defaults, fallback = NORMALIZED_TEXT),
                overflow = overflow.copyFirstValidValueFrom(preferences, defaults, fallback = OVERFLOW),
                pageMargins = pageMargins.copyFirstValidValueFrom(preferences, defaults, fallback = PAGE_MARGINS),
                paragraphIndent = paragraphIndent.copyFirstValidValueFrom(preferences, defaults, fallback = PARAGRAPH_INDENT),
                paragraphSpacing = paragraphSpacing.copyFirstValidValueFrom(preferences, defaults, fallback = PARAGRAPH_SPACING),
                publisherStyles = publisherStyles.copyFirstValidValueFrom(preferences, defaults, fallback = PUBLISHER_STYLES),
                readingProgression = readingProgression.copyFirstValidValueFrom(preferences, defaults, fallback = READING_PROGRESSION),
                textAlign = textAlign.copyFirstValidValueFrom(preferences, defaults, fallback = TEXT_ALIGN),
                textColor = textColor.copyFirstValidValueFrom( preferences, defaults, fallback = TEXT_COLOR),
                theme = theme.copyFirstValidValueFrom(preferences, defaults, fallback = THEME),
                typeScale = typeScale.copyFirstValidValueFrom( preferences, defaults, fallback = TYPE_SCALE),
                wordSpacing = wordSpacing.copyFirstValidValueFrom( preferences, defaults, fallback = WORD_SPACING),
            )
    }
}

@ExperimentalReadiumApi
fun ReadiumCss.update(
    settings: EpubSettings.Reflowable,
    metadata: Metadata,
): ReadiumCss =
    with(settings) {
        copy(
            layout = Layout.from(
                language = settings.language.value ?: metadata.language,
                hasMultipleLanguages =
                    if (settings.language.value != null) false
                    else metadata.languages.size > 1,
                readingProgression = settings.readingProgression.value.takeIf { it != ReadingProgression.AUTO }
                    ?: metadata.readingProgression
            ),
            userProperties = userProperties.copy(
                view = when (overflow.value) {
                    Overflow.AUTO -> null
                    Overflow.PAGINATED -> View.PAGED
                    Overflow.SCROLLED -> View.SCROLL
                },
                colCount = when (columnCount?.value) {
                    ColumnCount.ONE -> ColCount.ONE
                    ColumnCount.TWO -> ColCount.TWO
                    else -> ColCount.AUTO
                },
                pageMargins = pageMargins.value,
                appearance = when (theme.value) {
                    Theme.LIGHT -> null
                    Theme.DARK -> Appearance.NIGHT
                    Theme.SEPIA -> Appearance.SEPIA
                },
                darkenImages = imageFilter?.value?.let { it == ImageFilter.DARKEN },
                invertImages = imageFilter?.value?.let { it == ImageFilter.INVERT },
                textColor = textColor.value
                    .takeIf { it != Color.AUTO }
                    ?.let { CssColor.int(it.int) },
                backgroundColor = backgroundColor.value
                    .takeIf { it != Color.AUTO }
                    ?.let { CssColor.int(it.int) },
                fontOverride = (font.value != Font.ORIGINAL || normalizedText.value),
                fontFamily = font.value.name?.let { listOf(it) },
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Relative.Percent(it) },
                advancedSettings = !publisherStyles.value,
                typeScale = typeScale.value,
                textAlign = when (textAlign.value) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                },
                lineHeight = Either(lineHeight.value),
                paraSpacing = Length.Relative.Rem(paragraphSpacing.value),
                paraIndent = Length.Relative.Rem(paragraphIndent.value),
                wordSpacing = Length.Relative.Rem(wordSpacing.value),
                letterSpacing = Length.Relative.Rem(letterSpacing.value / 2),
                bodyHyphens = if (hyphens.value) Hyphens.AUTO else Hyphens.NONE,
                ligatures = if (ligatures.value) Ligatures.COMMON else Ligatures.NONE,
                a11yNormalize = normalizedText.value,
            )
        )
    }