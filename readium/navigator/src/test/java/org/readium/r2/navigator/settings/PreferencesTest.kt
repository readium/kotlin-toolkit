/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.settings

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val readingProgression: EnumSetting<ReadingProgression> = EnumSetting(
    key = Setting.READING_PROGRESSION,
    value = ReadingProgression.LTR,
    values = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
)

private val fontSize: PercentSetting = PercentSetting(
    key = Setting.FONT_SIZE,
    value = 1.0,
    range = 0.4..5.0,
    suggestedSteps = listOf(0.5, 0.8, 1.0, 2.0, 3.0, 5.0)
)

private val pageMargins: RangeSetting<Double> = RangeSetting(
    key = Setting.PAGE_MARGINS,
    value = 1.0,
    range = 1.0..2.0,
    suggestedIncrement = 0.5,
)

private val columnCount: RangeSetting<Int> = RangeSetting(
    key = Setting.COLUMN_COUNT,
    value = 1,
    range = 1..5,
)

private val publisherStyles: ToggleSetting = ToggleSetting(
    key = Setting.PUBLISHER_STYLES,
    value = true,
)

private val wordSpacing: PercentSetting = PercentSetting(
    key = Setting.WORD_SPACING,
    value = 0.0,
    activator = object : SettingActivator {
        override fun isActiveWithPreferences(preferences: Preferences): Boolean =
            preferences[publisherStyles] == false

        override fun activateInPreferences(preferences: MutablePreferences) {
            preferences[publisherStyles] = false
        }
    }
)

private val theme: EnumSetting<Theme> = EnumSetting(
    key = Setting.THEME,
    value = Theme.LIGHT,
    values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
)

@RunWith(RobolectricTestRunner::class)
class PreferencesTest {

    @Test
    fun `Create from builder`() {
        assertEquals(
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(true),
                "fontSize" to JsonPrimitive(1.2)
            )),
            Preferences {
                set(publisherStyles, true)
                set(fontSize, 1.2)
            }
        )
    }

    @Test
    fun `Parse from empty JSON`() {
        assertEquals(Preferences(), Preferences.fromJson("{}"))
    }

    @Test
    fun `Parse from invalid JSON`() {
        assertNull(Preferences.fromJson(null))
        assertNull(Preferences.fromJson(""))
        assertNull(Preferences.fromJson("invalid"))
    }

    @Test
    fun `Parse from valid JSON`() {
        assertEquals(
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(false),
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )),
            Preferences.fromJson("""{"publisherStyles":false,"fontSize":1.2,"readingProgression":"ltr"}"""),
        )
    }

    @Test
    fun `Serialize to JSON`() {
        assertEquals(
            """{"publisherStyles":false,"fontSize":1.2,"readingProgression":"ltr"}""",
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(false),
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )).toJsonString()
        )
    }

    @Test
    fun `Make a copy after modifying it`() {
        assertEquals(
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(false),
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )),
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(true),
                "fontSize" to JsonPrimitive(1.2)
            )).copy {
                set(publisherStyles, false)
                set(readingProgression, ReadingProgression.LTR)
            }
        )
    }

    @Test
    fun `Get a preference`() {
        val prefs = Preferences(mapOf(
            "publisherStyles" to JsonPrimitive(false),
            "fontSize" to JsonPrimitive(1.2),
            "readingProgression" to JsonPrimitive("ltr")
        ))

        assertNull(prefs[theme])
        assertEquals(false, prefs[publisherStyles])
        assertEquals(1.2, prefs[fontSize])
        assertEquals(ReadingProgression.LTR, prefs[readingProgression])
    }

    @Test
    fun `Set a preference`() {
        assertEquals(
            Preferences(mapOf(
                "readingProgression" to JsonPrimitive("rtl"),
                "theme" to JsonPrimitive("light"),
            )),
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )).copy {
                set(readingProgression, ReadingProgression.RTL)
                set(fontSize, null)
                set(theme, Theme.LIGHT)
            }
        )
    }

    @Test
    fun `Invalid enum values are nulled out`() {
        assertEquals(
            Preferences(),
            Preferences(mapOf(
                "readingProgression" to JsonPrimitive("rtl")
            )).copy {
                set(readingProgression, ReadingProgression.TTB)
            }
        )
    }

    @Test
    fun `Out of range values are coerced into the range`() {
        assertEquals(
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(0.4),
            )),
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(0.5),
            )).copy {
                set(fontSize, 0.2)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(5.0),
            )),
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(0.5),
            )).copy {
                set(fontSize, 6.0)
            }
        )
    }

    @Test
    fun `Remove a preference`() {
        assertEquals(
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(1.2),
            )),
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )).copy {
                remove(readingProgression)
            }
        )
    }

    @Test
    fun `Clear preferences`() {
        assertEquals(
            Preferences(),
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )).copy {
                clear()
            }
        )
    }

    @Test
    fun `Merge two preferences`() {
        assertEquals(
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(false),
                "fontSize" to JsonPrimitive(1.2),
                "readingProgression" to JsonPrimitive("ltr")
            )),
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(true),
                "fontSize" to JsonPrimitive(1.2)
            )).copy {
                merge(Preferences(mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "readingProgression" to JsonPrimitive("ltr")
                )))
            }
        )
    }

    @Test
    fun `Check if a setting is active`() {
        assertFalse(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4)
            )).isActive(wordSpacing)
        )
        assertFalse(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(true)
            )).isActive(wordSpacing)
        )
        assertTrue(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(false)
            )).isActive(wordSpacing)
        )
    }

    @Test
    fun `Activate a setting`() {
        assertEquals(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4)
            )).copy {
                activate(wordSpacing)
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
                activate(wordSpacing)
            }
        )
    }

    @Test
    fun `Setting a setting automatically activates it`() {
        assertEquals(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.5),
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4)
            )).copy {
                set(wordSpacing, 0.5)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.5)
            )),
            Preferences(mapOf(
                "wordSpacing" to JsonPrimitive(0.4)
            )).copy {
                set(wordSpacing, 0.5, activate = false)
            }
        )
    }

    @Test
    fun `Updates a preference from its current value`() {
        assertEquals(
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(0.8)
            )),
            Preferences(mapOf(
                "fontSize" to JsonPrimitive(0.4)
            )).copy {
                update(fontSize) { it + 0.4 }
            }
        )
    }

    @Test
    fun `Invert a toggle setting`() {
        assertEquals(
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(true)
            )),
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(false)
            )).copy {
                toggle(publisherStyles)
            }
        )
        assertEquals(
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(false)
            )),
            Preferences(mapOf(
                "publisherStyles" to JsonPrimitive(true)
            )).copy {
                toggle(publisherStyles)
            }
        )
    }

    @Test
    fun `Toggles an enum setting`() {
        // toggles on
        assertEquals(
            Preferences(mapOf("theme" to JsonPrimitive("light"))),
            Preferences().copy {
                toggle(theme, Theme.LIGHT)
            }
        )
        assertEquals(
            Preferences(mapOf("theme" to JsonPrimitive("light"))),
            Preferences(mapOf("theme" to JsonPrimitive("dark"))).copy {
                toggle(theme, Theme.LIGHT)
            }
        )
        // toggles off
        assertEquals(
            Preferences(),
            Preferences(mapOf("theme" to JsonPrimitive("light"))).copy {
                toggle(theme, Theme.LIGHT)
            }
        )
    }

    @Test
    fun `Increment and decrement by suggested steps`() {
        Preferences {
            set(fontSize, 0.5)
            assertEquals(0.5, get(fontSize))
            increment(fontSize)
            assertEquals(0.8, get(fontSize))
            increment(fontSize)
            assertEquals(1.0, get(fontSize))
            increment(fontSize)
            assertEquals(2.0, get(fontSize))
            increment(fontSize)
            assertEquals(3.0, get(fontSize))
            increment(fontSize)
            assertEquals(5.0, get(fontSize))
            increment(fontSize)
            assertEquals(5.0, get(fontSize))
            decrement(fontSize)
            assertEquals(3.0, get(fontSize))
            decrement(fontSize)
            assertEquals(2.0, get(fontSize))
            decrement(fontSize)
            assertEquals(1.0, get(fontSize))
            decrement(fontSize)
            assertEquals(0.8, get(fontSize))
            decrement(fontSize)
            assertEquals(0.5, get(fontSize))
            decrement(fontSize)
            assertEquals(0.5, get(fontSize))

            // from unknown starting value
            set(fontSize, 0.9)
            increment(fontSize)
            assertEquals(1.0, get(fontSize))
            set(fontSize, 0.9)
            decrement(fontSize)
            assertEquals(0.8, get(fontSize))
        }
    }

    @Test
    fun `Increment and decrement by suggested increments`() {
        Preferences {
            set(pageMargins, 1.0)
            assertEquals(1.0, get(pageMargins))
            increment(pageMargins)
            assertEquals(1.5, get(pageMargins))
            increment(pageMargins)
            assertEquals(2.0, get(pageMargins))
            increment(pageMargins)
            assertEquals(2.0, get(pageMargins))
            decrement(pageMargins)
            assertEquals(1.5, get(pageMargins))
            decrement(pageMargins)
            assertEquals(1.0, get(pageMargins))
            decrement(pageMargins)
            assertEquals(1.0, get(pageMargins))

            // from arbitrary starting value
            set(pageMargins, 1.1)
            increment(pageMargins)
            assertEquals(1.6, get(pageMargins))
            increment(pageMargins)
            assertEquals(2.0, get(pageMargins))

            set(pageMargins, 1.9)
            decrement(pageMargins)
            assertEquals(1.4, get(pageMargins))
            decrement(pageMargins)
            assertEquals(1.0, get(pageMargins))
        }
    }

    @Test
    fun `Increment and decrement by provided amount`() {
        Preferences {
            set(columnCount, 1)
            assertEquals(1, get(columnCount))
            adjustBy(columnCount, 1)
            assertEquals(2, get(columnCount))
            adjustBy(columnCount, 1)
            assertEquals(3, get(columnCount))
            adjustBy(columnCount, 1)
            assertEquals(4, get(columnCount))
            adjustBy(columnCount, 1)
            assertEquals(5, get(columnCount))
            adjustBy(columnCount, 1)
            assertEquals(5, get(columnCount))
            adjustBy(columnCount, -1)
            assertEquals(4, get(columnCount))
            adjustBy(columnCount, -1)
            assertEquals(3, get(columnCount))
            adjustBy(columnCount, -1)
            assertEquals(2, get(columnCount))
            adjustBy(columnCount, -1)
            assertEquals(1, get(columnCount))
            adjustBy(columnCount, -1)
            assertEquals(1, get(columnCount))
        }
    }

    @Test
    fun `Filter in setting keys`() {
        assertEquals(
            Preferences(
                mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "readingProgression" to JsonPrimitive("ltr")
                )
            ),
            Preferences(
                mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "fontSize" to JsonPrimitive(1.2),
                    "readingProgression" to JsonPrimitive("ltr")
                )
            ).filter(Setting.PUBLISHER_STYLES, Setting.READING_PROGRESSION)
        )
    }

    @Test
    fun `Filter out setting keys`() {
        assertEquals(
            Preferences(
                mapOf(
                    "fontSize" to JsonPrimitive(1.2),
                )
            ),
            Preferences(
                mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "fontSize" to JsonPrimitive(1.2),
                    "readingProgression" to JsonPrimitive("ltr")
                )
            ).filterNot(Setting.PUBLISHER_STYLES, Setting.READING_PROGRESSION)
        )
    }

    @Test
    fun `Filter in settings`() {
        assertEquals(
            Preferences(
                mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "readingProgression" to JsonPrimitive("ltr")
                )
            ),
            Preferences(
                mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "fontSize" to JsonPrimitive(1.2),
                    "readingProgression" to JsonPrimitive("ltr")
                )
            ).filter(publisherStyles, readingProgression)
        )
    }

    @Test
    fun `Filter out settings`() {
        assertEquals(
            Preferences(
                mapOf(
                    "fontSize" to JsonPrimitive(1.2),
                )
            ),
            Preferences(
                mapOf(
                    "publisherStyles" to JsonPrimitive(false),
                    "fontSize" to JsonPrimitive(1.2),
                    "readingProgression" to JsonPrimitive("ltr")
                )
            ).filterNot(publisherStyles, readingProgression)
        )
    }
}
