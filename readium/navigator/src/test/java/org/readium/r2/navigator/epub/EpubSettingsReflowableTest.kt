/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubSettings.Reflowable
import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.Color
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language
import org.robolectric.RobolectricTestRunner
import kotlin.test.*
import android.graphics.Color as AndroidColor
import org.readium.r2.navigator.epub.css.Color as CssColor
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign

@RunWith(RobolectricTestRunner::class)
class EpubSettingsReflowableTest {

    private val fontFamilies = listOf(FontFamily.ACCESSIBLE_DFA, FontFamily.ROBOTO)

    private val namedColors = mapOf(
        "red" to AndroidColor.RED,
        "green" to AndroidColor.GREEN,
        "blue" to AndroidColor.BLUE,
    )

    private fun Reflowable.update(metadata: Metadata = metadata(), preferences: Preferences = Preferences(), defaults: Preferences = Preferences()): Reflowable =
        update(
            metadata = metadata, fontFamilies = fontFamilies, namedColors = namedColors,
            preferences = preferences, defaults = defaults
        )

    @Test
    fun `Default values`() {
        val settings = Reflowable(fontFamilies = fontFamilies)
        assertEquals(Color.AUTO, settings.backgroundColor.value)
        assertEquals(ColumnCount.AUTO, settings.columnCount.value)
        assertNull(settings.fontFamily.value)
        assertEquals(listOf(null, FontFamily.ACCESSIBLE_DFA, FontFamily.ROBOTO), settings.fontFamily.values)
        assertEquals(1.0, settings.fontSize.value)
        assertEquals(true, settings.hyphens.value)
        assertEquals(ImageFilter.NONE, settings.imageFilter.value)
        assertEquals(0.0, settings.letterSpacing.value)
        assertEquals(true,  settings.ligatures.value)
        assertEquals(1.2, settings.lineHeight.value)
        assertEquals(1.0, settings.pageMargins.value)
        assertEquals(0.0, settings.paragraphIndent.value)
        assertEquals(0.0, settings.paragraphSpacing.value)
        assertTrue(settings.publisherStyles.value)
        assertEquals(false, settings.scroll.value)
        assertEquals(TextAlign.START, settings.textAlign.value)
        assertEquals(Color.AUTO, settings.textColor.value)
        assertEquals(TextNormalization.NONE, settings.textNormalization.value)
        assertEquals(listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY), settings.textAlign.values)
        assertEquals(Theme.LIGHT, settings.theme.value)
        assertEquals(1.2, settings.typeScale.value)
        assertEquals(false, settings.verticalText.value)
        assertEquals(0.0, settings.wordSpacing.value)
    }

    @Test
    fun `update() takes given preferences before defaults`() {
        var sut = Reflowable(fontFamilies = listOf(FontFamily.ACCESSIBLE_DFA, FontFamily.ROBOTO))

        val preferences = Preferences {
            set(sut.backgroundColor, Color(3))
            set(sut.columnCount, ColumnCount.ONE)
            set(sut.fontFamily, FontFamily.ROBOTO)
            set(sut.fontSize, 0.5)
            set(sut.hyphens, false)
            set(sut.letterSpacing, 0.2)
            set(sut.ligatures, true)
            set(sut.lineHeight, 1.8)
            set(sut.pageMargins, 1.4)
            set(sut.paragraphIndent, 0.3)
            set(sut.paragraphSpacing, 0.4)
            set(sut.publisherStyles, false)
            set(sut.scroll, false)
            set(sut.textAlign, TextAlign.LEFT)
            set(sut.textColor, Color(5))
            set(sut.textNormalization, TextNormalization.BOLD)
            set(sut.theme, Theme.DARK)
            set(sut.typeScale, 1.5)
            set(sut.verticalText, false)
            set(sut.wordSpacing, 0.5)
        }

        val defaults = Preferences {
            set(sut.backgroundColor, Color(4))
            set(sut.columnCount, ColumnCount.TWO)
            set(sut.fontFamily, FontFamily.ACCESSIBLE_DFA)
            set(sut.fontSize, 0.6)
            set(sut.hyphens, true)
            set(sut.letterSpacing, 0.7)
            set(sut.ligatures, false)
            set(sut.lineHeight, 1.9)
            set(sut.pageMargins, 1.5)
            set(sut.paragraphIndent, 0.8)
            set(sut.paragraphIndent, 0.9)
            set(sut.publisherStyles, true)
            set(sut.scroll, true)
            set(sut.textAlign, TextAlign.RIGHT)
            set(sut.textColor, Color(6))
            set(sut.textNormalization, TextNormalization.ACCESSIBILITY)
            set(sut.theme, Theme.SEPIA)
            set(sut.typeScale, 1.6)
            set(sut.verticalText, true)
            set(sut.wordSpacing, 1.0)
        }

        sut = sut.update(metadata(), preferences = preferences, defaults = defaults)
        assertEquals(Color(3), sut.backgroundColor.value)
        assertEquals(ColumnCount.ONE, sut.columnCount.value)
        assertEquals(FontFamily.ROBOTO, sut.fontFamily.value)
        assertEquals(0.5, sut.fontSize.value)
        assertEquals(false, sut.hyphens.value)
        assertEquals(ImageFilter.NONE, sut.imageFilter.value)
        assertTrue(sut.ligatures.value)
        assertEquals(0.2, sut.letterSpacing.value)
        assertEquals(1.8, sut.lineHeight.value)
        assertEquals(1.4, sut.pageMargins.value)
        assertEquals(0.3, sut.paragraphIndent.value)
        assertEquals(0.4, sut.paragraphSpacing.value)
        assertFalse(sut.publisherStyles.value)
        assertEquals(false, sut.scroll.value)
        assertEquals(TextAlign.LEFT, sut.textAlign.value)
        assertEquals(Color(5), sut.textColor.value)
        assertEquals(TextNormalization.BOLD, sut.textNormalization.value)
        assertEquals(1.5, sut.typeScale.value)
        assertEquals(Theme.DARK, sut.theme.value)
        assertEquals(false, sut.verticalText.value)
        assertEquals(0.5, sut.wordSpacing.value)
    }

    @Test
    fun `update() falls back on defaults when preferences are missing`() {
        var sut = Reflowable(fontFamilies = listOf(FontFamily.ACCESSIBLE_DFA, FontFamily.ROBOTO))

        val defaults = Preferences {
            set(sut.backgroundColor, Color(3))
            set(sut.columnCount, ColumnCount.ONE)
            set(sut.fontFamily, FontFamily.ROBOTO)
            set(sut.fontSize, 0.5)
            set(sut.hyphens, false)
            set(sut.letterSpacing, 0.2)
            set(sut.ligatures, true)
            set(sut.lineHeight, 1.8)
            set(sut.pageMargins, 1.4)
            set(sut.paragraphIndent, 0.2)
            set(sut.paragraphSpacing, 0.4)
            set(sut.publisherStyles, false)
            set(sut.scroll, false)
            set(sut.textAlign, TextAlign.LEFT)
            set(sut.textColor, Color(6))
            set(sut.textNormalization, TextNormalization.BOLD)
            set(sut.theme, Theme.DARK)
            set(sut.typeScale, 1.4)
            set(sut.verticalText, false)
            set(sut.wordSpacing, 0.2)
        }

        sut = sut.update(metadata(), preferences = Preferences(), defaults = defaults)
        assertEquals(Color(3), sut.backgroundColor.value)
        assertEquals(ColumnCount.ONE, sut.columnCount.value)
        assertEquals(FontFamily.ROBOTO, sut.fontFamily.value)
        assertEquals(0.5, sut.fontSize.value)
        assertEquals(false, sut.hyphens.value)
        assertEquals(ImageFilter.NONE, sut.imageFilter.value)
        assertEquals(0.2, sut.letterSpacing.value)
        assertEquals(1.8, sut.lineHeight.value)
        assertTrue(sut.ligatures.value)
        assertEquals(1.4, sut.pageMargins.value)
        assertEquals(0.2, sut.paragraphIndent.value)
        assertEquals(0.4, sut.paragraphSpacing.value)
        assertFalse(sut.publisherStyles.value)
        assertEquals(false, sut.scroll.value)
        assertEquals(TextAlign.LEFT, sut.textAlign.value)
        assertEquals(Color(6), sut.textColor.value)
        assertEquals(TextNormalization.BOLD, sut.textNormalization.value)
        assertEquals(Theme.DARK, sut.theme.value)
        assertEquals(1.4, sut.typeScale.value)
        assertEquals(false, sut.verticalText.value)
        assertEquals(0.2, sut.wordSpacing.value)
    }

    @Test
    fun `Encode and decode named colors`() {
        val sut = Reflowable(namedColors = mapOf(
            "red" to AndroidColor.RED,
            "green" to AndroidColor.GREEN,
        ))

        assertEquals(
            """{"textColor":"red","backgroundColor":"green"}""",
            Preferences {
                set(sut.textColor, Color(AndroidColor.RED))
                set(sut.backgroundColor, Color(AndroidColor.GREEN))
            }.toJsonString()
        )

        val prefs = Preferences.fromJson("""{"textColor":"red","backgroundColor":"green"}""")!!
        assertEquals(Color(AndroidColor.RED), prefs[sut.textColor])
        assertEquals(Color(AndroidColor.GREEN), prefs[sut.backgroundColor])
        assertNotEquals(Color(AndroidColor.GREEN), prefs[sut.textColor])
    }

    @Test
    fun `Unknown fonts revert to the default Original one`() {
        var sut = Reflowable(fontFamilies = listOf(FontFamily.ACCESSIBLE_DFA, FontFamily.ROBOTO))

        sut = sut.update(metadata(), Preferences {
            set(sut.fontFamily, FontFamily.ACCESSIBLE_DFA)
        })
        assertEquals(FontFamily.ACCESSIBLE_DFA, sut.fontFamily.value)
        sut = sut.update(metadata(), Preferences {
            set(sut.fontFamily, FontFamily.PT_SERIF)
        })
        assertNull(sut.fontFamily.value)
    }

    @Test
    fun `Null scroll reverts to the default one`() {
        var sut = Reflowable()

        sut = sut.update(metadata(), Preferences {
            set(sut.scroll, true)
        })
        assertEquals(true, sut.scroll.value)

        sut = sut.update(metadata(), Preferences {})
        assertEquals(false, sut.scroll.value)
    }

    @Test
    fun `Null theme reverts to the default one`() {
        var sut = Reflowable()

        sut = sut.update(metadata(), Preferences {
            set(sut.theme, Theme.SEPIA)
        })
        assertEquals(Theme.SEPIA, sut.theme.value)

        sut = sut.update(metadata(), Preferences {})
        assertEquals(Theme.LIGHT, sut.theme.value)
    }

    @Test
    fun `Unsupported text align revert to the default one`() {
        var sut = Reflowable()

        sut = sut.update(metadata(), Preferences {
            set(sut.textAlign, TextAlign.JUSTIFY)
        })
        assertEquals(TextAlign.JUSTIFY, sut.textAlign.value)
        sut = sut.update(metadata(), Preferences {
            set(sut.textAlign, TextAlign.CENTER)
        })
        assertEquals(TextAlign.START, sut.textAlign.value)
    }

    @Test
    fun `Column count requires scroll to be disabled`() {
        val sut = Reflowable()

        assertFalse(
            Preferences { set(sut.scroll, true) }
                .isActive(sut.columnCount)
        )
        assertTrue(
            Preferences { set(sut.scroll, false) }
                .isActive(sut.columnCount)
        )
    }

    @Test
    fun `Page margins requires scroll to be disabled`() {
        val sut = Reflowable()

        assertFalse(
            Preferences { set(sut.scroll, true) }
                .isActive(sut.pageMargins)
        )
        assertTrue(
            Preferences { set(sut.scroll, false) }
                .isActive(sut.pageMargins)
        )
    }

    @Test
    fun `Hyphens requires the default LTR layout and publisher styles to be disabled`() {
        val sut = Reflowable()

        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
            }.isActive(sut.hyphens)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, true)
            }.isActive(sut.hyphens)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
            }.isActive(sut.hyphens)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.hyphens)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.hyphens)
        )
    }

    @Test
    fun `Activate hyphens`() {
        val sut = Reflowable()
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
    fun `Image filter requires the Dark theme`() {
        val sut = Reflowable()

        assertFalse(
            Preferences { remove(sut.theme) }
                .isActive(sut.imageFilter)
        )
        assertFalse(
            Preferences { set(sut.theme, Theme.LIGHT) }
                .isActive(sut.imageFilter)
        )
        assertFalse(
            Preferences { set(sut.theme, Theme.SEPIA) }
                .isActive(sut.imageFilter)
        )
        assertTrue(
            Preferences { set(sut.theme, Theme.DARK) }
                .isActive(sut.imageFilter)
        )
    }

    @Test
    fun `Letter spacing requires the default LTR layout and publisher styles to be disabled`() {
        val sut = Reflowable()

        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
            }.isActive(sut.letterSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, true)
            }.isActive(sut.letterSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
            }.isActive(sut.letterSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.letterSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.letterSpacing)
        )
    }

    @Test
    fun `Activate letter spacing`() {
        val sut = Reflowable()
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
    fun `Ligatures requires the RTL layout and publisher styles to be disabled`() {
        val sut = Reflowable()

        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
            }.isActive(sut.ligatures)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, true)
            }.isActive(sut.ligatures)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
            }.isActive(sut.ligatures)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.ligatures)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.ligatures)
        )
    }

    @Test
    fun `Activate ligatures`() {
        val sut = Reflowable()
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
    fun `Paragraph indent requires the RTL or LTR layouts and publisher styles to be disabled`() {
        val sut = Reflowable()

        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
            }.isActive(sut.paragraphIndent)
        )
        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
            }.isActive(sut.paragraphIndent)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, true)
            }.isActive(sut.paragraphIndent)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.paragraphIndent)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.paragraphIndent)
        )
    }

    @Test
    fun `Activate paragraph indent`() {
        val sut = Reflowable()
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
    fun `Text align requires the RTL or LTR layouts and publisher styles to be disabled`() {
        val sut = Reflowable()

        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
            }.isActive(sut.textAlign)
        )
        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
            }.isActive(sut.textAlign)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, true)
            }.isActive(sut.textAlign)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.textAlign)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.textAlign)
        )
    }

    @Test
    fun `Activate text align`() {
        val sut = Reflowable()
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
    fun `Word spacing requires the default LTR layout and publisher styles to be disabled`() {
        val sut = Reflowable()

        assertTrue(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
            }.isActive(sut.wordSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, true)
            }.isActive(sut.wordSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
            }.isActive(sut.wordSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.RTL)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.wordSpacing)
        )
        assertFalse(
            Preferences {
                set(sut.readingProgression, ReadingProgression.LTR)
                set(sut.publisherStyles, false)
                set(sut.language, Language("ja"))
            }.isActive(sut.wordSpacing)
        )
    }

    @Test
    fun `Activate word spacing`() {
        val sut = Reflowable()
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
    fun `Line height requires publisher styles disabled`() {
        val sut = Reflowable()
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
        val sut = Reflowable()
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
    fun `Type scale requires publisher styles disabled`() {
        val sut = Reflowable()
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
        val sut = Reflowable()
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
    fun `Paragraph spacing requires publisher styles disabled`() {
        val sut = Reflowable()
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
        val sut = Reflowable()
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
            readiumCss(
                userProperties = UserProperties(
                    view = View.PAGED,
                    colCount = ColCount.AUTO,
                    pageMargins = 1.0,
                    fontOverride = false,
                    fontFamily = null,
                    darkenImages = false,
                    invertImages = false,
                    advancedSettings = false,
                    typeScale = 1.2,
                    textAlign = CssTextAlign.START,
                    lineHeight = Either(1.2),
                    paraSpacing = Length.Rem(0.0),
                    paraIndent = Length.Rem(0.0),
                    wordSpacing = Length.Rem(0.0),
                    letterSpacing = Length.Rem(0.0),
                    bodyHyphens = Hyphens.AUTO,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = false,
                    overrides = mapOf(
                        "font-weight" to null
                    )
                )
            ),
            readiumCss().update(settings())
        )

        assertEquals(
            readiumCss(
                userProperties = UserProperties(
                    view = View.SCROLL,
                    colCount = ColCount.AUTO,
                    pageMargins = 1.0,
                    textColor = CssColor.Int(3),
                    backgroundColor = CssColor.Int(4),
                    fontOverride = true,
                    fontFamily = listOf("Roboto", "sans-serif"),
                    darkenImages = false,
                    invertImages = false,
                    advancedSettings = true,
                    typeScale = 1.4,
                    textAlign = CssTextAlign.LEFT,
                    lineHeight = Either(1.8),
                    paraSpacing = Length.Rem(0.4),
                    paraIndent = Length.Rem(0.2),
                    wordSpacing = Length.Rem(0.4),
                    letterSpacing = Length.Rem(0.3),
                    bodyHyphens = Hyphens.NONE,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = false,
                    overrides = mapOf(
                        "font-weight" to "bold"
                    )
                )
            ),
            readiumCss().update(
                settings {
                    it[backgroundColor] = Color(4)
                    it[fontFamily] = FontFamily.ROBOTO
                    it[hyphens] = false
                    it[letterSpacing] = 0.6
                    it[lineHeight] = 1.8
                    it[paragraphIndent] = 0.2
                    it[paragraphSpacing] = 0.4
                    it[publisherStyles] = false
                    it[scroll] = true
                    it[textAlign] = TextAlign.LEFT
                    it[theme] = Theme.LIGHT
                    it[textColor] = Color(3)
                    it[textNormalization] = TextNormalization.BOLD
                    it[typeScale] = 1.4
                    it[wordSpacing] = 0.4
                }
            )
        )

        assertEquals(
            readiumCss(
                userProperties = UserProperties(
                    view = View.PAGED,
                    colCount = ColCount.ONE,
                    pageMargins = 1.0,
                    appearance = Appearance.NIGHT,
                    darkenImages = false,
                    invertImages = false,
                    fontOverride = true,
                    advancedSettings = true,
                    typeScale = 1.2,
                    textAlign = CssTextAlign.RIGHT,
                    lineHeight = Either(1.2),
                    paraSpacing = Length.Rem(0.0),
                    paraIndent = Length.Rem(0.0),
                    wordSpacing = Length.Rem(1.0),
                    letterSpacing = Length.Rem(0.5),
                    bodyHyphens = Hyphens.AUTO,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = true,
                    overrides = mapOf(
                        "font-weight" to null
                    )
                )
            ),
            readiumCss().update(
                settings {
                    it[columnCount] = ColumnCount.ONE
                    it[letterSpacing] = 1.0
                    it[publisherStyles] = false
                    it[textAlign] = TextAlign.RIGHT
                    it[textNormalization] = TextNormalization.ACCESSIBILITY
                    it[theme] = Theme.DARK
                    it[wordSpacing] = 1.0
                }
            )
        )

        assertEquals(
            readiumCss(
                userProperties = UserProperties(
                    view = View.PAGED,
                    colCount = ColCount.TWO,
                    pageMargins = 1.0,
                    appearance = Appearance.SEPIA,
                    fontOverride = false,
                    darkenImages = false,
                    invertImages = false,
                    advancedSettings = true,
                    typeScale = 1.2,
                    textAlign = CssTextAlign.JUSTIFY,
                    lineHeight = Either(1.2),
                    paraSpacing = Length.Rem(0.0),
                    paraIndent = Length.Rem(0.0),
                    wordSpacing = Length.Rem(0.0),
                    letterSpacing = Length.Rem(0.0),
                    bodyHyphens = Hyphens.AUTO,
                    ligatures = Ligatures.COMMON,
                    a11yNormalize = false,
                    overrides = mapOf(
                        "font-weight" to null
                    )
                )
            ),
            readiumCss().update(
                settings {
                    it[columnCount] = ColumnCount.TWO
                    it[textAlign] = TextAlign.JUSTIFY
                    it[theme] = Theme.SEPIA
                }
            )
        )
    }

    @Test
    fun `Changing image filter flags`() {
        var sut = readiumCss()
        assertNull(sut.userProperties.darkenImages)
        assertNull(sut.userProperties.invertImages)

        sut = sut.update(
            settings {
                it[theme] = Theme.DARK
            }
        )
        assertEquals(false, sut.userProperties.darkenImages)
        assertEquals(false, sut.userProperties.invertImages)

        sut = sut.update(
            settings {
                it[theme] = Theme.DARK
                it[imageFilter] = ImageFilter.NONE
            }
        )
        assertEquals(false, sut.userProperties.darkenImages)
        assertEquals(false, sut.userProperties.invertImages)

        sut = sut.update(
            settings {
                it[theme] = Theme.DARK
                it[imageFilter] = ImageFilter.DARKEN
            }
        )
        assertEquals(true, sut.userProperties.darkenImages)
        assertEquals(false, sut.userProperties.invertImages)

        sut = sut.update(
            settings {
                it[theme] = Theme.DARK
                it[imageFilter] = ImageFilter.INVERT
            }
        )
        assertEquals(false, sut.userProperties.darkenImages)
        assertEquals(true, sut.userProperties.invertImages)
    }

    @Test
    fun `Changing the font or normalizing the text for accessibility activate the fontOverride flag`() {
        assertEquals(false, readiumCss().update(settings()).userProperties.fontOverride)

        assertEquals(true, readiumCss().update(
            settings {
                it[fontFamily] = FontFamily.ROBOTO
            }
        ).userProperties.fontOverride)

        assertEquals(true, readiumCss().update(
            settings {
                it[textNormalization] = TextNormalization.ACCESSIBILITY
            }
        ).userProperties.fontOverride)
    }

    @Test
    fun `Changing the language or reading progression updates the layout`() {
        val metadata = metadata(language = "ar", ReadingProgression.RTL)

        assertEquals(
            Layout(language = Language("ar"), stylesheets = Layout.Stylesheets.Rtl, readingProgression = ReadingProgression.RTL),
            readiumCss().update(settings(metadata)).layout
        )

        assertEquals(
            Layout(language = Language("fr"), stylesheets = Layout.Stylesheets.Rtl, readingProgression = ReadingProgression.RTL),
            readiumCss().update(
                settings(metadata) {
                    it[language] = Language("fr")
                }
            ).layout
        )

        assertEquals(
            Layout(language = Language("fr"), stylesheets = Layout.Stylesheets.Default, readingProgression = ReadingProgression.LTR),
            readiumCss().update(
                settings(metadata) {
                    it[language] = Language("fr")
                    it[readingProgression] = ReadingProgression.LTR
                }
            ).layout
        )

        assertEquals(
            Layout(language = Language("ar"), stylesheets = Layout.Stylesheets.Default, readingProgression = ReadingProgression.LTR),
            readiumCss().update(
                settings(metadata) {
                    it[readingProgression] = ReadingProgression.LTR
                }
            ).layout
        )
    }

    @Test
    fun `Font families are added with their alternate fallbacks`() {
        val f1 = FontFamily("Times New Roman")
        val f2 = FontFamily("Arial", alternate = f1)
        val f3 = FontFamily("Helvetica", alternate = f2)
        val ffs = listOf(f1, f2, f3)

        assertEquals(
            listOf("Times New Roman"),
            readiumCss().update(settings(fontFamilies = ffs) { it[fontFamily] = f1 }).userProperties.fontFamily
        )

        assertEquals(
            listOf("Arial", "Times New Roman"),
            readiumCss().update(settings(fontFamilies = ffs) { it[fontFamily] = f2 }).userProperties.fontFamily
        )

        assertEquals(
            listOf("Helvetica", "Arial", "Times New Roman"),
            readiumCss().update(settings(fontFamilies = ffs) { it[fontFamily] = f3 }).userProperties.fontFamily
        )
    }

    private fun readiumCss(userProperties: UserProperties = UserProperties()): ReadiumCss =
        ReadiumCss(
            userProperties = userProperties,
            assetsBaseHref = "/assets/"
        )

    private fun settings(
        metadata: Metadata = metadata(),
        fontFamilies: List<FontFamily> = listOf(FontFamily.ACCESSIBLE_DFA, FontFamily.ROBOTO),
        init: Reflowable.(MutablePreferences) -> Unit = {}
    ): Reflowable {
        val settings = Reflowable(fontFamilies = fontFamilies)
        return settings.update(
            metadata = metadata,
            preferences = Preferences { init(settings, this) }
        )
    }

    private fun metadata(language: String? = null, readingProgression: ReadingProgression = ReadingProgression.AUTO): Metadata =
        Metadata(
            localizedTitle = LocalizedString(""),
            languages = listOfNotNull(language),
            readingProgression = readingProgression
        )

    private val rtlMetadata = metadata(language = "ar", readingProgression = ReadingProgression.RTL)
    private val cjkHorizontalMetadata = metadata(language = "ja", readingProgression = ReadingProgression.LTR)
    private val cjkVerticalMetadata = metadata(language = "ja", readingProgression = ReadingProgression.RTL)
}