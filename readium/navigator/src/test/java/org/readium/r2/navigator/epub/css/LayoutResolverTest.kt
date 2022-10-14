/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub.css

import org.junit.Test
import org.readium.r2.navigator.epub.css.Layout.Stylesheets
import org.readium.r2.navigator.preferences.MutablePreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression.LTR
import org.readium.r2.shared.publication.ReadingProgression.RTL
import org.readium.r2.shared.util.Language
import kotlin.test.assertEquals

class LayoutResolverTest {

    private val fakeTitle = LocalizedString("fake title")
    private val readingProgressionSetting = EpubSettings.Reflowable.readingProgressionSetting()
    private val verticalTextSetting = EpubSettings.Reflowable.verticalTextSetting()
    private val languageSetting = EpubSettings.Reflowable.languageSetting()

    @Test
    fun `Compute the layout without any preferences or defaults`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle)).resolve()
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"))).resolve()
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ar"))).resolve()
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("fa")),).resolve()
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("he"))).resolve()
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"))).resolve()
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ko"))).resolve()
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh")),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-HK"))).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-Hans")),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-Hant"))).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-TW"))).resolve()
        )
    }

    @Test
    fun `Compute the layout with LTR reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ar"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("fa"), readingProgression = LTR)).resolve()
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("he"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ko"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-HK"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-Hans"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-Hant"), readingProgression = LTR),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-TW"), readingProgression = LTR),).resolve()
        )
    }

    @Test
    fun `Compute the layout with RTL reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ar"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("fa"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("he"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ko"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-HK"), readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-Hans"), readingProgression = RTL),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-Hant"), readingProgression = RTL),).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-TW"), readingProgression = RTL),).resolve()
        )
    }

    @Test
    fun `Compute the layout with vertical text force enabled`() {
        val preferences = MutablePreferences().apply {
            set(verticalTextSetting, true)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.CjkVertical, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle)).resolve(preferences)
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.CjkVertical, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = LTR)).resolve(preferences)
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = RTL)).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.CjkVertical, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"), readingProgression = LTR)).resolve(preferences)
        )
    }

    @Test
    fun `Compute the layout with vertical text force disabled`() {
        val preferences = MutablePreferences().apply {
            set(verticalTextSetting, false)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle)).resolve(preferences)
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = LTR)).resolve(preferences)
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = RTL)).resolve(preferences)
        )

        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"), readingProgression = LTR)).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ar"), readingProgression = RTL)).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"))).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"), readingProgression = LTR)).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"), readingProgression = RTL)).resolve(preferences)
        )
    }

    @Test
    fun `RTL readingProgression setting takes precedence over LTR readingProgression hint`() {
        val preferences = MutablePreferences().apply {
            set(readingProgressionSetting, RTL)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = LTR)).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("zh-tw"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-tw"), readingProgression = LTR)).resolve(preferences)
        )
    }

    @Test
    fun `LTR readingProgression setting takes precedence over RTL readingProgression hint`() {
        val preferences = MutablePreferences().apply {
            set(readingProgressionSetting, LTR)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, readingProgression = RTL)).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("zh-tw"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-tw"), readingProgression = RTL)).resolve(preferences)
        )
    }

    @Test
    fun `LTR readingProgression hint takes precedence over default RTL readingProgression`() {
        val defaults = MutablePreferences().apply {
            set(readingProgressionSetting, RTL)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle, readingProgression = LTR)).resolve()
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("ja"), readingProgression = LTR)).resolve()
        )
    }

    @Test
    fun `RTL readingProgression hint takes precedence over default LTR readingProgression`() {
        val defaults = MutablePreferences().apply {
            set(readingProgressionSetting, LTR)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle, readingProgression = RTL)).resolve()
        )
        assertEquals(
            Layout(language = Language("zh-tw"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-tw"), readingProgression = RTL)).resolve()
        )
    }

    @Test
    fun `readingProgression fallbacks to default readingProgression if there is no language, no setting and no hint`() {
        val defaults = MutablePreferences().apply {
            set(readingProgressionSetting, RTL)
        }

        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle)).resolve()
        )
    }

    @Test
    fun `readingProgression fallbacks to LTR`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle)).resolve()
        )
    }

    @Test
    fun `metadata language takes precedence over default readingProgression`() {
        val defaults = MutablePreferences().apply {
            set(readingProgressionSetting, RTL)
        }

        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"))).resolve()
        )

        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(defaults = defaults, metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh"))).resolve()
        )
    }

    @Test
    fun `RTL readingProgression setting takes precedence over metadata language`() {
        val preferences = MutablePreferences().apply {
            set(readingProgressionSetting, RTL)
        }

        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"))).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh"))).resolve(preferences)
        )
    }

    @Test
    fun `LTR readingProgression setting takes precedence over metadata language`() {
        val preferences = MutablePreferences().apply {
            set(readingProgressionSetting, LTR)
        }

        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("he"))).resolve(preferences)
        )
        assertEquals(
            Layout(language = Language("zh-tw"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("zh-tw"))).resolve(preferences)
        )
    }

    @Test
    fun `RTL readingProgression setting takes precedence over language setting`() {
        val preferences = MutablePreferences().apply {
            set(readingProgressionSetting, RTL)
            set(languageSetting, Language("en"))
        }

        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle)).resolve(preferences)
        )
    }

    @Test
    fun `LTR readingProgression setting takes precedence over language setting`() {
        val preferences = MutablePreferences().apply {
            set(readingProgressionSetting, LTR)
            set(languageSetting, Language("he"))
        }

        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle)).resolve(preferences)
        )
    }

    @Test
    fun `he language setting takes precedence over language metadata`() {
        val preferences = MutablePreferences().apply {
            set(languageSetting, Language("he"))
        }

        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"), readingProgression = LTR)).resolve(preferences)
        )
    }

    @Test
    fun `zh-tw language setting takes precedence over language metadata`() {
        val preferences = MutablePreferences().apply {
            set(languageSetting, Language("zh-tw"))
        }

        assertEquals(
            Layout(language = Language("zh-tw"), stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            LayoutResolver(metadata = Metadata(localizedTitle = fakeTitle, languages = listOf("en"), readingProgression = LTR)).resolve(preferences)
        )
    }
}
