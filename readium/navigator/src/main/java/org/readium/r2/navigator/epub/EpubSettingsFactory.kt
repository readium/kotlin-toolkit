/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
internal class EpubSettingsFactory(
    private val metadata: Metadata = Metadata(localizedTitle = LocalizedString("")),
    private val fontFamilies: List<FontFamily> = emptyList(),
    private val settingsPolicy: EpubSettingsPolicy = EpubSettingsPolicy(),
) {

    fun createSettings(preferences: Preferences): EpubSettings =
        when (metadata.presentation.layout) {
            EpubLayout.REFLOWABLE , null -> createReflowableSettings(preferences)
            EpubLayout.FIXED -> createFixedLayoutSettings(preferences)
        }

    private fun createFixedLayoutSettings(preferences: Preferences): EpubSettings.FixedLayout {
        val values = settingsPolicy.fixedLayoutSettings(metadata, preferences)

        return EpubSettings.FixedLayout(
            language = languageSetting(values.language),
            readingProgression = readingProgressionSetting(values.readingProgression),
            spread = spreadSetting(values.spread)
        )
    }

    fun createReflowableSettings(preferences: Preferences): EpubSettings.Reflowable {
        val values = settingsPolicy.reflowableSettings(metadata, preferences)

        return EpubSettings.Reflowable(
            backgroundColor = backgroundColorSetting(values.backgroundColor),
            columnCount = columnCountSetting(values.columnCount),
            fontFamily = fontFamilySetting(values.fontFamily, fontFamilies),
            fontSize = fontSizeSetting(values.fontSize),
            hyphens = hyphensSetting(values.hyphens),
            imageFilter = imageFilterSetting(values.imageFilter),
            language = languageSetting(values.language),
            letterSpacing = letterSpacingSetting(values.letterSpacing),
            ligatures = ligaturesSetting(values.ligatures),
            lineHeight = lineHeightSetting(values.lineHeight),
            pageMargins = pageMarginsSetting(values.pageMargins),
            paragraphIndent = paragraphIndentSetting(values.paragraphIndent),
            paragraphSpacing = paragraphSpacingSetting(values.paragraphSpacing),
            publisherStyles = publisherStylesSetting(values.publisherStyles),
            readingProgression = readingProgressionSetting(values.readingProgression),
            scroll = scrollSetting(values.scroll),
            textAlign = textAlignSetting(values.textAlign),
            textColor = textColorSetting(values.textColor),
            textNormalization = textNormalizationSetting(values.textNormalization),
            theme = themeSetting(values.theme),
            typeScale = typeScaleSetting(values.typeScale),
            verticalText = verticalTextSetting(values.verticalText),
            wordSpacing = wordSpacingSetting(values.wordSpacing),
            layout = Layout.from(values)
        )
    }

    private fun languageSetting(
        value: Language?
    ): Setting<Language?> = Setting(
        key = EpubSettings.LANGUAGE,
        value = value,
    )

    private fun readingProgressionSetting(
        value: ReadingProgression
    ): EnumSetting<ReadingProgression> = EnumSetting(
        key = EpubSettings.READING_PROGRESSION,
        value = value,
        values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
    )

    private fun spreadSetting(
        value: Presentation.Spread
    ): EnumSetting<Presentation.Spread> = EnumSetting(
        key = EpubSettings.SPREAD,
        value = value,
        // FIXME: Support Spread.AUTO and Spread.LANDSCAPE.
        values = listOf(Presentation.Spread.NONE, Presentation.Spread.BOTH),
    )

    /** Default page background color. */
    private fun backgroundColorSetting(
        value: Color,
    ): Setting<Color> = Setting(
        key = EpubSettings.BACKGROUND_COLOR,
        value = value
    )

    /** Number of columns to display (one-page view or two-page spread). */
    private fun columnCountSetting(
        value: ColumnCount
    ): EnumSetting<ColumnCount> = EnumSetting(
        key = EpubSettings.COLUMN_COUNT,
        value = value,
        values = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
        activator = requiresScroll(false),
    )

    /** Default typeface for the text. */
    private fun fontFamilySetting(
        value: FontFamily?,
        fontFamilies: List<FontFamily> = emptyList()
    ): EnumSetting<FontFamily?> = EnumSetting(
        key = EpubSettings.FONT_FAMILY,
        value = value,
        values = listOf(null) + fontFamilies,
        formatValue = { it?.name }
    )

    /** Base text font size. */
    private fun fontSizeSetting(
        value: Double
    ): PercentSetting = PercentSetting(
        key = EpubSettings.FONT_SIZE,
        value = value,
        range = 0.4..5.0
    )

    /** Enable hyphenation. */
    private fun hyphensSetting(
        value: Boolean
    ): ToggleSetting = ToggleSetting(
        key = EpubSettings.HYPHENS,
        value = value,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet(Layout.Stylesheets.Default)
    )

    /** Filter applied to images in dark theme. */
    private fun imageFilterSetting(
        value: ImageFilter
    ): EnumSetting<ImageFilter> = EnumSetting(
        key = EpubSettings.IMAGE_FILTER,
        value = value,
        values = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT),
        activator = requiresTheme(Theme.DARK)
    )

    /** Space between letters. */
    private fun letterSpacingSetting(
        value: Double
    ): PercentSetting = PercentSetting(
        key = EpubSettings.LETTER_SPACING,
        value = value,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet(Layout.Stylesheets.Default)
    )

    /** Enable ligatures in Arabic. */
    private fun ligaturesSetting(
        value: Boolean
    ): ToggleSetting = ToggleSetting(
        key = EpubSettings.LIGATURES,
        value = value,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet(Layout.Stylesheets.Rtl)
    )

    /** Leading line height. */
    private fun lineHeightSetting(
        value: Double
    ): RangeSetting<Double> = RangeSetting(
        key = EpubSettings.LINE_HEIGHT,
        value = value,
        range = 1.0..2.0,
        activator = requiresPublisherStylesDisabled
    )

    /** Factor applied to horizontal margins. */
    private fun pageMarginsSetting(
        value: Double
    ): RangeSetting<Double> = RangeSetting(
        key = EpubSettings.PAGE_MARGINS,
        value = value,
        range = 0.5..4.0,
        activator = requiresScroll(false),
    )

    /** Text indentation for paragraphs. */
    private fun paragraphIndentSetting(
        value: Double
    ): PercentSetting = PercentSetting(
        key = EpubSettings.PARAGRAPH_INDENT,
        value = value,
        range = 0.0..3.0,
        suggestedIncrement = 0.2,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet { it == Layout.Stylesheets.Default || it == Layout.Stylesheets.Rtl }
    )

    /** Vertical margins for paragraphs. */
    private fun paragraphSpacingSetting(
        value: Double
    ): PercentSetting = PercentSetting(
        key = EpubSettings.PARAGRAPH_SPACING,
        value = value,
        range = 0.0..2.0,
        activator = requiresPublisherStylesDisabled
    )

    /**
     * Indicates whether the original publisher styles should be observed.
     *
     * Many settings require this to be off.
     */
    private fun publisherStylesSetting(
        value: Boolean
    ): ToggleSetting = ToggleSetting(
        key = EpubSettings.PUBLISHER_STYLES,
        value = value
    )

    /**
     * Indicates if the overflow of resources should be handled using scrolling instead
     * of synthetic pagination.
     */
    private fun scrollSetting(
        value: Boolean
    ): ToggleSetting = ToggleSetting(
        key = EpubSettings.SCROLL,
        value = value
    )

    /** Page text alignment. */
    private fun textAlignSetting(
        value: TextAlign
    ): EnumSetting<TextAlign> = EnumSetting(
        key = EpubSettings.TEXT_ALIGN,
        value = value,
        values = listOf(
            TextAlign.START,
            TextAlign.LEFT,
            TextAlign.RIGHT,
            TextAlign.JUSTIFY
        ),
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet { it == Layout.Stylesheets.Default || it == Layout.Stylesheets.Rtl }
    )

    /** Default page text color. */
    private fun textColorSetting(
        value: Color,
    ): Setting<Color> = Setting(
        key = EpubSettings.TEXT_COLOR,
        value = value,
    )

    /** Normalize font style, weight and variants using a specific strategy. */
    private fun textNormalizationSetting(
        value: TextNormalization
    ): EnumSetting<TextNormalization> = EnumSetting(
        key = EpubSettings.TEXT_NORMALIZATION,
        value = value,
        values = listOf(
            TextNormalization.NONE,
            TextNormalization.BOLD,
            TextNormalization.ACCESSIBILITY
        )
    )

    /** Reader theme. */
    private fun themeSetting(
        value: Theme
    ): EnumSetting<Theme> = EnumSetting(
        key = EpubSettings.THEME,
        value = value,
        values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
    )

    /**
     * Scale applied to all element font sizes.
     *
     * See https://readium.org/readium-css/docs/CSS19-api.html#typography
     */
    private fun typeScaleSetting(
        value: Double
    ): RangeSetting<Double> = RangeSetting(
        key = EpubSettings.TYPE_SCALE,
        value = value,
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
        value: Boolean
    ): ToggleSetting = ToggleSetting(
        key = EpubSettings.VERTICAL_TEXT,
        value = value,
    )

    /** Space between words. */
    private fun wordSpacingSetting(
        value: Double
    ): PercentSetting = PercentSetting(
        key = EpubSettings.WORD_SPACING,
        value = value,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet(Layout.Stylesheets.Default)
    )

    /** [SettingActivator] for settings requiring the publisher styles to be disabled. */
    private val requiresPublisherStylesDisabled = ForcePreferenceSettingActivator(
        key = EpubSettings.PUBLISHER_STYLES,
        value = false
    ) { preferences -> settingsPolicy.reflowableSettings(metadata, preferences).publisherStyles }

    /** [SettingActivator] for settings active only with the given [theme]. */
    private fun requiresTheme(theme: Theme) = ForcePreferenceSettingActivator(
        key = EpubSettings.THEME,
        value = theme
    ) { preferences -> settingsPolicy.reflowableSettings(metadata, preferences).theme }

    /** [SettingActivator] for settings active when the scroll is enabled or disabled. */
    private fun requiresScroll(scroll: Boolean) = RequirePreferenceSettingActivator(
        key = EpubSettings.SCROLL,
        value = scroll
    ) { preferences -> settingsPolicy.reflowableSettings(metadata, preferences).scroll  }

    /** [SettingActivator] for settings active only with the given layout [stylesheets]. */
    private fun requiresStylesheet(stylesheets: Layout.Stylesheets) =
        MatchLayoutSettingActivator(
            { preferences -> Layout.from(settingsPolicy.reflowableSettings(metadata, preferences)) },
            { layout -> layout.stylesheets == stylesheets }
        )

    /**
     * [SettingActivator] for settings active when the layout stylesheet matches the given
     * condition.
     */
    private fun requiresStylesheet(
        matches: (Layout.Stylesheets) -> Boolean
    ) = MatchLayoutSettingActivator(
        { preferences -> Layout.from(settingsPolicy.reflowableSettings(metadata, preferences)) },
        { layout -> matches(layout.stylesheets) }
    )

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
}