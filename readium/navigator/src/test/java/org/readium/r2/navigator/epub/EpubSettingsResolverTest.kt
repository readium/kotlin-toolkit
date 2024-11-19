/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.epub.css.Layout.Stylesheets
import org.readium.r2.navigator.preferences.ReadingProgression.LTR
import org.readium.r2.navigator.preferences.ReadingProgression.RTL
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
import org.readium.r2.shared.publication.ReadingProgression.LTR as PublicationLTR
import org.readium.r2.shared.publication.ReadingProgression.RTL as PublicationRTL
import org.readium.r2.shared.util.Language
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpubSettingsResolverTest {

    private fun resolveLayout(
        languages: List<String> = emptyList(),
        readingProgression: PublicationReadingProgression? = null,
        defaults: EpubDefaults = EpubDefaults(),
        preferences: EpubPreferences = EpubPreferences(),
    ): Layout {
        val metadata = Metadata(
            localizedTitle = LocalizedString("fake title"),
            languages = languages,
            readingProgression = readingProgression
        )
        val resolver = EpubSettingsResolver(
            metadata = metadata,
            defaults = defaults
        )
        val settings = resolver.settings(preferences)
        return Layout.from(settings)
    }

    @Test
    fun `Compute the layout without any preferences or defaults`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout()
        )
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(languages = listOf("en"))
        )
        assertEquals(
            Layout(
                language = Language("ar"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(languages = listOf("ar"))
        )
        assertEquals(
            Layout(
                language = Language("fa"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(languages = listOf("fa"))
        )
        assertEquals(
            Layout(
                language = Language("he"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(languages = listOf("he"))
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(languages = listOf("ja"))
        )
        assertEquals(
            Layout(
                language = Language("ko"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(languages = listOf("ko"))
        )
        assertEquals(
            Layout(
                language = Language("zh"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(languages = listOf("zh"))
        )
        assertEquals(
            Layout(
                language = Language("zh-HK"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(languages = listOf("zh-HK"))
        )
        assertEquals(
            Layout(
                language = Language("zh-Hans"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(languages = listOf("zh-Hans"))
        )
        assertEquals(
            Layout(
                language = Language("zh-Hant"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(languages = listOf("zh-Hant"))
        )
        assertEquals(
            Layout(
                language = Language("zh-TW"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(languages = listOf("zh-TW"))
        )
    }

    @Test
    fun `Compute the layout with LTR reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout(readingProgression = PublicationLTR)
        )
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("en"))
        )
        assertEquals(
            Layout(
                language = Language("ar"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("ar"))
        )
        assertEquals(
            Layout(
                language = Language("fa"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("fa"))
        )
        assertEquals(
            Layout(
                language = Language("he"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("he"))
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("ja"))
        )
        assertEquals(
            Layout(
                language = Language("ko"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("ko"))
        )
        assertEquals(
            Layout(
                language = Language("zh"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("zh"))
        )
        assertEquals(
            Layout(
                language = Language("zh-HK"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("zh-HK"))
        )
        assertEquals(
            Layout(
                language = Language("zh-Hans"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("zh-Hans"))
        )
        assertEquals(
            Layout(
                language = Language("zh-Hant"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("zh-Hant"))
        )
        assertEquals(
            Layout(
                language = Language("zh-TW"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(readingProgression = PublicationLTR, languages = listOf("zh-TW"))
        )
    }

    @Test
    fun `Compute the layout with RTL reading progression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            resolveLayout(readingProgression = PublicationRTL)
        )
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("en"))
        )
        assertEquals(
            Layout(
                language = Language("ar"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("ar"))
        )
        assertEquals(
            Layout(
                language = Language("fa"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("fa"))
        )
        assertEquals(
            Layout(
                language = Language("he"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("he"))
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("ja"))
        )
        assertEquals(
            Layout(
                language = Language("ko"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("ko"))
        )
        assertEquals(
            Layout(
                language = Language("zh"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("zh"))
        )
        assertEquals(
            Layout(
                language = Language("zh-HK"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("zh-HK"))
        )
        assertEquals(
            Layout(
                language = Language("zh-Hans"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("zh-Hans"))
        )
        assertEquals(
            Layout(
                language = Language("zh-Hant"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("zh-Hant"))
        )
        assertEquals(
            Layout(
                language = Language("zh-TW"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(readingProgression = PublicationRTL, languages = listOf("zh-TW"))
        )
    }

    @Test
    fun `Compute the layout with vertical text force enabled`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.CjkVertical, readingProgression = LTR),
            resolveLayout(preferences = EpubPreferences(verticalText = true))
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.CjkVertical, readingProgression = LTR),
            resolveLayout(
                readingProgression = PublicationLTR,
                preferences = EpubPreferences(verticalText = true)
            )
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.CjkVertical, readingProgression = RTL),
            resolveLayout(
                readingProgression = PublicationRTL,
                preferences = EpubPreferences(verticalText = true)
            )
        )
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = LTR
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("en"),
                preferences = EpubPreferences(verticalText = true)
            )
        )
    }

    @Test
    fun `Compute the layout with vertical text force disabled`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout(preferences = EpubPreferences(verticalText = false))
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout(
                readingProgression = PublicationLTR,
                preferences = EpubPreferences(verticalText = false)
            )
        )
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            resolveLayout(
                readingProgression = PublicationRTL,
                preferences = EpubPreferences(verticalText = false)
            )
        )
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("en"),
                preferences = EpubPreferences(verticalText = false)
            )
        )
        assertEquals(
            Layout(
                language = Language("ar"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(
                readingProgression = PublicationRTL,
                languages = listOf("ar"),
                preferences = EpubPreferences(verticalText = false)
            )
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("ja"),
                preferences = EpubPreferences(verticalText = false)
            )
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("ja"),
                preferences = EpubPreferences(verticalText = false)
            )
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = RTL
            ),
            resolveLayout(
                readingProgression = PublicationRTL,
                languages = listOf("ja"),
                preferences = EpubPreferences(verticalText = false)
            )
        )
    }

    @Test
    fun `RTL readingProgression preference takes precedence over LTR readingProgression hint`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            resolveLayout(
                readingProgression = PublicationLTR,
                preferences = EpubPreferences(readingProgression = RTL)
            )
        )
        assertEquals(
            Layout(
                language = Language("zh-tw"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("zh-tw"),
                preferences = EpubPreferences(readingProgression = RTL)
            )
        )
    }

    @Test
    fun `LTR readingProgression setting takes precedence over RTL readingProgression hint`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout(
                readingProgression = PublicationRTL,
                preferences = EpubPreferences(readingProgression = LTR)
            )
        )
        assertEquals(
            Layout(
                language = Language("zh-tw"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(
                readingProgression = PublicationRTL,
                languages = listOf("zh-tw"),
                preferences = EpubPreferences(readingProgression = LTR)
            )
        )
    }

    @Test
    fun `LTR readingProgression hint takes precedence over default RTL readingProgression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout(
                readingProgression = PublicationLTR,
                defaults = EpubDefaults(readingProgression = RTL)
            )
        )
        assertEquals(
            Layout(
                language = Language("ja"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("ja"),
                defaults = EpubDefaults(readingProgression = RTL)
            )
        )
    }

    @Test
    fun `RTL readingProgression hint takes precedence over default LTR readingProgression`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            resolveLayout(
                readingProgression = PublicationRTL,
                defaults = EpubDefaults(readingProgression = LTR)
            )
        )
        assertEquals(
            Layout(
                language = Language("zh-tw"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(
                readingProgression = PublicationRTL,
                languages = listOf("zh-tw"),
                defaults = EpubDefaults(readingProgression = LTR)
            )
        )
    }

    @Test
    fun `readingProgression fallbacks to default readingProgression if there is no language, no preference and no hint`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Rtl, readingProgression = RTL),
            resolveLayout(defaults = EpubDefaults(readingProgression = RTL))
        )
    }

    @Test
    fun `readingProgression fallbacks to LTR`() {
        assertEquals(
            Layout(language = null, stylesheets = Stylesheets.Default, readingProgression = LTR),
            resolveLayout()
        )
    }

    @Test
    fun `metadata language takes precedence over default readingProgression`() {
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(
                languages = listOf("en"),
                defaults = EpubDefaults(readingProgression = RTL)
            )

        )
        assertEquals(
            Layout(
                language = Language("zh"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(
                languages = listOf("zh"),
                defaults = EpubDefaults(readingProgression = RTL)
            )
        )
    }

    @Test
    fun `RTL readingProgression preference takes precedence over metadata language`() {
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(
                languages = listOf("en"),
                preferences = EpubPreferences(readingProgression = RTL)
            )
        )
        assertEquals(
            Layout(
                language = Language("zh"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(
                languages = listOf("zh"),
                preferences = EpubPreferences(readingProgression = RTL)
            )
        )
    }

    @Test
    fun `LTR readingProgression preference takes precedence over metadata language`() {
        assertEquals(
            Layout(
                language = Language("he"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(
                languages = listOf("he"),
                preferences = EpubPreferences(readingProgression = LTR)
            )
        )
        assertEquals(
            Layout(
                language = Language("zh-tw"),
                stylesheets = Stylesheets.CjkHorizontal,
                readingProgression = LTR
            ),
            resolveLayout(
                languages = listOf("zh-tw"),
                preferences = EpubPreferences(readingProgression = LTR)
            )
        )
    }

    @Test
    fun `RTL readingProgression preference takes precedence over language preference`() {
        assertEquals(
            Layout(
                language = Language("en"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(
                preferences = EpubPreferences(readingProgression = RTL, language = Language("en"))
            )
        )
    }

    @Test
    fun `LTR readingProgression preference takes precedence over language preference`() {
        assertEquals(
            Layout(
                language = Language("he"),
                stylesheets = Stylesheets.Default,
                readingProgression = LTR
            ),
            resolveLayout(
                preferences = EpubPreferences(readingProgression = LTR, language = Language("he"))
            )
        )
    }

    @Test
    fun `he language preference takes precedence over language metadata`() {
        assertEquals(
            Layout(
                language = Language("he"),
                stylesheets = Stylesheets.Rtl,
                readingProgression = RTL
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("en"),
                preferences = EpubPreferences(language = Language("he"))
            )
        )
    }

    @Test
    fun `zh-tw language preference takes precedence over language metadata`() {
        assertEquals(
            Layout(
                language = Language("zh-tw"),
                stylesheets = Stylesheets.CjkVertical,
                readingProgression = RTL
            ),
            resolveLayout(
                readingProgression = PublicationLTR,
                languages = listOf("en"),
                preferences = EpubPreferences(language = Language("zh-tw"))
            )
        )
    }
}
