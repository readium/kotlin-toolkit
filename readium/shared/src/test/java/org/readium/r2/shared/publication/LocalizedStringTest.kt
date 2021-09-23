/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import java.util.*

class LocalizedStringTest {

    @Test fun `parse JSON string`() {
        assertEquals(
            LocalizedString("a string"),
            LocalizedString.fromJSON("a string")
        )
    }

    @Test fun `parse JSON localized strings`() {
        assertEquals(
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )),
            LocalizedString.fromJSON(JSONObject("""{
                "en": "a string",
                "fr": "une chaîne"
            }"""))
        )
    }

    @Test fun `parse invalid JSON`() {
        assertNull(LocalizedString.fromJSON(JSONArray("[1, 2]")))
    }

    @Test fun `parse null JSON`() {
        assertNull(LocalizedString.fromJSON(null))
    }

    @Test fun `get JSON with one translation and no language`() {
        assertJSONEquals(
            LocalizedString("a string").toJSON(),
            JSONObject("""{
                "und": "a string"
            }""")
        )
    }

    @Test fun `get JSON`() {
        assertJSONEquals(
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne",
                LocalizedString.UNDEFINED_LANGUAGE to "Surgh"
            )).toJSON(),
            JSONObject("""{
                "en": "a string",
                "fr": "une chaîne",
                "und": "Surgh"
            }""")
        )
    }

    @Test fun `get the default translation`() {
        assertEquals(
            LocalizedString.Translation("a string"),
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).defaultTranslation
        )
    }

    @Test fun `get the default translation's string`() {
        assertEquals(
            "a string",
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).string
        )
    }

    @Test fun `find translation by language`() {
        assertEquals(
            LocalizedString.Translation("une chaîne"),
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).getOrFallback("fr")
        )
    }

    @Test fun `find translation by language defaults to the default Locale`() {
        val language = Locale.getDefault().toLanguageTag()
        assertEquals(
            LocalizedString.Translation("a string"),
            LocalizedString.fromStrings(mapOf(
                language to "a string",
                "foobar" to "une chaîne"
            )).getOrFallback(null)
        )
    }

    @Test fun `find translation by language defaults to null`() {
        assertEquals(
            LocalizedString.Translation("Surgh"),
            LocalizedString.fromStrings(mapOf(
                "foo" to "a string",
                "bar" to "une chaîne",
                null to "Surgh"
            )).getOrFallback(null)
        )
    }

    @Test fun `find translation by language defaults to undefined`() {
        assertEquals(
            LocalizedString.Translation(string = "Surgh"),
            LocalizedString.fromStrings(mapOf(
                "foo" to "a string",
                "bar" to "une chaîne",
                LocalizedString.UNDEFINED_LANGUAGE to "Surgh"
            )).getOrFallback(null)
        )
    }

    @Test fun `find translation by language defaults to English`() {
        assertEquals(
            LocalizedString.Translation("a string"),
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).getOrFallback(null)
        )
    }

    @Test fun `find translation by language defaults to the first found translation`() {
        assertEquals(
            LocalizedString.Translation("une chaîne"),
            LocalizedString.fromStrings(mapOf(
                "fr" to "une chaîne"
            )).getOrFallback(null)
        )
    }

    @Test fun `maps the languages`() {
        assertEquals(
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )),
            LocalizedString.fromStrings(mapOf(
                null to "a string",
                "fr" to "une chaîne"
            )).mapLanguages { (language, translation) ->
                if (translation.string == "a string")
                    "en"
                else
                    language
            }
        )
    }

    @Test fun `maps the translations`() {
        assertEquals(
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )),
            LocalizedString.fromStrings(mapOf(
                "en" to "Surgh",
                "fr" to "une chaîne"
            )).mapTranslations { (language, translation) ->
                if (language == "en")
                    translation.copy(string = "a string")
                else
                    translation
            }
        )
    }

    @Test fun `add or replace a new translation`() {
        assertEquals(
            LocalizedString.fromStrings(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )),
            LocalizedString.fromStrings(mapOf(
                "en" to "a string"
            )).copyWithString("fr", "une chaîne")
        )
    }

}
