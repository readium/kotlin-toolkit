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
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = null, hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("en"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("ar"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("fa"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("he"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("ja"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("ko"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-HK"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-Hans"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh-Hant"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh-TW"), hasMultipleLanguages = false, readingProgression = AUTO)
        )
    }

    @Test
    fun `Compute the layout with automatic reading progression and multiple languages`() {
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = null, hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("en"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("ar"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("fa"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("he"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("ja"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("ko"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-HK"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-Hans"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-Hant"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-TW"), hasMultipleLanguages = true, readingProgression = AUTO)
        )
    }

    @Test
    fun `Compute the layout with LTR reading progression`() {
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = null, hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("en"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("ar"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("fa"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Default, readingProgression = LTR),
            Layout(language = Language("he"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("ja"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("ko"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-HK"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-Hans"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-Hant"), hasMultipleLanguages = false, readingProgression = LTR)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkHorizontal, readingProgression = LTR),
            Layout(language = Language("zh-TW"), hasMultipleLanguages = false, readingProgression = LTR)
        )
    }

    @Test
    fun `Compute the layout with RTL reading progression`() {
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = null, hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("en"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("ar"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("fa"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            Layout(language = Language("he"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("ja"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("ko"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh-HK"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh-Hans"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh-Hant"), hasMultipleLanguages = false, readingProgression = RTL)
        )
        assertEquals(
            Layout(stylesheets = Stylesheets.CjkVertical, readingProgression = TTB),
            Layout(language = Language("zh-TW"), hasMultipleLanguages = false, readingProgression = RTL)
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