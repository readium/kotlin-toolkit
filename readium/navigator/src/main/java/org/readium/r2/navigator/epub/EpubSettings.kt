/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.content.Context
import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.epub.css.Layout.Stylesheets
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.Color
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.Spread
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language
import org.readium.r2.navigator.epub.css.Color as CssColor
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign

/**
 * EPUB navigator settings.
 *
 * There are two implementations, depending on the type of publications: [Reflowable] and [FixedLayout].
 */
@ExperimentalReadiumApi
sealed class EpubSettings : Configurable.Settings {

    /** Language of the publication content. */
    abstract val language: ValueSetting<Language?>
    /** Direction of the reading progression across resources. */
    abstract val readingProgression: EnumSetting<ReadingProgression>

    internal abstract fun update(metadata: Metadata, preferences: Preferences, defaults: Preferences = Preferences()): EpubSettings

    /**
     * EPUB navigator settings for fixed-layout publications.
     *
     * @param language Language of the publication content.
     * @param readingProgression Direction of the reading progression across resources.
     * @param spread Indicates the condition to be met for the publication to be rendered with a
     * synthetic spread (dual-page).
     */
    @ExperimentalReadiumApi
    data class FixedLayout(
        override val language: ValueSetting<Language?> = LANGUAGE,
        override val readingProgression: EnumSetting<ReadingProgression> = READING_PROGRESSION,
        val spread: EnumSetting<Spread> = SPREAD,
    ) : EpubSettings() {

        companion object {

            /** Language of the publication content. */
            val LANGUAGE: ValueSetting<Language?> = ValueSetting(
                key = Setting.LANGUAGE,
                value = null,
            )

            /** Direction of the reading progression across resources. */
            val READING_PROGRESSION: EnumSetting<ReadingProgression> = EnumSetting(
                key = Setting.READING_PROGRESSION,
                value = ReadingProgression.LTR,
                values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
            )

            /**
             * Indicates the condition to be met for the publication to be rendered with a
             * synthetic spread (dual-page).
             */
            val SPREAD: EnumSetting<Spread> = EnumSetting(
                key = Setting.SPREAD,
                value = Spread.NONE,
                // FIXME: Support Spread.AUTO and Spread.LANDSCAPE.
                values = listOf(Spread.NONE, Spread.BOTH),
            )
        }

        override fun update(metadata: Metadata, preferences: Preferences, defaults: Preferences): FixedLayout =
            copy(
                language = language.copyFirstValidValueFrom(preferences, defaults, fallback = LANGUAGE),
                readingProgression = readingProgression.copyFirstValidValueFrom(preferences, defaults, fallback = READING_PROGRESSION),
                spread = spread.copyFirstValidValueFrom(preferences, defaults, fallback = SPREAD),
            )
    }

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
    data class Reflowable(
        val backgroundColor: ColorSetting = BACKGROUND_COLOR,
        val columnCount: EnumSetting<ColumnCount>? = COLUMN_COUNT,
        val fontFamily: EnumSetting<FontFamily?> = FONT_FAMILY,
        val fontSize: PercentSetting = FONT_SIZE,
        val hyphens: ToggleSetting? = HYPHENS,
        val imageFilter: EnumSetting<ImageFilter>? = IMAGE_FILTER,
        override val language: ValueSetting<Language?> = LANGUAGE,
        val letterSpacing: PercentSetting? = LETTER_SPACING,
        val ligatures: ToggleSetting? = LIGATURES,
        val lineHeight: RangeSetting<Double> = LINE_HEIGHT,
        val pageMargins: RangeSetting<Double>? = PAGE_MARGINS,
        val paragraphIndent: PercentSetting? = PARAGRAPH_INDENT,
        val paragraphSpacing: PercentSetting = PARAGRAPH_SPACING,
        val publisherStyles: ToggleSetting = PUBLISHER_STYLES,
        override val readingProgression: EnumSetting<ReadingProgression> = READING_PROGRESSION,
        val scroll: ToggleSetting? = SCROLL,
        val textAlign: EnumSetting<TextAlign>? = TEXT_ALIGN,
        val textColor: ColorSetting = TEXT_COLOR,
        val textNormalization: EnumSetting<TextNormalization> = TEXT_NORMALIZATION,
        val theme: EnumSetting<Theme> = THEME,
        val typeScale: RangeSetting<Double> = TYPE_SCALE,
        val verticalText: ToggleSetting = VERTICAL_TEXT,
        val wordSpacing: PercentSetting? = WORD_SPACING,

        internal val layout: Layout = Layout()
    ) : EpubSettings() {

        constructor(
            fontFamilies: List<FontFamily> = emptyList(),
            namedColors: Map<String, Int> = emptyMap()
        ) : this(
            backgroundColor = BACKGROUND_COLOR.copy(
                coder = Color.Coder(namedColors)
            ),
            fontFamily = FONT_FAMILY.copy(
                coder = FontFamily.Coder(fontFamilies),
                values = listOf(null) + fontFamilies
            ),
            textColor = TEXT_COLOR.copy(
                coder = Color.Coder(namedColors)
            ),
        )

        companion object {

            /** Default page background color. */
            val BACKGROUND_COLOR: ColorSetting = ColorSetting(
                key = Setting.BACKGROUND_COLOR,
                value = Color.AUTO
            )

            /** Number of columns to display (one-page view or two-page spread). */
            val COLUMN_COUNT: EnumSetting<ColumnCount> = EnumSetting(
                key = Setting.COLUMN_COUNT,
                value = ColumnCount.AUTO,
                values = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
            )

            /** Default typeface for the text. */
            val FONT_FAMILY: EnumSetting<FontFamily?> = EnumSetting(
                key = Setting.FONT_FAMILY,
                coder = FontFamily.Coder(),
                value = null,
                values = listOf(null),
                formatValue = { it?.name }
            )

            /** Base text font size. */
            val FONT_SIZE: PercentSetting = PercentSetting(
                key = Setting.FONT_SIZE,
                value = 1.0,
                range = 0.4..5.0
            )

            /** Enable hyphenation. */
            val HYPHENS: ToggleSetting = ToggleSetting(
                key = Setting.HYPHENS,
                value = true,
                activator = RequiresPublisherStylesDisabled
            )

            /** Filter applied to images in dark theme. */
            val IMAGE_FILTER: EnumSetting<ImageFilter> = EnumSetting(
                key = Setting.IMAGE_FILTER,
                value = ImageFilter.NONE,
                values = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT)
            )

            /** Language of the publication content. */
            val LANGUAGE: ValueSetting<Language?> = ValueSetting(
                key = Setting.LANGUAGE,
                value = null,
            )

            /** Space between letters. */
            val LETTER_SPACING: PercentSetting = PercentSetting(
                key = Setting.LETTER_SPACING,
                value = 0.0,
                activator = RequiresPublisherStylesDisabled
            )

            /** Enable ligatures in Arabic. */
            val LIGATURES: ToggleSetting = ToggleSetting(
                key = Setting.LIGATURES,
                value = true,
                activator = RequiresPublisherStylesDisabled
            )

            /** Leading line height. */
            val LINE_HEIGHT: RangeSetting<Double> = RangeSetting(
                key = Setting.LINE_HEIGHT,
                value = 1.2,
                range = 1.0..2.0,
                activator = RequiresPublisherStylesDisabled
            )

            /** Factor applied to horizontal margins. */
            val PAGE_MARGINS: RangeSetting<Double> = RangeSetting(
                key = Setting.PAGE_MARGINS,
                value = 1.0,
                range = 0.5..4.0
            )

            /** Text indentation for paragraphs. */
            val PARAGRAPH_INDENT: PercentSetting = PercentSetting(
                key = Setting.PARAGRAPH_INDENT,
                value = 0.0,
                range = 0.0..3.0,
                suggestedIncrement = 0.2,
                activator = RequiresPublisherStylesDisabled
            )

            /** Vertical margins for paragraphs. */
            val PARAGRAPH_SPACING: PercentSetting = PercentSetting(
                key = Setting.PARAGRAPH_SPACING,
                value = 0.0,
                range = 0.0..2.0,
                activator = RequiresPublisherStylesDisabled
            )

            /**
             * Indicates whether the original publisher styles should be observed.
             *
             * Many settings require this to be off.
             */
            val PUBLISHER_STYLES: ToggleSetting = ToggleSetting(
                key = Setting.PUBLISHER_STYLES,
                value = true,
            )

            /** Direction of the reading progression across resources. */
            val READING_PROGRESSION: EnumSetting<ReadingProgression> = EnumSetting(
                key = Setting.READING_PROGRESSION,
                value = ReadingProgression.LTR,
                values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
            )

            /**
             * Indicates if the overflow of resources should be handled using scrolling instead
             * of synthetic pagination.
             */
            val SCROLL: ToggleSetting = ToggleSetting(
                key = Setting.SCROLL,
                value = false,
            )

            /** Page text alignment. */
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

            /** Default page text color. */
            val TEXT_COLOR: ColorSetting = ColorSetting(
                key = Setting.TEXT_COLOR,
                value = Color.AUTO
            )

            /** Normalize font style, weight and variants using a specific strategy. */
            val TEXT_NORMALIZATION: EnumSetting<TextNormalization> = EnumSetting(
                key = Setting.TEXT_NORMALIZATION,
                value = TextNormalization.NONE,
                values = listOf(TextNormalization.NONE, TextNormalization.BOLD, TextNormalization.ACCESSIBILITY)
            )

            /** Reader theme. */
            val THEME: EnumSetting<Theme> = EnumSetting(
                key = Setting.THEME,
                value = Theme.LIGHT,
                values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
            )

            /**
             * Scale applied to all element font sizes.
             *
             * See https://readium.org/readium-css/docs/CSS19-api.html#typography
             */
            val TYPE_SCALE: RangeSetting<Double> = RangeSetting(
                key = Setting.TYPE_SCALE,
                value = 1.2,
                range = 1.0..2.0,
                suggestedSteps = listOf(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
                activator = RequiresPublisherStylesDisabled
            )

            /**
             * Indicates whether the text should be laid out vertically. This is used for example
             * with CJK languages.
             *
             * This setting is automatically derived from the language if no preference is given.
             */
            val VERTICAL_TEXT: ToggleSetting = ToggleSetting(
                key = Setting.VERTICAL_TEXT,
                value = false,
            )

            /** Space between words. */
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

        override fun update(metadata: Metadata, preferences: Preferences, defaults: Preferences): Reflowable {
            val language = language.copyFirstValidValueFrom(preferences, defaults, fallback = metadata.language)

            val layout = Layout.from(
                language = language.value,
                hasMultipleLanguages =
                    if (language.value != null) false
                    else metadata.languages.size > 1,
                readingProgression =  (preferences[readingProgression] ?: defaults[readingProgression])
                    .takeIf { it == ReadingProgression.LTR || it == ReadingProgression.RTL }
                    ?: metadata.readingProgression,
                verticalText = (preferences[verticalText] ?: defaults[verticalText])
            )
            val isVerticalText = (layout.stylesheets == Stylesheets.CjkVertical)

            val scroll = if (isVerticalText) null
                else (scroll ?: SCROLL).copyFirstValidValueFrom(preferences, defaults, fallback = SCROLL)

            val isPaginated = (scroll?.value == false)

            return copy(
                backgroundColor = backgroundColor.copyFirstValidValueFrom(preferences, defaults, fallback = BACKGROUND_COLOR),
                columnCount = if (isPaginated) (columnCount ?: COLUMN_COUNT).copyFirstValidValueFrom(preferences, defaults, fallback = COLUMN_COUNT)
                    else null,
                fontFamily = fontFamily.copyFirstValidValueFrom(preferences, defaults, fallback = FONT_FAMILY),
                fontSize = fontSize.copyFirstValidValueFrom(preferences, defaults, fallback = FONT_SIZE),
                hyphens = if (layout.stylesheets != Stylesheets.Default) null
                    else (hyphens ?: HYPHENS).copyFirstValidValueFrom(preferences, defaults, fallback = HYPHENS),
                imageFilter = if (preferences[theme] != Theme.DARK) null
                    else (imageFilter ?: IMAGE_FILTER).copyFirstValidValueFrom(preferences, defaults, fallback = IMAGE_FILTER),
                language = language,
                letterSpacing = if (layout.stylesheets != Stylesheets.Default) null
                    else (letterSpacing ?: LETTER_SPACING).copyFirstValidValueFrom(preferences, defaults, fallback = LETTER_SPACING),
                ligatures = if (layout.stylesheets != Stylesheets.Rtl) null
                    else (ligatures ?: LIGATURES).copyFirstValidValueFrom(preferences, defaults, fallback = LIGATURES),
                lineHeight = lineHeight.copyFirstValidValueFrom(preferences, defaults, fallback = LINE_HEIGHT),
                pageMargins = if (isPaginated) (pageMargins ?: PAGE_MARGINS).copyFirstValidValueFrom(preferences, defaults, fallback = PAGE_MARGINS)
                    else null,
                paragraphIndent = if (layout.stylesheets == Stylesheets.CjkVertical || layout.stylesheets == Stylesheets.CjkHorizontal) null
                    else (paragraphIndent ?: PARAGRAPH_INDENT).copyFirstValidValueFrom(preferences, defaults, fallback = PARAGRAPH_INDENT),
                paragraphSpacing = paragraphSpacing.copyFirstValidValueFrom(preferences, defaults, fallback = PARAGRAPH_SPACING),
                publisherStyles = publisherStyles.copyFirstValidValueFrom(preferences, defaults, fallback = PUBLISHER_STYLES),
                readingProgression = READING_PROGRESSION.copy(value = layout.readingProgression),
                scroll = scroll,
                textAlign = if (layout.stylesheets == Stylesheets.CjkVertical || layout.stylesheets == Stylesheets.CjkHorizontal) null
                    else (textAlign ?: TEXT_ALIGN).copyFirstValidValueFrom(preferences, defaults, fallback = TEXT_ALIGN),
                textColor = textColor.copyFirstValidValueFrom( preferences, defaults, fallback = TEXT_COLOR),
                textNormalization = textNormalization.copyFirstValidValueFrom(preferences, defaults, fallback = TEXT_NORMALIZATION),
                theme = theme.copyFirstValidValueFrom(preferences, defaults, fallback = THEME),
                typeScale = typeScale.copyFirstValidValueFrom( preferences, defaults, fallback = TYPE_SCALE),
                verticalText = VERTICAL_TEXT.copy(value = isVerticalText),
                wordSpacing = if (layout.stylesheets != Stylesheets.Default) null
                    else (wordSpacing ?: WORD_SPACING).copyFirstValidValueFrom( preferences, defaults, fallback = WORD_SPACING),
                layout = layout
            )
        }
    }
}

@ExperimentalReadiumApi
fun ReadiumCss.update(settings: EpubSettings): ReadiumCss {
    if (settings !is EpubSettings.Reflowable) return this

    return with(settings) {
        copy(
            layout = settings.layout,
            userProperties = userProperties.copy(
                view = when (scroll?.value) {
                    false -> View.PAGED
                    null, true -> View.SCROLL
                },
                colCount = when (columnCount?.value) {
                    ColumnCount.ONE -> ColCount.ONE
                    ColumnCount.TWO -> ColCount.TWO
                    else -> ColCount.AUTO
                },
                pageMargins = pageMargins?.value,
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
                fontOverride = (fontFamily.value != null || (textNormalization.value == TextNormalization.ACCESSIBILITY)),
                fontFamily = fontFamily.value?.toCss(),
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Relative.Percent(it) },
                advancedSettings = !publisherStyles.value,
                typeScale = typeScale.value,
                textAlign = when (textAlign?.value) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                    else -> null
                },
                lineHeight = Either(lineHeight.value),
                paraSpacing = Length.Relative.Rem(paragraphSpacing.value),
                paraIndent = paragraphIndent?.run { Length.Relative.Rem(value) },
                wordSpacing = wordSpacing?.run { Length.Relative.Rem(value) },
                letterSpacing = letterSpacing?.run { Length.Relative.Rem(value / 2) },
                bodyHyphens = hyphens?.run { if (value) Hyphens.AUTO else Hyphens.NONE },
                ligatures = ligatures?.run { if (value) Ligatures.COMMON else Ligatures.NONE },
                a11yNormalize = textNormalization.value == TextNormalization.ACCESSIBILITY,
                overrides = mapOf(
                    "font-weight" to if (textNormalization.value == TextNormalization.BOLD) "bold" else null
                )
            )
        )
    }
}

private fun FontFamily.toCss(): List<String> = buildList {
    add(name)
    alternate?.let { addAll(it.toCss()) }
}

/**
 * Loads the preferences from the legacy EPUB settings stored in the [SharedPreferences] with
 * given [sharedPreferencesName].
 *
 * This can be used to migrate the legacy settings to the new [Preferences] format.
 *
 * If you changed the `fontFamilyValues` in the original Test App `UserSettings`, pass it to
 * [fontFamilies] to migrate the font family properly.
 */
@ExperimentalReadiumApi
fun Preferences.Companion.fromLegacyEpubSettings(
    context: Context,
    sharedPreferencesName: String = "org.readium.r2.settings",
    fontFamilies: List<String> = listOf(
        "Original", "PT Serif", "Roboto", "Source Sans Pro", "Vollkorn", "OpenDyslexic",
        "AccessibleDfA", "IA Writer Duospace"
    )
): Preferences {
    val sp = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
    return Preferences {
        if (sp.contains("appearance")) {
            val appearance = sp.getInt("appearance", 0)
            set(EpubSettings.Reflowable.THEME, when (appearance) {
                0 -> Theme.LIGHT
                1 -> Theme.SEPIA
                2 -> Theme.DARK
                else -> null
            })
        }

        if (sp.contains("scroll")) {
            set(EpubSettings.Reflowable.SCROLL, sp.getBoolean("scroll", false))
        }

        if (sp.contains("colCount")) {
            val colCount = sp.getInt("colCount", 0)
            set(EpubSettings.Reflowable.COLUMN_COUNT, when (colCount) {
                0 -> ColumnCount.AUTO
                1 -> ColumnCount.ONE
                2 -> ColumnCount.TWO
                else -> null
            })
        }

        if (sp.contains("pageMargins")) {
            val pageMargins = sp.getFloat("pageMargins", 1.0f).toDouble()
            set(EpubSettings.Reflowable.PAGE_MARGINS, pageMargins)
        }

        if (sp.contains("fontFamily")) {
            val index = sp.getInt("fontFamily", 0)
            val fontFamily = fontFamilies.getOrNull(index)
            if (fontFamily != null && fontFamily != "Original") {
                val setting = EpubSettings.Reflowable.FONT_FAMILY.copy(
                    values = fontFamilies
                        .mapNotNull {
                            if (it == "Original") null
                            else FontFamily(it)
                        }
                )
                set(setting, FontFamily(fontFamily))
            }
        }

        if (sp.contains("fontSize")) {
            val fontSize = (sp.getFloat("fontSize", 0f) / 100).toDouble()
            set(EpubSettings.Reflowable.FONT_SIZE, fontSize)
        }

        if (sp.contains("textAlign")) {
            val textAlign = sp.getInt("textAlign", 0)
            set(EpubSettings.Reflowable.TEXT_ALIGN, when (textAlign) {
                0 -> TextAlign.JUSTIFY
                1 -> TextAlign.START
                else -> null
            })
        }

        if (sp.contains("wordSpacing")) {
            val wordSpacing = sp.getFloat("wordSpacing", 0f).toDouble()
            set(EpubSettings.Reflowable.WORD_SPACING, wordSpacing)
        }

        if (sp.contains("letterSpacing")) {
            val letterSpacing = sp.getFloat("letterSpacing", 0f).toDouble() * 2
            set(EpubSettings.Reflowable.LETTER_SPACING, letterSpacing)
        }

        if (sp.contains("lineHeight")) {
            val lineHeight = sp.getFloat("lineHeight", 1.2f).toDouble()
            set(EpubSettings.Reflowable.LINE_HEIGHT, lineHeight)
        }

        if (sp.contains("advancedSettings")) {
            val advancedSettings = sp.getBoolean("advancedSettings", false)
            set(EpubSettings.Reflowable.PUBLISHER_STYLES, !advancedSettings)
        }
    }
}
