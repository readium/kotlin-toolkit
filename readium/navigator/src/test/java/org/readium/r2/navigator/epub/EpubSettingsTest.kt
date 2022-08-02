/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.shared.util.Either
import kotlin.test.*
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign

class EpubSettingsTest {

    @Test
    fun `Default values`() {
        val settings = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))
        assertEquals(ColumnCount.AUTO, settings.columnCount?.value)
        assertEquals(Font.ORIGINAL, settings.font.value)
        assertEquals(listOf(Font.ORIGINAL, Font.ACCESSIBLE_DFA, Font.ROBOTO), settings.font.values)
        assertEquals(1.0, settings.fontSize.value)
        assertTrue(settings.hyphens.value)
        assertEquals(0.0, settings.letterSpacing.value)
        assertTrue(settings.ligatures.value)
        assertEquals(1.2, settings.lineHeight.value)
        assertFalse(settings.normalizedText.value)
        assertEquals(Overflow.PAGINATED, settings.overflow.value)
        assertEquals(1.0, settings.pageMargins.value)
        assertEquals(0.0, settings.paragraphIndent.value)
        assertEquals(0.0, settings.paragraphSpacing.value)
        assertTrue(settings.publisherStyles.value)
        assertEquals(TextAlign.START, settings.textAlign.value)
        assertEquals(listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY), settings.textAlign.values)
        assertEquals(Theme.LIGHT, settings.theme.value)
        assertEquals(1.2, settings.typeScale.value)
        assertEquals(0.0, settings.wordSpacing.value)
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
            set(sut.columnCount!!, ColumnCount.ONE)
            set(sut.font, Font.ROBOTO)
            set(sut.fontSize, 0.5)
            set(sut.hyphens, false)
            set(sut.letterSpacing, 0.2)
            set(sut.ligatures, true)
            set(sut.lineHeight, 1.8)
            set(sut.normalizedText, true)
            set(sut.overflow, Overflow.PAGINATED)
            set(sut.pageMargins, 1.4)
            set(sut.paragraphIndent, 0.2)
            set(sut.paragraphSpacing, 0.4)
            set(sut.publisherStyles, false)
            set(sut.textAlign, TextAlign.LEFT)
            set(sut.theme, Theme.DARK)
            set(sut.typeScale, 1.5)
            set(sut.wordSpacing, 0.2)
        }

        val defaults = Preferences {
            set(sut.columnCount!!, ColumnCount.TWO)
            set(sut.font, Font.ACCESSIBLE_DFA)
            set(sut.fontSize, 0.8)
            set(sut.hyphens, true)
            set(sut.letterSpacing, 0.4)
            set(sut.ligatures, false)
            set(sut.lineHeight, 1.9)
            set(sut.normalizedText, false)
            set(sut.overflow, Overflow.SCROLLED)
            set(sut.pageMargins, 1.5)
            set(sut.paragraphIndent, 0.3)
            set(sut.paragraphIndent, 0.5)
            set(sut.publisherStyles, true)
            set(sut.textAlign, TextAlign.RIGHT)
            set(sut.theme, Theme.SEPIA)
            set(sut.typeScale, 1.6)
            set(sut.wordSpacing, 0.4)
        }

        sut = sut.update(preferences = preferences, defaults = defaults)
        assertEquals(ColumnCount.ONE, sut.columnCount?.value)
        assertEquals(Font.ROBOTO, sut.font.value)
        assertEquals(0.5, sut.fontSize.value)
        assertFalse(sut.hyphens.value)
        assertEquals(0.2, sut.letterSpacing.value)
        assertTrue(sut.ligatures.value)
        assertEquals(0.2, sut.letterSpacing.value)
        assertEquals(1.8, sut.lineHeight.value)
        assertTrue(sut.normalizedText.value)
        assertEquals(Overflow.PAGINATED, sut.overflow.value)
        assertEquals(1.4, sut.pageMargins.value)
        assertEquals(0.2, sut.paragraphIndent.value)
        assertEquals(0.4, sut.paragraphSpacing.value)
        assertFalse(sut.publisherStyles.value)
        assertEquals(TextAlign.LEFT, sut.textAlign.value)
        assertEquals(1.5, sut.typeScale.value)
        assertEquals(Theme.DARK, sut.theme.value)
        assertEquals(0.2, sut.wordSpacing.value)
    }

    @Test
    fun `update() falls back on defaults when preferences are missing`() {
        var sut = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))

        val defaults = Preferences {
            set(sut.columnCount!!, ColumnCount.ONE)
            set(sut.font, Font.ROBOTO)
            set(sut.fontSize, 0.5)
            set(sut.hyphens, false)
            set(sut.letterSpacing, 0.2)
            set(sut.ligatures, true)
            set(sut.lineHeight, 1.8)
            set(sut.normalizedText, true)
            set(sut.overflow, Overflow.PAGINATED)
            set(sut.pageMargins, 1.4)
            set(sut.paragraphIndent, 0.2)
            set(sut.paragraphSpacing, 0.4)
            set(sut.publisherStyles, false)
            set(sut.textAlign, TextAlign.LEFT)
            set(sut.theme, Theme.DARK)
            set(sut.typeScale, 1.4)
            set(sut.wordSpacing, 0.2)
        }

        sut = sut.update(preferences = Preferences(), defaults = defaults)
        assertEquals(ColumnCount.ONE, sut.columnCount?.value)
        assertEquals(Font.ROBOTO, sut.font.value)
        assertEquals(0.5, sut.fontSize.value)
        assertFalse(sut.hyphens.value)
        assertEquals(0.2, sut.letterSpacing.value)
        assertEquals(1.8, sut.lineHeight.value)
        assertTrue(sut.ligatures.value)
        assertTrue(sut.normalizedText.value)
        assertEquals(Overflow.PAGINATED, sut.overflow.value)
        assertEquals(1.4, sut.pageMargins.value)
        assertEquals(0.2, sut.paragraphIndent.value)
        assertEquals(0.4, sut.paragraphSpacing.value)
        assertFalse(sut.publisherStyles.value)
        assertEquals(TextAlign.LEFT, sut.textAlign.value)
        assertEquals(Theme.DARK, sut.theme.value)
        assertEquals(1.4, sut.typeScale.value)
        assertEquals(0.2, sut.wordSpacing.value)
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
            set(sut.theme, Theme.SEPIA)
        })
        assertEquals(Theme.SEPIA, sut.theme.value)

        sut = sut.update(Preferences {})
        assertEquals(Theme.LIGHT, sut.theme.value)
    }

    @Test
    fun `Unsupported text align revert to the default one`() {
        var sut = EpubSettings()

        sut = sut.update(Preferences {
            set(sut.textAlign, TextAlign.JUSTIFY)
        })
        assertEquals(TextAlign.JUSTIFY, sut.textAlign.value)
        sut = sut.update(Preferences {
            set(sut.textAlign, TextAlign.CENTER)
        })
        assertEquals(TextAlign.START, sut.textAlign.value)
    }

    @Test
    fun `Line height requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.lineHeight)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.lineHeight)
        )
    }

    @Test
    fun `Activate line height`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "lineHeight" to JsonPrimitive(1.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "lineHeight" to JsonPrimitive(1.4)
            )).copy {
                activate(sut.lineHeight)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "lineHeight" to JsonPrimitive(1.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "lineHeight" to JsonPrimitive(1.4),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.lineHeight)
            }
        )
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
    fun `Text align requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.textAlign)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.textAlign)
        )
    }

    @Test
    fun `Activate text align`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "textAlign" to JsonPrimitive("left"),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "textAlign" to JsonPrimitive("left")
            )).copy {
                activate(sut.textAlign)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "textAlign" to JsonPrimitive("left"),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "textAlign" to JsonPrimitive("left"),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.textAlign)
            }
        )
    }

    @Test
    fun `Type scale requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.typeScale)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.typeScale)
        )
    }

    @Test
    fun `Activate type scale`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "typeScale" to JsonPrimitive(1.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "typeScale" to JsonPrimitive(1.4)
            )).copy {
                activate(sut.typeScale)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "typeScale" to JsonPrimitive(1.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "typeScale" to JsonPrimitive(1.4),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.typeScale)
            }
        )
    }

    @Test
    fun `Hyphens requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.hyphens)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.hyphens)
        )
    }

    @Test
    fun `Activate hyphens`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "hyphens" to JsonPrimitive(false),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "hyphens" to JsonPrimitive(false)
            )).copy {
                activate(sut.hyphens)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "hyphens" to JsonPrimitive(false),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "hyphens" to JsonPrimitive(false),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.hyphens)
            }
        )
    }

    @Test
    fun `Ligatures requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.ligatures)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.ligatures)
        )
    }

    @Test
    fun `Activate ligatures`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "ligatures" to JsonPrimitive(false),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "ligatures" to JsonPrimitive(false)
            )).copy {
                activate(sut.ligatures)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "ligatures" to JsonPrimitive(false),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "ligatures" to JsonPrimitive(false),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.ligatures)
            }
        )
    }

    @Test
    fun `Paragraph indent requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.paragraphIndent)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.paragraphIndent)
        )
    }

    @Test
    fun `Activate paragraph indent`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "paragraphIndent" to JsonPrimitive(1.2),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "paragraphIndent" to JsonPrimitive(1.2)
            )).copy {
                activate(sut.paragraphIndent)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "paragraphIndent" to JsonPrimitive(1.2),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "paragraphIndent" to JsonPrimitive(1.2),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.paragraphIndent)
            }
        )
    }

    @Test
    fun `Paragraph spacing requires publisher styles disabled`() {
        val sut = EpubSettings()
        assertFalse(
            Preferences { set(sut.publisherStyles, true) }
                .isActive(sut.paragraphSpacing)
        )
        assertTrue(
            Preferences { set(sut.publisherStyles, false) }
                .isActive(sut.paragraphSpacing)
        )
    }

    @Test
    fun `Activate paragraph spacing`() {
        val sut = EpubSettings()
        assertEquals(
            Preferences(mapOf(
                "paragraphSpacing" to JsonPrimitive(1.2),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "paragraphSpacing" to JsonPrimitive(1.2)
            )).copy {
                activate(sut.paragraphSpacing)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "paragraphSpacing" to JsonPrimitive(1.2),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "paragraphSpacing" to JsonPrimitive(1.2),
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                activate(sut.paragraphSpacing)
            }
        )
    }

    @Test
    fun `Update Readium CSS using EPUB settings`() {
        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.PAGED,
                    colCount = ColCount.AUTO,
                    pageMargins = 1.0,
                    fontOverride = false,
                    fontFamily = null,
                    advancedSettings = false,
                    typeScale = 1.2,
                    textAlign = CssTextAlign.START,
                    lineHeight = Either(1.2),
                    paraSpacing = Length.Relative.Rem(0.0),
                    paraIndent = Length.Relative.Rem(0.0),
                    wordSpacing = Length.Relative.Rem(0.0),
                    letterSpacing = Length.Relative.Rem(0.0),
                    bodyHyphens = Hyphens.AUTO,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = false,
                )
            ),
            ReadiumCss().update(settings())
        )

        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.SCROLL,
                    colCount = ColCount.AUTO,
                    pageMargins = 1.9,
                    fontOverride = true,
                    fontFamily = listOf("Roboto"),
                    advancedSettings = true,
                    typeScale = 1.4,
                    textAlign = CssTextAlign.LEFT,
                    lineHeight = Either(1.8),
                    paraSpacing = Length.Relative.Rem(0.4),
                    paraIndent = Length.Relative.Rem(0.2),
                    wordSpacing = Length.Relative.Rem(0.4),
                    letterSpacing = Length.Relative.Rem(0.3),
                    bodyHyphens = Hyphens.NONE,
                    ligatures = Ligatures.NONE,
                    a11yNormalize = true,
                )
            ),
            ReadiumCss().update(settings {
                it[font] = Font.ROBOTO
                it[hyphens] = false
                it[letterSpacing] = 0.6
                it[ligatures] = false
                it[lineHeight] = 1.8
                it[normalizedText] = true
                it[overflow] = Overflow.SCROLLED
                it[pageMargins] = 1.9
                it[paragraphIndent] = 0.2
                it[paragraphSpacing] = 0.4
                it[publisherStyles] = false
                it[textAlign] = TextAlign.LEFT
                it[theme] = Theme.LIGHT
                it[typeScale] = 1.4
                it[wordSpacing] = 0.4
            })
        )

        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.PAGED,
                    colCount = ColCount.ONE,
                    pageMargins = 1.0,
                    appearance = Appearance.NIGHT,
                    fontOverride = false,
                    advancedSettings = true,
                    typeScale = 1.2,
                    textAlign = CssTextAlign.RIGHT,
                    lineHeight = Either(1.2),
                    paraSpacing = Length.Relative.Rem(0.0),
                    paraIndent = Length.Relative.Rem(0.0),
                    wordSpacing = Length.Relative.Rem(1.0),
                    letterSpacing = Length.Relative.Rem(0.5),
                    bodyHyphens = Hyphens.AUTO,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = false,
                )
            ),
            ReadiumCss().update(settings {
                it[columnCount!!] = ColumnCount.ONE
                it[letterSpacing] = 1.0
                it[publisherStyles] = false
                it[textAlign] = TextAlign.RIGHT
                it[theme] = Theme.DARK
                it[wordSpacing] = 1.0
            })
        )

        assertEquals(
            ReadiumCss(
                userProperties = UserProperties(
                    view = View.PAGED,
                    colCount = ColCount.TWO,
                    pageMargins = 1.0,
                    appearance = Appearance.SEPIA,
                    fontOverride = false,
                    advancedSettings = true,
                    typeScale = 1.2,
                    textAlign = CssTextAlign.JUSTIFY,
                    lineHeight = Either(1.2),
                    paraSpacing = Length.Relative.Rem(0.0),
                    paraIndent = Length.Relative.Rem(0.0),
                    wordSpacing = Length.Relative.Rem(0.0),
                    letterSpacing = Length.Relative.Rem(0.0),
                    bodyHyphens = Hyphens.AUTO,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = false,
                )
            ),
            ReadiumCss().update(settings {
                it[columnCount!!] = ColumnCount.TWO
                it[textAlign] = TextAlign.JUSTIFY
                it[theme] = Theme.SEPIA
            })
        )
    }

    @Test
    fun `Changing the font or normalizing the text activate the fontOverride flag`() {
        assertEquals(false, ReadiumCss().update(settings()).userProperties.fontOverride)

        assertEquals(true, ReadiumCss().update(settings {
            it[font] = Font.ROBOTO
        }).userProperties.fontOverride)

        assertEquals(true, ReadiumCss().update(settings {
            it[normalizedText] = true
        }).userProperties.fontOverride)
    }

    private fun settings(init: EpubSettings.(MutablePreferences) -> Unit = {}): EpubSettings {
        val settings = EpubSettings(fonts = listOf(Font.ACCESSIBLE_DFA, Font.ROBOTO))
        return settings.update(Preferences {
            init(settings, this)
        })
    }
}