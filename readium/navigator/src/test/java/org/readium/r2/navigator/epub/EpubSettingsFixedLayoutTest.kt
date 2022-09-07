/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.Spread
import org.readium.r2.shared.util.Language
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class EpubSettingsFixedLayoutTest {

    @Test
    fun `Default values`() {
        val settings = EpubSettings.FixedLayout()
        assertNull(settings.language.value)
        assertEquals(Spread.NONE, settings.spread.value)
        assertEquals(ReadingProgression.LTR, settings.readingProgression.value)
    }

    @Test
    fun `update() takes given preferences before defaults`() {
        var sut = EpubSettings.FixedLayout()

        val preferences = Preferences {
            set(sut.language, Language("fr"))
            set(sut.readingProgression, ReadingProgression.LTR)
            set(sut.spread, Spread.BOTH)
        }

        val defaults = Preferences {
            set(sut.language, Language("en"))
            set(sut.readingProgression, ReadingProgression.RTL)
            set(sut.spread, Spread.LANDSCAPE)
        }

        sut = sut.update(preferences = preferences, defaults = defaults)
        assertEquals(Language("fr"), sut.language.value)
        assertEquals(Spread.BOTH, sut.spread.value)
        assertEquals(ReadingProgression.LTR, sut.readingProgression.value)
    }

    @Test
    fun `update() falls back on defaults when preferences are missing`() {
        var sut = EpubSettings.FixedLayout()

        val defaults = Preferences {
            set(sut.language, Language("fr"))
            set(sut.readingProgression, ReadingProgression.LTR)
            set(sut.spread, Spread.BOTH)
        }

        sut = sut.update(preferences = Preferences(), defaults = defaults)
        assertEquals(Language("fr"), sut.language.value)
        assertEquals(Spread.BOTH, sut.spread.value)
        assertEquals(ReadingProgression.LTR, sut.readingProgression.value)
    }
}