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
import java.util.*

class LocalizedStringTest {

    @Test fun `parse JSON string`() {
        assertEquals(
            LocalizedString().withTranslation(string = "a string", language = null),
            LocalizedString.fromJSON("a string")
        )
    }

    @Test fun `parse JSON localized strings`() {
        assertEquals(
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr"),
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
        assertEquals(
            LocalizedString()
                .withTranslation("a string")
                .toJSON().toString(),
            JSONObject("""{
                "UND": "a string"
            }""").toString()
        )
    }

    @Test fun `get JSON`() {
        assertEquals(
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr")
                .withTranslation("Surgh")
                .toJSON().toString(),
            JSONObject("""{
                "en": "a string",
                "fr": "une chaîne",
                "UND": "Surgh"
            }""").toString()
        )
    }

    @Test fun `get the default translation`() {
        assertEquals(
            LocalizedString.Translation("en", "a string"),
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr")
                .defaultTranslation
        )
    }

    @Test fun `get the default translation's string`() {
        assertEquals(
            "a string",
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr")
                .string
        )
    }

    @Test fun `find translation by language`() {
        assertEquals(
            LocalizedString.Translation("fr", "une chaîne"),
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr")
                .findTranslationByLanguage("fr")
        )
    }

    @Test fun `find translation by language defaults to the default Locale`() {
        val language = Locale.getDefault().toLanguageTag()
        assertEquals(
            LocalizedString.Translation(language, "a string"),
            LocalizedString()
                .withTranslation(language, "a string")
                .withTranslation("une chaîne", "foobar")
                .findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to undefined`() {
        assertEquals(
            LocalizedString.Translation(string = "Surgh"),
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr")
                .withTranslation(string = "Surgh")
                .findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to English`() {
        assertEquals(
            LocalizedString.Translation("en", "a string"),
            LocalizedString()
                .withTranslation("a string", "en")
                .withTranslation("une chaîne", "fr")
                .findTranslationByLanguage(null)
        )
    }

    @Test fun `find translation by language defaults to the first found translation`() {
        assertEquals(
            LocalizedString.Translation("fr", "une chaîne"),
            LocalizedString()
                .withTranslation("une chaîne", "fr")
                .findTranslationByLanguage(null)
        )
    }

}