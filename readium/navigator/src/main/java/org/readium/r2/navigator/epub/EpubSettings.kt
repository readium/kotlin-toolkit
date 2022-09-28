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
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
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

        companion object {

            operator fun invoke(): FixedLayout =
                EpubSettingsFactory().defaultFixedLayoutSettings()
        }
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
        val backgroundColor: ColorSetting,
        val columnCount: EnumSetting<ColumnCount>,
        val fontFamily: EnumSetting<FontFamily?>,
        val fontSize: PercentSetting,
        val hyphens: ToggleSetting,
        val imageFilter: EnumSetting<ImageFilter>,
        override val language: Setting<Language?>,
        val letterSpacing: PercentSetting,
        val ligatures: ToggleSetting,
        val lineHeight: RangeSetting<Double>,
        val pageMargins: RangeSetting<Double>,
        val paragraphIndent: PercentSetting,
        val paragraphSpacing: PercentSetting,
        val publisherStyles: ToggleSetting,
        override val readingProgression: EnumSetting<ReadingProgression>,
        val scroll: ToggleSetting,
        val textAlign: EnumSetting<TextAlign>,
        val textColor: ColorSetting,
        val textNormalization: EnumSetting<TextNormalization>,
        val theme: EnumSetting<Theme>,
        val typeScale: RangeSetting<Double>,
        val verticalText: ToggleSetting,
        val wordSpacing: PercentSetting,

        internal val layout: Layout
    ) : EpubSettings() {

        companion object {

            operator fun invoke(): Reflowable =
                EpubSettingsFactory().createDefaultReflowableSettings()
        }
    }
}

internal fun ReadiumCss.update(settings: EpubSettings): ReadiumCss {
    if (settings !is Reflowable) return this

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
    val settings = EpubSettingsFactory(
        fontFamilies = fontFamilies
    ).createDefaultReflowableSettings()

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
