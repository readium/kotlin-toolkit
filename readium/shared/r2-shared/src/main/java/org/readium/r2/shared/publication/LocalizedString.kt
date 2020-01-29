/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.optNullableString
import java.io.Serializable
import java.util.*

/**
 * Represents a string with multiple [translations] indexed by a BCP 47 language tag.
 */
data class LocalizedString(val translations: Set<Translation> = emptySet()): JSONable, Serializable {

    data class Translation(
        val language: String? = null,
        val string: String
    )

    /**
     * Shortcut to create a [LocalizedString] using a single string, without a language.
     */
    constructor(string: String): this(
        translations = setOf(Translation(string = string))
    )

    /**
     * Shortcut to create a [LocalizedString] using a map of translations indexed by the BCP 47
     * language tag.
     */
    constructor(strings: Map<String, String>): this(
        translations = strings
            .map { (language, string) -> Translation(language = language, string = string) }
            .toSet()
    )

    /**
     * The default translation for this localized string.
     */
    val defaultTranslation: Translation
        get() = findTranslationByLanguage(null)
            ?: Translation(string = "")

    /**
     * The default translation string for this localized string.
     * This is a shortcut for apps.
     */
    val string: String
        get() = defaultTranslation.string

    /**
     * Returns the first translation for the given [language] BCP–47 tag.
     * If not found, then fallback:
     *    1. on the default [Locale]
     *    2. on the undefined language
     *    3. on the English language
     *    4. the first translation in the set
     */
    fun findTranslationByLanguage(language: String?): Translation? {
        fun find(code: String?) =
            translations.firstOrNull {
                it.language?.toLowerCase(Locale.ROOT) == code?.toLowerCase(Locale.ROOT)
            }

        return find(language)
            ?: find(Locale.getDefault().toLanguageTag())
            ?: find(null)
            ?: find(UNDEFINED_LANGUAGE)
            ?: find("en")
            ?: translations.firstOrNull()
    }


    /**
     * Serializes a [LocalizedString] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        for (translation in translations) {
            put(translation.language ?: UNDEFINED_LANGUAGE, translation.string)
        }
    }

    companion object {

        /**
         * BCP-47 tag for an undefined language.
         */
        const val UNDEFINED_LANGUAGE = "UND"

        /**
         * Parses a [LocalizedString] from its RWPM JSON representation.
         * If the localized string can't be parsed, a warning will be logged with [warnings].
         *
         * "anyOf": [
         *   {
         *     "type": "string"
         *   },
         *   {
         *     "description": "The language in a language map must be a valid BCP 47 tag.",
         *     "type": "object",
         *     "patternProperties": {
         *       "^((?<grandfathered>(en-GB-oed|i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn|i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE)|(art-lojban|cel-gaulish|no-bok|no-nyn|zh-guoyu|zh-hakka|zh-min|zh-min-nan|zh-xiang))|((?<language>([A-Za-z]{2,3}(-(?<extlang>[A-Za-z]{3}(-[A-Za-z]{3}){0,2}))?)|[A-Za-z]{4}|[A-Za-z]{5,8})(-(?<script>[A-Za-z]{4}))?(-(?<region>[A-Za-z]{2}|[0-9]{3}))?(-(?<variant>[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*(-(?<extension>[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+))*(-(?<privateUse>x(-[A-Za-z0-9]{1,8})+))?)|(?<privateUse2>x(-[A-Za-z0-9]{1,8})+))$": {
         *         "type": "string"
         *       }
         *     },
         *     "additionalProperties": false,
         *     "minProperties": 1
         *   }
         * ]
         */
        fun fromJSON(json: Any?, warnings: WarningLogger? = null): LocalizedString? {
            json ?: return null

            return when (json) {
                is String -> LocalizedString(setOf(Translation(string = json)))
                is JSONObject -> fromJSONObject(json, warnings)
                else -> {
                    warnings?.log(Warning.JsonParsing(LocalizedString::class.java, "invalid localized string object"))
                    null
                }
            }
        }

        private fun fromJSONObject(json: JSONObject, warnings: WarningLogger?): LocalizedString? {
            val translations = mutableSetOf<Translation>()
            for (key in json.keys()) {
                val string = json.optNullableString(key)
                if (string == null) {
                    warnings?.log(Warning.JsonParsing(LocalizedString::class.java, "invalid localized string object", json))
                } else {
                    translations.add(Translation(key, string))
                }
            }

            return LocalizedString(translations)
        }

    }

}
