/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import org.junit.Test
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.Spread
import org.readium.r2.shared.util.Language
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EpubSettingsFixedLayoutTest {

    @Test
    fun `Default values`() {
        val settings = EpubSettings.FixedLayout()
        assertNull(settings.language.value)
        assertEquals(Spread.AUTO, settings.spread.value)
        assertEquals(ReadingProgression.LTR, settings.readingProgression.value)
        assertEquals(Theme.LIGHT, settings.theme.value)
    }

    @Test
    fun `update() takes given preferences before defaults`() {
        var sut = EpubSettings.FixedLayout()

        val preferences = Preferences {
            set(sut.language, Language("fr"))
            set(sut.readingProgression, ReadingProgression.LTR)
            set(sut.spread, Spread.BOTH)
            set(sut.theme, Theme.DARK)
        }

        val defaults = Preferences {
            set(sut.language, Language("en"))
            set(sut.readingProgression, ReadingProgression.RTL)
            set(sut.spread, Spread.LANDSCAPE)
            set(sut.theme, Theme.SEPIA)
        }

        sut = sut.update(metadata(), preferences = preferences, defaults = defaults)
        assertEquals(Language("fr"), sut.language.value)
        assertEquals(Spread.BOTH, sut.spread.value)
        assertEquals(ReadingProgression.LTR, sut.readingProgression.value)
        assertEquals(Theme.DARK, sut.theme.value)
    }

    @Test
    fun `update() falls back on defaults when preferences are missing`() {
        var sut = EpubSettings.FixedLayout()

        val defaults = Preferences {
            set(sut.language, Language("fr"))
            set(sut.readingProgression, ReadingProgression.LTR)
            set(sut.spread, Spread.BOTH)
            set(sut.theme, Theme.DARK)
        }

        sut = sut.update(metadata(), preferences = Preferences(), defaults = defaults)
        assertEquals(Language("fr"), sut.language.value)
        assertEquals(Spread.BOTH, sut.spread.value)
        assertEquals(ReadingProgression.LTR, sut.readingProgression.value)
        assertEquals(Theme.DARK, sut.theme.value)
    }

    private fun metadata(language: String? = null, readingProgression: ReadingProgression = ReadingProgression.AUTO): Metadata =
        Metadata(
            localizedTitle = LocalizedString(""),
            languages = listOfNotNull(language),
            readingProgression = readingProgression
        )
}