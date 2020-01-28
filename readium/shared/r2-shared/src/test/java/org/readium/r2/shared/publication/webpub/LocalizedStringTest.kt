/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.mockito.internal.debugging.Localized
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
            LocalizedString(mapOf(
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
                "UND": "a string"
            }""")
        )
    }

    @Test fun `get JSON`() {
        assertJSONEquals(
            LocalizedString(mapOf(
                "en" to "a string",
                "fr" to "une chaîne",
                LocalizedString.UNDEFINED_LANGUAGE to "Surgh"
            )).toJSON(),
            JSONObject("""{
                "en": "a string",
                "fr": "une chaîne",
                "UND": "Surgh"
            }""")
        )
    }

    @Test fun `get the default translation`() {
        assertEquals(
            LocalizedString.Translation("en", "a string"),
            LocalizedString(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).defaultTranslation
        )
    }

    @Test fun `get the default translation's string`() {
        assertEquals(
            "a string",
            LocalizedString(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).string
        )
    }

    @Test fun `find translation by language`() {
        assertEquals(
            LocalizedString.Translation("fr", "une chaîne"),
            LocalizedString(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).findTranslationByLanguage("fr")
        )
    }

    @Test fun `find translation by language defaults to the default Locale`() {
        val language = Locale.getDefault().toLanguageTag()
        assertEquals(
            LocalizedString.Translation(language, "a string"),
            LocalizedString(mapOf(
                language to "a string",
                "foobar" to "une chaîne"
            )).findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to null`() {
        assertEquals(
            LocalizedString.Translation(language = null, string = "Surgh"),
            LocalizedString(setOf(
                LocalizedString.Translation("foo", "a string"),
                LocalizedString.Translation("bar", "une chaîne"),
                LocalizedString.Translation(language = null, string = "Surgh")
            )).findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to undefined`() {
        assertEquals(
            LocalizedString.Translation(language = LocalizedString.UNDEFINED_LANGUAGE, string = "Surgh"),
            LocalizedString(mapOf(
                "foo" to "a string",
                "bar" to "une chaîne",
                LocalizedString.UNDEFINED_LANGUAGE to "Surgh"
            )).findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to English`() {
        assertEquals(
            LocalizedString.Translation("en", "a string"),
            LocalizedString(mapOf(
                "en" to "a string",
                "fr" to "une chaîne"
            )).findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to the first found translation`() {
        assertEquals(
            LocalizedString.Translation("fr", "une chaîne"),
            LocalizedString(mapOf(
                "fr" to "une chaîne"
            )).findTranslationByLanguage(null)
        )
    }

}