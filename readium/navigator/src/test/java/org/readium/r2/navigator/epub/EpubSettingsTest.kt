/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.readium.r2.navigator.ColumnCount
import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import kotlin.test.*

class EpubSettingsTest {

    @Test
    fun `Default values`() {
        val settings = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))
        assertEquals(ColumnCount.Auto, settings.columnCount?.value)
        assertEquals(Font.ORIGINAL, settings.font.value)
        assertEquals(listOf(Font.ORIGINAL, Font.ACCESSIBLE_DFA, Font.ROBOTO), settings.font.values)
        assertEquals(1.0, settings.fontSize.value)
        assertEquals(Overflow.PAGINATED, settings.overflow.value)
        assertTrue(settings.publisherStyles.value)
        assertEquals(Theme.Light, settings.theme.value)
        assertEquals(0.0, settings.wordSpacing.value)
        assertEquals(0.0, settings.letterSpacing.value)
    }

    @Test
    fun `Column count is only available when not in scrolled overflow`() {
        val sut = EpubSettings()
        assertNotNull(sut.update(Preferences { remove(EpubSettings.OVERFLOW) }).columnCount)
        assertNotNull(sut.update(Preferences { set(EpubSettings.OVERFLOW, Overflow.PAGINATED) }).columnCount)
        assertNull(sut.update(Preferences { set(EpubSettings.OVERFLOW, Overflow.SCROLLED) }).columnCount)
    }

    @Test
    fun `update() takes given preferences before defaults`() {
        var sut = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))

        val preferences = Preferences {
            set(sut.columnCount!!, ColumnCount.One)
            set(sut.font, Font.ROBOTO)
            set(sut.fontSize, 0.5)
            set(sut.overflow, Overflow.PAGINATED)
            set(sut.publisherStyles, false)
            set(sut.theme, Theme.Dark)
            set(sut.wordSpacing, 0.2)
            set(sut.letterSpacing, 0.2)
        }

        val defaults = Preferences {
            set(sut.columnCount!!, ColumnCount.Two)
            set(sut.font, Font.ACCESSIBLE_DFA)
            set(sut.fontSize, 0.8)
            set(sut.overflow, Overflow.SCROLLED)
            set(sut.publisherStyles, true)
            set(sut.theme, Theme.Sepia)
            set(sut.wordSpacing, 0.4)
            set(sut.letterSpacing, 0.4)
        }

        sut = sut.update(preferences = preferences, defaults = defaults)
        assertEquals(ColumnCount.One, sut.columnCount?.value)
        assertEquals(Font.ROBOTO, sut.font.value)
        assertEquals(0.5, sut.fontSize.value)
        assertEquals(Overflow.PAGINATED, sut.overflow.value)
        assertFalse(sut.publisherStyles.value)
        assertEquals(Theme.Dark, sut.theme.value)
        assertEquals(0.2, sut.wordSpacing.value)
        assertEquals(0.2, sut.letterSpacing.value)
    }

    @Test
    fun `update() falls back on defaults when preferences are missing`() {
        var sut = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))

        val defaults = Preferences {
            set(sut.columnCount!!, ColumnCount.One)
            set(sut.font, Font.ROBOTO)
            set(sut.fontSize, 0.5)
            set(sut.overflow, Overflow.PAGINATED)
            set(sut.publisherStyles, false)
            set(sut.theme, Theme.Dark)
            set(sut.wordSpacing, 0.2)
            set(sut.letterSpacing, 0.2)
        }

        sut = sut.update(preferences = Preferences(), defaults = defaults)
        assertEquals(ColumnCount.One, sut.columnCount?.value)
        assertEquals(Font.ROBOTO, sut.font.value)
        assertEquals(0.5, sut.fontSize.value)
        assertEquals(Overflow.PAGINATED, sut.overflow.value)
        assertFalse(sut.publisherStyles.value)
        assertEquals(Theme.Dark, sut.theme.value)
        assertEquals(0.2, sut.wordSpacing.value)
        assertEquals(0.2, sut.letterSpacing.value)
    }

    @Test
    fun `Unknown fonts revert to the default Original one`() {
        var sut = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))

        sut = sut.update(Preferences {
            set(sut.font, Font.ACCESSIBLE_DFA)
        })
        assertEquals(Font.ACCESSIBLE_DFA, sut.font.value)
        sut = sut.update(Preferences {
            set(sut.font, Font.PT_SERIF)
        })
        assertEquals(Font.ORIGINAL, sut.font.value)
    }

    @Test
    fun `Null overflow reverts to the default one`() {
        var sut = EpubSettings()

        sut = sut.update(Preferences {
            set(sut.overflow, Overflow.SCROLLED)
        })
        assertEquals(Overflow.SCROLLED, sut.overflow.value)

        sut = sut.update(Preferences {})
        assertEquals(Overflow.PAGINATED, sut.overflow.value)
    }

    @Test
    fun `Null theme reverts to the default one`() {
        var sut = EpubSettings()

        sut = sut.update(Preferences {
            set(sut.theme, Theme.Sepia)
        })
        assertEquals(Theme.Sepia, sut.theme.value)

        sut = sut.update(Preferences {})
        assertEquals(Theme.Light, sut.theme.value)
    }

    @Test
    fun `Word spacing requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.wordSpacing)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.wordSpacing)
        )
    }

    @Test
    fun `Activate word spacing`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4)
            )).copy {
                activate(sut.wordSpacing)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.wordSpacing)
            }
        )
    }

    @Test
    fun `Letter spacing requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.letterSpacing)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.letterSpacing)
        )
    }

    @Test
    fun `Activate letter spacing`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "letterSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "letterSpacing" to JsonPrimitive(0.4)
            )).copy {
                activate(sut.letterSpacing)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "letterSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "letterSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.letterSpacing)
            }
        )
    }

    @Test
    fun `Update Readium CSS using EPUB settings`() {
        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.Paged,
                    colCount = ColCount.Auto,
                    fontOverride = false,
                    fontFamily = null,
                    advancedSettings = false,
                    wordSpacing = Length.Relative.Rem(0.0),
                    letterSpacing = Length.Relative.Rem(0.0),
                )
            ),
            ReadiumCss().update(settings())
        )

        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.Scroll,
                    colCount = ColCount.Auto,
                    fontOverride = true,
                    fontFamily = listOf("Roboto"),
                    advancedSettings = true,
                    wordSpacing = Length.Relative.Rem(0.4),
                    letterSpacing = Length.Relative.Rem(0.3),
                )
            ),
            ReadiumCss().update(settings {
                it[overflow] = Overflow.SCROLLED
                it[theme] = Theme.Light
                it[font] = Font.ROBOTO
                it[publisherStyles] = false
                it[wordSpacing] = 0.4
                it[letterSpacing] = 0.6
            })
        )

        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.Paged,
                    colCount = ColCount.One,
                    appearance = Appearance.Night,
                    fontOverride = false,
                    advancedSettings = true,
                    wordSpacing = Length.Relative.Rem(1.0),
                    letterSpacing = Length.Relative.Rem(0.5),
                )
            ),
            ReadiumCss().update(settings {
                it[columnCount!!] = ColumnCount.One
                it[theme] = Theme.Dark
                it[publisherStyles] = false
                it[wordSpacing] = 1.0
                it[letterSpacing] = 1.0
            })
        )

        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.Paged,
                    colCount = ColCount.Two,
                    appearance = Appearance.Sepia,
                    fontOverride = false,
                    advancedSettings = false,
                    wordSpacing = Length.Relative.Rem(0.0),
                    letterSpacing = Length.Relative.Rem(0.0),
                )
            ),
            ReadiumCss().update(settings {
                it[columnCount!!] = ColumnCount.Two
                it[theme] = Theme.Sepia
            })
        )
    }

    private fun settings(init: EpubSettings.(MutablePreferences) -> Unit = {}): EpubSettings {
        val settings = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))
        return settings.update(Preferences {
            init(settings, this)
        })
    }
}