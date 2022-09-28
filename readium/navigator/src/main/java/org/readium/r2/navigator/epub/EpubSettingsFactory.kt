package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.epub.css.LayoutResolver
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
    private val namedColors: Map<String, Int> = emptyMap(),
    private val preferences: Preferences = Preferences(),
    private val defaults: Preferences = Preferences()
) {

    fun createSettings(): EpubSettings =
        when (metadata.presentation.layout) {
            EpubLayout.FIXED -> updateFixedLayoutSettings(EpubSettings.FixedLayout())
            EpubLayout.REFLOWABLE, null -> updateReflowableSettings(EpubSettings.Reflowable())
        }

    fun updateSettings(settings: EpubSettings): EpubSettings =
        when (settings) {
            is EpubSettings.Reflowable -> updateReflowableSettings(settings)
            is EpubSettings.FixedLayout -> updateFixedLayoutSettings(settings)
        }

    fun defaultFixedLayoutSettings(): EpubSettings.FixedLayout =
        EpubSettings.FixedLayout(
            language = languageSetting(),
            readingProgression = readingProgressionSetting(),
            spread = spreadSetting()
        )

    private fun updateFixedLayoutSettings(settings: EpubSettings.FixedLayout): EpubSettings.FixedLayout =
        EpubSettings.FixedLayout(
            language = languageSetting(
                value = settings.language.firstValidValue(preferences, defaults)
            ),
            readingProgression = readingProgressionSetting(
                value = settings.readingProgression.firstValidValue(preferences, defaults)
            ),
            spread = spreadSetting(
                value = settings.spread.firstValidValue(preferences, defaults)
            )
        )

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
        value: Presentation.Spread? = null
    ): EnumSetting<Presentation.Spread> = EnumSetting(
        key = Setting.SPREAD,
        value = value ?: Presentation.Spread.NONE,
        // FIXME: Support Spread.AUTO and Spread.LANDSCAPE.
        values = listOf(Presentation.Spread.NONE, Presentation.Spread.BOTH),
    )

    fun createDefaultReflowableSettings(): EpubSettings.Reflowable =
        EpubSettings.Reflowable(
            backgroundColor = backgroundColorSetting(),
            columnCount = columnCountSetting(),
            fontFamily = fontFamilySetting(),
            fontSize = fontSizeSetting(),
            hyphens = hyphensSetting(),
            imageFilter = imageFilterSetting(),
            language = languageSetting(),
            letterSpacing = letterSpacingSetting(),
            ligatures = ligaturesSetting(),
            lineHeight = lineHeightSetting(),
            pageMargins = pageMarginsSetting(),
            paragraphIndent = paragraphIndentSetting(),
            paragraphSpacing = paragraphSpacingSetting(),
            publisherStyles = publisherStylesSetting(),
            readingProgression = readingProgressionSetting(),
            scroll = scrollSetting(),
            textAlign = textAlignSetting(),
            textColor = textColorSetting(),
            textNormalization = textNormalizationSetting(),
            theme = themeSetting(),
            typeScale = typeScaleSetting(),
            verticalText = verticalTextSetting(),
            wordSpacing = wordSpacingSetting(),
            layout = Layout()
        )

    private fun updateReflowableSettings(settings: EpubSettings.Reflowable): EpubSettings.Reflowable {
        val layoutResolver = LayoutResolver(metadata, defaults)
        val layout = layoutResolver.resolve(preferences)

        return EpubSettings.Reflowable(
            backgroundColor = backgroundColorSetting(
                value = settings.backgroundColor.firstValidValue(preferences, defaults),
                namedColors = namedColors
            ),
            columnCount = columnCountSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.columnCount.firstValidValue(preferences, defaults),
            ),
            fontFamily = fontFamilySetting(
                value = settings.fontFamily.firstValidValue(preferences, defaults),
                fontFamilies = fontFamilies
            ),
            fontSize = fontSizeSetting(
                value = settings.fontSize.firstValidValue(preferences, defaults),
            ),
            hyphens = hyphensSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.hyphens.firstValidValue(preferences, defaults),
            ),
            imageFilter = imageFilterSetting(
                value = settings.imageFilter.firstValidValue(preferences, defaults),
            ),
            language = languageSetting(
                value = settings.layout.language
            ),
            letterSpacing = letterSpacingSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.letterSpacing.firstValidValue(preferences, defaults),
            ),
            ligatures = ligaturesSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.ligatures.firstValidValue(preferences, defaults),
            ),
            lineHeight = lineHeightSetting(
                value = settings.lineHeight.firstValidValue(preferences, defaults),
            ),
            pageMargins = pageMarginsSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.pageMargins.firstValidValue(preferences, defaults),
            ),
            paragraphIndent = paragraphIndentSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.paragraphIndent.firstValidValue(preferences, defaults),
            ),
            paragraphSpacing = paragraphSpacingSetting(
                value = settings.paragraphSpacing.firstValidValue(preferences, defaults),
            ),
            publisherStyles = publisherStylesSetting(
                value = settings.publisherStyles.firstValidValue(preferences, defaults),
            ),
            readingProgression = readingProgressionSetting(
                value = layout.readingProgression
            ),
            scroll = scrollSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.scroll.firstValidValue(preferences, defaults),
            ),
            textAlign = textAlignSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.textAlign.firstValidValue(preferences, defaults),
            ),
            textColor = textColorSetting(
                value = settings.textColor.firstValidValue(preferences, defaults),
                namedColors = namedColors,
            ),
            textNormalization = textNormalizationSetting(
                value = settings.textNormalization.firstValidValue(preferences, defaults),
            ),
            theme = themeSetting(
                value = settings.theme.firstValidValue(preferences, defaults),
            ),
            typeScale = typeScaleSetting(
                value = settings.typeScale.firstValidValue(preferences, defaults),
            ),
            verticalText = verticalTextSetting(
                value = settings.layout.stylesheets == Layout.Stylesheets.CjkVertical
            ),
            wordSpacing = wordSpacingSetting(
                layoutResolver = layoutResolver::resolve,
                value = settings.wordSpacing.firstValidValue(preferences, defaults),
            ),
            layout = settings.layout
        )
    }

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
            + requiresStylesheet(layoutResolver, Layout.Stylesheets.Default)
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

    /** Space between letters. */
    private fun letterSpacingSetting(
        layoutResolver: (Preferences) -> Layout = { Layout() },
        value: Double? = null
    ): PercentSetting = PercentSetting(
        key = Setting.LETTER_SPACING,
        value = value ?: 0.0,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet(layoutResolver, Layout.Stylesheets.Default)
    )

    /** Enable ligatures in Arabic. */
    private fun ligaturesSetting(
        layoutResolver: (Preferences) -> Layout = { Layout() },
        value: Boolean? = null
    ): ToggleSetting = ToggleSetting(
        key = Setting.LIGATURES,
        value = value ?: true,
        activator = requiresPublisherStylesDisabled
            + requiresStylesheet(layoutResolver, Layout.Stylesheets.Rtl)
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
            + requiresStylesheet(layoutResolver) { it == Layout.Stylesheets.Default || it == Layout.Stylesheets.Rtl }
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
            + requiresStylesheet(layoutResolver) { it == Layout.Stylesheets.Default || it == Layout.Stylesheets.Rtl }
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
        values = listOf(
            TextNormalization.NONE,
            TextNormalization.BOLD,
            TextNormalization.ACCESSIBILITY
        )
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
            + requiresStylesheet(layoutResolver, Layout.Stylesheets.Default)
    )

    /** [SettingActivator] for settings requiring the publisher styles to be disabled. */
    private val requiresPublisherStylesDisabled = ForcePreferenceSettingActivator(
        key = Setting.PUBLISHER_STYLES,
        value = false,
        fallbackValue = publisherStylesDefault
    )

    /** [SettingActivator] for settings active only with the given [theme]. */
    private fun requiresTheme(theme: Theme) = ForcePreferenceSettingActivator(
        key = Setting.THEME,
        value = theme,
        fallbackValue = themeDefault
    )

    /** [SettingActivator] for settings active when the scroll is enabled or disabled. */
    private fun requiresScroll(scroll: Boolean) = RequirePreferenceSettingActivator(
        key = Setting.SCROLL,
        value = scroll,
        scrollDefault
    )

    /** [SettingActivator] for settings active only with the given layout [stylesheet]. */
    private fun requiresStylesheet(layoutResolver: (Preferences) -> Layout, value: Layout.Stylesheets) =
        MatchLayoutSettingActivator(layoutResolver) { layout ->
            layout.stylesheets == value
        }

    /**
     * [SettingActivator] for settings active when the layout stylesheet matches the given
     * condition.
     */
    private fun requiresStylesheet(
        layoutResolver: (Preferences) -> Layout,
        matches: (Layout.Stylesheets) -> Boolean
    ) = MatchLayoutSettingActivator(layoutResolver) { layout ->
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

    companion object {

        private val publisherStylesDefault: Boolean = true

        private val scrollDefault: Boolean = false

        private val themeDefault: Theme = Theme.LIGHT
    }
}