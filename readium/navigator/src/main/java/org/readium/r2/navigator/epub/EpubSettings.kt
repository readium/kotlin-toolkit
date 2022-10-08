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
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
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
     * @param spread Indicates if the publication should be rendered with a
     * synthetic spread (dual-page).
     */
    @ExperimentalReadiumApi
    data class FixedLayout internal constructor(
        override val language: Setting<Language?>,
        override val readingProgression: EnumSetting<ReadingProgression>,
        override val spread: EnumSetting<Spread>,
    ) : EpubSettings(), FixedLayoutSettings {

        override val offset: Setting<Boolean>? = null
        override val fit: EnumSetting<Fit>? = null
        override val scroll: Setting<Boolean>? = null
        override val scrollAxis: EnumSetting<Axis>? = null
        override val pageSpacing: RangeSetting<Double>? = null
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
        override val backgroundColor: Setting<Color>,
        override val columnCount: EnumSetting<ColumnCount>,
        override val fontFamily: EnumSetting<FontFamily?>,
        override val fontSize: PercentSetting,
        override val hyphens: Setting<Boolean>,
        override val imageFilter: EnumSetting<ImageFilter>,
        override val language: Setting<Language?>,
        override val letterSpacing: PercentSetting,
        override val ligatures: Setting<Boolean>,
        override val lineHeight: RangeSetting<Double>,
        override val pageMargins: RangeSetting<Double>,
        override val paragraphIndent: PercentSetting,
        override val paragraphSpacing: PercentSetting,
        override val publisherStyles: Setting<Boolean>,
        override val readingProgression: EnumSetting<ReadingProgression>,
        override val scroll: Setting<Boolean>,
        override val textAlign: EnumSetting<TextAlign>,
        override val textColor: Setting<Color>,
        override val textNormalization: EnumSetting<TextNormalization>,
        override val theme: EnumSetting<Theme>,
        override val typeScale: RangeSetting<Double>,
        override val verticalText: Setting<Boolean>,
        override val wordSpacing: PercentSetting,

        internal val layout: Layout
    ) : EpubSettings(), ReflowaleSettings

    companion object {

        val BACKGROUND_COLOR = Setting.Key<Color>("backgroundColor")
        val COLUMN_COUNT = Setting.Key<ColumnCount>("columnCount")
        val FONT_FAMILY = Setting.Key<FontFamily?>("fontFamily")
        val FONT_SIZE = Setting.Key<Double>("fontSize")
        val HYPHENS = Setting.Key<Boolean>("hyphens")
        val IMAGE_FILTER = Setting.Key<ImageFilter>("imageFilter")
        val LANGUAGE = Setting.Key<Language?>("language")
        val LETTER_SPACING = Setting.Key<Double>("letterSpacing")
        val LIGATURES = Setting.Key<Boolean>("ligatures")
        val LINE_HEIGHT = Setting.Key<Double>("lineHeight")
        val ORIENTATION = Setting.Key<Double>("orientation")
        val PAGE_MARGINS = Setting.Key<Double>("pageMargins")
        val PARAGRAPH_INDENT = Setting.Key<Double>("paragraphIndent")
        val PARAGRAPH_SPACING = Setting.Key<Double>("paragraphSpacing")
        val PUBLISHER_STYLES = Setting.Key<Boolean>("publisherStyles")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL = Setting.Key<Boolean>("scroll")
        val SCROLL_AXIS = Setting.Key<Boolean>("scrollAxis")
        val SPREAD = Setting.Key<Spread>("spread")
        val TEXT_ALIGN = Setting.Key<TextAlign>("textAlign")
        val TEXT_COLOR = Setting.Key<Color>("textColor")
        val TEXT_NORMALIZATION = Setting.Key<TextNormalization>("textNormalization")
        val THEME = Setting.Key<Theme>("theme")
        val TYPE_SCALE = Setting.Key<Double>("typeScale")
        val VERTICAL_TEXT = Setting.Key<Boolean>("verticalText")
        val WORD_SPACING = Setting.Key<Double>("wordSpacing")
    }
}

internal fun ReadiumCss.update(settings: EpubSettings, fontFamilies: List<FontFamilyDeclaration>): ReadiumCss {
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
                textColor = textColor.value.toCss(),
                backgroundColor = backgroundColor.value.toCss(),
                fontOverride = (fontFamily.value != null || (textNormalization.value == TextNormalization.ACCESSIBILITY)),
                fontFamily = fontFamily.value?.toCss(fontFamilies),
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

private fun FontFamily.toCss(declarations: List<FontFamilyDeclaration>): List<String> = buildList {
    val declaration = declarations.firstOrNull { it.fontFamily == this@toCss }
    checkNotNull(declaration) { "Cannot resolve font name."}
    add(name)
    val alternateChain = declaration.alternate?.fontFamily?.toCss(declarations)
    alternateChain?.let {  addAll(it) }
}

private fun Color.toCss(): CssColor =
    CssColor.Int(int)

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
) = Preferences {

    val sp = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)

    if (sp.contains("fontFamily")) {
        val fontFamily = sp.getInt("fontFamily", 0)
            .let { fontFamilies.getOrNull(it) }
            .takeUnless { it == "Original" }
            ?.let { FontFamily(it) }

        set(EpubSettings.FONT_FAMILY, fontFamily)
    }

    if (sp.contains("appearance")) {
        val appearance = sp.getInt("appearance", 0)
        set(EpubSettings.THEME, when (appearance) {
            0 -> Theme.LIGHT
            1 -> Theme.SEPIA
            2 -> Theme.DARK
            else -> null
        })
    }

    if (sp.contains("scroll")) {
        set(EpubSettings.SCROLL, sp.getBoolean("scroll", false))
    }

    if (sp.contains("colCount")) {
        val colCount = sp.getInt("colCount", 0)
        set(EpubSettings.COLUMN_COUNT, when (colCount) {
            0 -> ColumnCount.AUTO
            1 -> ColumnCount.ONE
            2 -> ColumnCount.TWO
            else -> null
        })
    }

    if (sp.contains("pageMargins")) {
        val pageMargins = sp.getFloat("pageMargins", 1.0f).toDouble()
        set(EpubSettings.PAGE_MARGINS, pageMargins)
    }

    if (sp.contains("fontSize")) {
        val fontSize = (sp.getFloat("fontSize", 0f) / 100).toDouble()
        set(EpubSettings.FONT_SIZE, fontSize)
    }

    if (sp.contains("textAlign")) {
        val textAlign = sp.getInt("textAlign", 0)
        set(EpubSettings.TEXT_ALIGN, when (textAlign) {
            0 -> TextAlign.JUSTIFY
            1 -> TextAlign.START
            else -> null
        })
    }

    if (sp.contains("wordSpacing")) {
        val wordSpacing = sp.getFloat("wordSpacing", 0f).toDouble()
        set(EpubSettings.WORD_SPACING, wordSpacing)
    }

    if (sp.contains("letterSpacing")) {
        val letterSpacing = sp.getFloat("letterSpacing", 0f).toDouble() * 2
        set(EpubSettings.LETTER_SPACING, letterSpacing)
    }

    if (sp.contains("lineHeight")) {
        val lineHeight = sp.getFloat("lineHeight", 1.2f).toDouble()
        set(EpubSettings.LINE_HEIGHT, lineHeight)
    }

    if (sp.contains("advancedSettings")) {
        val advancedSettings = sp.getBoolean("advancedSettings", false)
        set(EpubSettings.PUBLISHER_STYLES, !advancedSettings)
    }
}
