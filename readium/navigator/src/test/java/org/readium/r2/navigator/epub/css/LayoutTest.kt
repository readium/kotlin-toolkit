package org.readium.r2.navigator.epub.css

import org.junit.Test
import org.readium.r2.navigator.epub.css.Layout.*
import org.readium.r2.shared.publication.ReadingProgression.*
import org.readium.r2.shared.util.Language
import kotlin.test.assertEquals

class LayoutTest {

    @Test
    fun `Compute the layout with automatic reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = null, hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("en"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("ar"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("fa"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("he"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("ja"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("ko"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-HK"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-Hans"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh-Hant"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh-TW"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
    }

    @Test
    fun `Compute the layout with automatic reading progression and multiple languages`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = null, hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("en"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("ar"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("fa"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("he"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("ja"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("ko"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-HK"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-Hans"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-Hant"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-TW"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
    }

    @Test
    fun `Compute the layout with LTR reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = null, hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("en"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("ar"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("fa"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout.from(language = Language("he"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("ja"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("ko"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-HK"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-Hans"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-Hant"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout.from(language = Language("zh-TW"), hasMultipleLanguages = false, readingProgression = LTR)
        )
    }

    @Test
    fun `Compute the layout with RTL reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = null, hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("en"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("en"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("ar"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("ar"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("fa"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("fa"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("he"), stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout.from(language = Language("he"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("ja"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("ja"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("ko"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("ko"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("zh"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("zh-HK"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh-HK"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("zh-Hans"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh-Hans"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("zh-Hant"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh-Hant"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(language = Language("zh-TW"), stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout.from(language = Language("zh-TW"), hasMultipleLanguages = false, readingProgression = RTL)
        )
    }

    @Test
    fun `Compute the HTML dir from the stylesheets`() {
        assertEquals(HtmlDir.Ltr, Stylesheets.Default.htmlDir)
        assertEquals(HtmlDir.Rtl, Stylesheets.Rtl.htmlDir)
        assertEquals(HtmlDir.Unspecified, Stylesheets.CjkVertical.htmlDir)
        assertEquals(HtmlDir.Ltr, Stylesheets.CjkHorizontal.htmlDir)
    }
}