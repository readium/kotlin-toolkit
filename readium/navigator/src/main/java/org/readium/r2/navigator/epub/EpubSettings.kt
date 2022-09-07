/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.content.Context
import org.readium.r2.navigator.epub.EpubSettings.FixedLayout
import org.readium.r2.navigator.epub.EpubSettings.Reflowable
import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.epub.css.Layout.Stylesheets
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.Color
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
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
    abstract val language: Setting<Language?>
    /** Direction of the reading progression across resources. */
    abstract val readingProgression: EnumSetting<ReadingProgression>

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
        override val language: Setting<Language?>,
        override val readingProgression: EnumSetting<ReadingProgression>,
        val spread: EnumSetting<Spread>,
    ) : EpubSettings() {

        constructor() : this(
            language = languageSetting(),
            readingProgression = readingProgressionSetting(),
            spread = spreadSetting(),
        )

        companion object {

            /** Language of the publication content. */
            private fun languageSetting(
                value: Language? = null
            ): Setting<Language?> = Setting(
                key = Setting.LANGUAGE,
                value = value,
            )

            /** Direction of the reading progression across resources. */
            private fun readingProgressionSetting(
                value: ReadingProgression? = null
            ): EnumSetting<ReadingProgression> = EnumSetting(
                key = Setting.READING_PROGRESSION,
                value = value ?: ReadingProgression.LTR,
                values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
            )

            /**
             * Indicates the condition to be met for the publication to be rendered with a
             * synthetic spread (dual-page).
             */
            private fun spreadSetting(
                value: Spread? = null
            ): EnumSetting<Spread> = EnumSetting(
                key = Setting.SPREAD,
                value = value ?: Spread.NONE,
                // FIXME: Support Spread.AUTO and Spread.LANDSCAPE.
                values = listOf(Spread.NONE, Spread.BOTH),
            )
        }

        internal fun update(preferences: Preferences, defaults: Preferences): FixedLayout =
            FixedLayout(
                language = languageSetting(
                    value = language.firstValidValue(preferences, defaults)
                ),
                readingProgression = readingProgressionSetting(
                    value = readingProgression.firstValidValue(preferences, defaults)
                ),
                spread = spreadSetting(
                    value = spread.firstValidValue(preferences, defaults)
                ),
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
    data class Reflowable internal constructor(
        val backgroundColor: ColorSetting = backgroundColorSetting(),
        val columnCount: EnumSetting<ColumnCount> = columnCountSetting(),
        val fontFamily: EnumSetting<FontFamily?> = fontFamilySetting(),
        val fontSize: PercentSetting = fontSizeSetting(),
        val hyphens: ToggleSetting = hyphensSetting(),
        val imageFilter: EnumSetting<ImageFilter> = imageFilterSetting(),
        override val language: Setting<Language?> = languageSetting(),
        val letterSpacing: PercentSetting = letterSpacingSetting(),
        val ligatures: ToggleSetting = ligaturesSetting(),
        val lineHeight: RangeSetting<Double> = lineHeightSetting(),
        val pageMargins: RangeSetting<Double> = pageMarginsSetting(),
        val paragraphIndent: PercentSetting = paragraphIndentSetting(),
        val paragraphSpacing: PercentSetting = paragraphSpacingSetting(),
        val publisherStyles: ToggleSetting = publisherStylesSetting(),
        override val readingProgression: EnumSetting<ReadingProgression> = readingProgressionSetting(),
        val scroll: ToggleSetting = scrollSetting(),
        val textAlign: EnumSetting<TextAlign> = textAlignSetting(),
        val textColor: ColorSetting = textColorSetting(),
        val textNormalization: EnumSetting<TextNormalization> = textNormalizationSetting(),
        val theme: EnumSetting<Theme> = themeSetting(),
        val typeScale: RangeSetting<Double> = typeScaleSetting(),
        val verticalText: ToggleSetting = verticalTextSetting(),
        val wordSpacing: PercentSetting = wordSpacingSetting(),

        internal val layout: Layout
    ) : EpubSettings() {

        internal fun update(
            metadata: Metadata,
            fontFamilies: List<FontFamily>,
            namedColors: Map<String, Int>,
            defaults: Preferences,
            preferences: Preferences,
        ): Reflowable {
            val layoutResolver = createLayoutResolver(metadata, defaults)
            val layout = layoutResolver(preferences)

            return Reflowable(
                backgroundColor = backgroundColorSetting(
                    value = backgroundColor.firstValidValue(preferences, defaults),
                    namedColors = namedColors
                ),
                columnCount = columnCountSetting(
                    layoutResolver = layoutResolver,
                    value = columnCount.firstValidValue(preferences, defaults),
                ),
                fontFamily = fontFamilySetting(
                    value = fontFamily.firstValidValue(preferences, defaults),
                    fontFamilies = fontFamilies
                ),
                fontSize = fontSizeSetting(
                    value = fontSize.firstValidValue(preferences, defaults),
                ),
                hyphens = hyphensSetting(
                    layoutResolver = layoutResolver,
                    value = hyphens.firstValidValue(preferences, defaults),
                ),
                imageFilter = imageFilterSetting(
                    value = imageFilter.firstValidValue(preferences, defaults),
                ),
                language = languageSetting(
                    value = language.firstValidValue(preferences, defaults) ?: metadata.language,
                ),
                letterSpacing = letterSpacingSetting(
                    layoutResolver = layoutResolver,
                    value = letterSpacing.firstValidValue(preferences, defaults),
                ),
                ligatures = ligaturesSetting(
                    layoutResolver = layoutResolver,
                    value = ligatures.firstValidValue(preferences, defaults),
                ),
                lineHeight = lineHeightSetting(
                    value = lineHeight.firstValidValue(preferences, defaults),
                ),
                pageMargins = pageMarginsSetting(
                    layoutResolver = layoutResolver,
                    value = pageMargins.firstValidValue(preferences, defaults),
                ),
                paragraphIndent = paragraphIndentSetting(
                    layoutResolver = layoutResolver,
                    value = paragraphIndent.firstValidValue(preferences, defaults),
                ),
                paragraphSpacing = paragraphSpacingSetting(
                    value = paragraphSpacing.firstValidValue(preferences, defaults),
                ),
                publisherStyles = publisherStylesSetting(
                    value = publisherStyles.firstValidValue(preferences, defaults),
                ),
                readingProgression = readingProgressionSetting(
                    value = layout.readingProgression
                ),
                scroll = scrollSetting(
                    layoutResolver = layoutResolver,
                    value = scroll.firstValidValue(preferences, defaults),
                ),
                textAlign = textAlignSetting(
                    layoutResolver = layoutResolver,
                    value = textAlign.firstValidValue(preferences, defaults),
                ),
                textColor = textColorSetting(
                    value = textColor.firstValidValue(preferences, defaults),
                    namedColors = namedColors,
                ),
                textNormalization = textNormalizationSetting(
                    value = textNormalization.firstValidValue(preferences, defaults),
                ),
                theme = themeSetting(
                    value = theme.firstValidValue(preferences, defaults),
                ),
                typeScale = typeScaleSetting(
                    value = typeScale.firstValidValue(preferences, defaults),
                ),
                verticalText = verticalTextSetting(
                    value = layout.stylesheets == Stylesheets.CjkVertical
                ),
                wordSpacing = wordSpacingSetting(
                    layoutResolver = layoutResolver,
                    value = wordSpacing.firstValidValue(preferences, defaults),
                ),
                layout = layout
            )
        }

        companion object {

            operator fun invoke(
                metadata: Metadata = Metadata(localizedTitle = LocalizedString("")),
                fontFamilies: List<FontFamily> = emptyList(),
                namedColors: Map<String, Int> = emptyMap(),
                defaults: Preferences = Preferences(),
                preferences: Preferences = Preferences(),
            ) : Reflowable =
                Reflowable(layout = Layout()).update(metadata, fontFamilies, namedColors, defaults = defaults, preferences = preferences)

            /** Default page background color. */
            private fun backgroundColorSetting(
                value: Color? = null,
                namedColors: Map<String, Int> = emptyMap()
            ): ColorSetting = ColorSetting(
                key = Setting.BACKGROUND_COLOR,
                value = value ?: Color.AUTO,
                coder = Color.Coder(namedColors)
            )

            /** Number of columns to display (one-page view or two-page spread). */
            private fun columnCountSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: ColumnCount? = null
            ): EnumSetting<ColumnCount> = EnumSetting(
                key = Setting.COLUMN_COUNT,
                value = value ?: ColumnCount.AUTO,
                values = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
                activator = requiresScroll(false),
            )

            /** Default typeface for the text. */
            private fun fontFamilySetting(
                value: FontFamily? = null,
                fontFamilies: List<FontFamily> = emptyList()
            ): EnumSetting<FontFamily?> = fontFamilySetting(
                coder = FontFamily.Coder(fontFamilies),
                value = value,
                values = listOf(null) + fontFamilies,
            )

            /** Default typeface for the text. */
            private fun fontFamilySetting(
                coder: FontFamily.Coder,
                value: FontFamily?,
                values: List<FontFamily?>,
            ): EnumSetting<FontFamily?> = EnumSetting(
                key = Setting.FONT_FAMILY,
                coder = coder,
                value = value,
                values = values,
                formatValue = { it?.name }
            )

            /** Base text font size. */
            private fun fontSizeSetting(
                value: Double? = null
            ): PercentSetting = PercentSetting(
                key = Setting.FONT_SIZE,
                value = value ?: 1.0,
                range = 0.4..5.0
            )

            /** Enable hyphenation. */
            private fun hyphensSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Boolean? = null
            ): ToggleSetting = ToggleSetting(
                key = Setting.HYPHENS,
                value = value ?: true,
                activator = requiresPublisherStylesDisabled
                    + requiresStylesheet(layoutResolver, Stylesheets.Default)
            )

            /** Filter applied to images in dark theme. */
            private fun imageFilterSetting(
                value: ImageFilter? = null
            ): EnumSetting<ImageFilter> = EnumSetting(
                key = Setting.IMAGE_FILTER,
                value = value ?: ImageFilter.NONE,
                values = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT),
                activator = requiresTheme(Theme.DARK)
            )

            /** Language of the publication content. */
            private fun languageSetting(
                value: Language? = null
            ): Setting<Language?> = Setting(
                key = Setting.LANGUAGE,
                value = value,
            )

            /** Space between letters. */
            private fun letterSpacingSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Double? = null
            ): PercentSetting = PercentSetting(
                key = Setting.LETTER_SPACING,
                value = value ?: 0.0,
                activator = requiresPublisherStylesDisabled
                    + requiresStylesheet(layoutResolver, Stylesheets.Default)
            )

            /** Enable ligatures in Arabic. */
            private fun ligaturesSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Boolean? = null
            ): ToggleSetting = ToggleSetting(
                key = Setting.LIGATURES,
                value = value ?: true,
                activator = requiresPublisherStylesDisabled
                    + requiresStylesheet(layoutResolver, Stylesheets.Rtl)
            )

            /** Leading line height. */
            private fun lineHeightSetting(
                value: Double? = null
            ): RangeSetting<Double> = RangeSetting(
                key = Setting.LINE_HEIGHT,
                value = value ?: 1.2,
                range = 1.0..2.0,
                activator = requiresPublisherStylesDisabled
            )

            /** Factor applied to horizontal margins. */
            private fun pageMarginsSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Double? = null
            ): RangeSetting<Double> = RangeSetting(
                key = Setting.PAGE_MARGINS,
                value = value ?: 1.0,
                range = 0.5..4.0,
                activator = requiresScroll(false),
            )

            /** Text indentation for paragraphs. */
            private fun paragraphIndentSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Double? = null
            ): PercentSetting = PercentSetting(
                key = Setting.PARAGRAPH_INDENT,
                value = value ?: 0.0,
                range = 0.0..3.0,
                suggestedIncrement = 0.2,
                activator = requiresPublisherStylesDisabled
                    + requiresStylesheet(layoutResolver) { it == Stylesheets.Default || it == Stylesheets.Rtl }
            )

            /** Vertical margins for paragraphs. */
            private fun paragraphSpacingSetting(
                value: Double? = null
            ): PercentSetting = PercentSetting(
                key = Setting.PARAGRAPH_SPACING,
                value = value ?: 0.0,
                range = 0.0..2.0,
                activator = requiresPublisherStylesDisabled
            )

            /**
             * Indicates whether the original publisher styles should be observed.
             *
             * Many settings require this to be off.
             */
            private fun publisherStylesSetting(
                value: Boolean? = null
            ): ToggleSetting = ToggleSetting(
                key = Setting.PUBLISHER_STYLES,
                value = value ?: true
            )

            /** Direction of the reading progression across resources. */
            private fun readingProgressionSetting(
                value: ReadingProgression? = null
            ): EnumSetting<ReadingProgression> = EnumSetting(
                key = Setting.READING_PROGRESSION,
                value = value ?: ReadingProgression.LTR,
                values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
            )

            /**
             * Indicates if the overflow of resources should be handled using scrolling instead
             * of synthetic pagination.
             */
            private fun scrollSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Boolean? = null
            ): ToggleSetting = ToggleSetting(
                key = Setting.SCROLL,
                value = value ?: false
            )

            /** Page text alignment. */
            private fun textAlignSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: TextAlign? = null
            ): EnumSetting<TextAlign> = EnumSetting(
                key = Setting.TEXT_ALIGN,
                value = value ?: TextAlign.START,
                values = listOf(
                    TextAlign.START,
                    TextAlign.LEFT,
                    TextAlign.RIGHT,
                    TextAlign.JUSTIFY
                ),
                activator = requiresPublisherStylesDisabled
                    + requiresStylesheet(layoutResolver) { it == Stylesheets.Default || it == Stylesheets.Rtl }
            )

            /** Default page text color. */
            private fun textColorSetting(
                value: Color? = null,
                namedColors: Map<String, Int> = emptyMap()
            ): ColorSetting = ColorSetting(
                key = Setting.TEXT_COLOR,
                value = value ?: Color.AUTO,
                coder = Color.Coder(namedColors)
            )

            /** Normalize font style, weight and variants using a specific strategy. */
            private fun textNormalizationSetting(
                value: TextNormalization? = null
            ): EnumSetting<TextNormalization> = EnumSetting(
                key = Setting.TEXT_NORMALIZATION,
                value = value ?: TextNormalization.NONE,
                values = listOf(TextNormalization.NONE, TextNormalization.BOLD, TextNormalization.ACCESSIBILITY)
            )

            /** Reader theme. */
            private fun themeSetting(
                value: Theme? = null
            ): EnumSetting<Theme> = EnumSetting(
                key = Setting.THEME,
                value = value ?: Theme.LIGHT,
                values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
            )

            /**
             * Scale applied to all element font sizes.
             *
             * See https://readium.org/readium-css/docs/CSS19-api.html#typography
             */
            private fun typeScaleSetting(
                value: Double? = null
            ): RangeSetting<Double> = RangeSetting(
                key = Setting.TYPE_SCALE,
                value = value ?: 1.2,
                range = 1.0..2.0,
                suggestedSteps = listOf(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
                activator = requiresPublisherStylesDisabled
            )

            /**
             * Indicates whether the text should be laid out vertically. This is used for example
             * with CJK languages.
             *
             * This setting is automatically derived from the language if no preference is given.
             */
            private fun verticalTextSetting(
                value: Boolean? = null
            ): ToggleSetting = ToggleSetting(
                key = Setting.VERTICAL_TEXT,
                value = value ?: false,
            )

            /** Space between words. */
            private fun wordSpacingSetting(
                layoutResolver: (Preferences) -> Layout = { Layout() },
                value: Double? = null
            ): PercentSetting = PercentSetting(
                key = Setting.WORD_SPACING,
                value = value ?: 0.0,
                activator = requiresPublisherStylesDisabled
                    + requiresStylesheet(layoutResolver, Stylesheets.Default)
            )

            /** [SettingActivator] for settings requiring the publisher styles to be disabled. */
            private val requiresPublisherStylesDisabled = RequiresPreferenceSettingActivator(
                key = Setting.PUBLISHER_STYLES,
                value = false
            )

            /** [SettingActivator] for settings active only with the given [theme]. */
            private fun requiresTheme(theme: Theme) = RequiresPreferenceSettingActivator(
                key = Setting.THEME,
                value = theme
            )

            /** [SettingActivator] for settings active when the scroll is enabled or disabled. */
            private fun requiresScroll(scroll: Boolean) = RequiresPreferenceSettingActivator(
                key = Setting.SCROLL,
                value = scroll
            )

            /** [SettingActivator] for settings active only with the given layout [stylesheet]. */
            private fun requiresStylesheet(layoutResolver: (Preferences) -> Layout, value: Stylesheets) =
                MatchLayoutSettingActivator(layoutResolver) { layout ->
                    layout.stylesheets == value
                }

            /**
             * [SettingActivator] for settings active when the layout stylesheet matches the given
             * condition.
             */
            private fun requiresStylesheet(layoutResolver: (Preferences) -> Layout, matches: (Stylesheets) -> Boolean) =
                MatchLayoutSettingActivator(layoutResolver) { layout ->
                    matches(layout.stylesheets)
                }

            private class MatchLayoutSettingActivator(
                val layoutResolver: (Preferences) -> Layout,
                val matches: (Layout) -> Boolean
            ) : SettingActivator {
                override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                    matches(layoutResolver(preferences))

                override fun activateInPreferences(preferences: MutablePreferences) {
                    // Cannot activate automatically.
                }
            }

            private fun createLayoutResolver(metadata: Metadata, defaults: Preferences): (Preferences) -> Layout =
                { preferences ->
                    val language = languageSetting().firstValidValue(preferences, defaults) ?: metadata.language
                    val readingProgression = readingProgressionSetting()

                    Layout.from(
                        language = language,
                        hasMultipleLanguages = if (language == null) metadata.languages.size > 1 else false,
                        readingProgression = readingProgression.firstValidValue(preferences, defaults)
                            ?: readingProgression.validate(metadata.readingProgression)
                            ?: ReadingProgression.AUTO,
                        verticalText = verticalTextSetting().firstValidValue(preferences, defaults)
                    )
                }
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
                view = when (scroll.value) {
                    false -> View.PAGED
                    true -> View.SCROLL
                },
                colCount = when (columnCount.value) {
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
                darkenImages = imageFilter.value == ImageFilter.DARKEN,
                invertImages = imageFilter.value == ImageFilter.INVERT,
                textColor = textColor.value
                    .takeIf { it != Color.AUTO }
                    ?.let { CssColor.Int(it.int) },
                backgroundColor = backgroundColor.value
                    .takeIf { it != Color.AUTO }
                    ?.let { CssColor.Int(it.int) },
                fontOverride = (fontFamily.value != null || (textNormalization.value == TextNormalization.ACCESSIBILITY)),
                fontFamily = fontFamily.value?.toCss(),
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Percent(it) },
                advancedSettings = !publisherStyles.value,
                typeScale = typeScale.value,
                textAlign = when (textAlign.value) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                },
                lineHeight = Either(lineHeight.value),
                paraSpacing = Length.Rem(paragraphSpacing.value),
                paraIndent = Length.Rem(paragraphIndent.value),
                wordSpacing = Length.Rem(wordSpacing.value),
                letterSpacing = Length.Rem(letterSpacing.value / 2),
                bodyHyphens = if (hyphens.value) Hyphens.AUTO else Hyphens.NONE,
                ligatures = if (ligatures.value) Ligatures.COMMON else Ligatures.NONE,
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

    var fontFamily: FontFamily? = null
    if (sp.contains("fontFamily")) {
        fontFamily = sp.getInt("fontFamily", 0)
            .let { fontFamilies.getOrNull(it) }
            .takeUnless { it == "Original" }
            ?.let { FontFamily(it) }
    }

    val fontFamilies = listOfNotNull(fontFamily)
    val settings = Reflowable(
        fontFamilies = fontFamilies
    )

    return Preferences {
        set(settings.fontFamily, fontFamily)

        if (sp.contains("appearance")) {
            val appearance = sp.getInt("appearance", 0)
            set(settings.theme, when (appearance) {
                0 -> Theme.LIGHT
                1 -> Theme.SEPIA
                2 -> Theme.DARK
                else -> null
            })
        }

        if (sp.contains("scroll")) {
            set(settings.scroll, sp.getBoolean("scroll", false))
        }

        if (sp.contains("colCount")) {
            val colCount = sp.getInt("colCount", 0)
            set(settings.columnCount, when (colCount) {
                0 -> ColumnCount.AUTO
                1 -> ColumnCount.ONE
                2 -> ColumnCount.TWO
                else -> null
            })
        }

        if (sp.contains("pageMargins")) {
            val pageMargins = sp.getFloat("pageMargins", 1.0f).toDouble()
            set(settings.pageMargins, pageMargins)
        }

        if (sp.contains("fontSize")) {
            val fontSize = (sp.getFloat("fontSize", 0f) / 100).toDouble()
            set(settings.fontSize, fontSize)
        }

        if (sp.contains("textAlign")) {
            val textAlign = sp.getInt("textAlign", 0)
            set(settings.textAlign, when (textAlign) {
                0 -> TextAlign.JUSTIFY
                1 -> TextAlign.START
                else -> null
            })
        }

        if (sp.contains("wordSpacing")) {
            val wordSpacing = sp.getFloat("wordSpacing", 0f).toDouble()
            set(settings.wordSpacing, wordSpacing)
        }

        if (sp.contains("letterSpacing")) {
            val letterSpacing = sp.getFloat("letterSpacing", 0f).toDouble() * 2
            set(settings.letterSpacing, letterSpacing)
        }

        if (sp.contains("lineHeight")) {
            val lineHeight = sp.getFloat("lineHeight", 1.2f).toDouble()
            set(settings.lineHeight, lineHeight)
        }

        if (sp.contains("advancedSettings")) {
            val advancedSettings = sp.getBoolean("advancedSettings", false)
            set(settings.publisherStyles, !advancedSettings)
        }
    }
}
