/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import android.os.Parcelable
import java.util.*
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Represents a string with multiple [translations] indexed by a BCP 47 language tag.
 */
@Parcelize
public data class LocalizedString(val translations: Map<String?, Translation> = emptyMap()) : JSONable, Parcelable {

    @Parcelize
    public data class Translation(
        val string: String,
    ) : Parcelable

    /**
     * Shortcut to create a [LocalizedString] using a single string for a given language.
     */
    public constructor(value: String, lang: String? = null) : this(
        translations = mapOf(lang to Translation(string = value))
    )

    /**
     * The default translation for this localized string.
     */
    val defaultTranslation: Translation
        get() = this.getOrFallback(null)
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
     *    4. the first translation found
     */
    public fun getOrFallback(language: String?): Translation? {
        return translations[language]
            ?: translations[Locale.getDefault().toLanguageTag()]
            ?: translations[null]
            ?: translations[UNDEFINED_LANGUAGE]
            ?: translations["en"]
            ?: translations.keys.firstOrNull()?.let { translations[it] }
    }

    /**
     * Returns a new [LocalizedString] after adding (or replacing) the translation with the given
     * [language].
     */
    public fun copyWithString(language: String?, string: String): LocalizedString =
        copy(translations = translations + Pair(language, Translation(string = string)))

    /**
     * Returns a new [LocalizedString] after applying the [transform] function to each language.
     */
    public fun mapLanguages(transform: (Map.Entry<String?, Translation>) -> String?): LocalizedString =
        copy(translations = translations.mapKeys(transform))

    /**
     * Returns a new [LocalizedString] after applying the [transform] function to each translation.
     */
    public fun mapTranslations(transform: (Map.Entry<String?, Translation>) -> Translation): LocalizedString =
        copy(translations = translations.mapValues(transform))

    /**
     * Serializes a [LocalizedString] to its RWPM JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        for ((language, translation) in translations) {
            put(language ?: UNDEFINED_LANGUAGE, translation.string)
        }
    }

    public companion object {

        /**
         * BCP-47 tag for an undefined language.
         */
        public const val UNDEFINED_LANGUAGE: String = "und"

        /**
         * Shortcut to create a [LocalizedString] using a map of translations indexed by the BCP 47
         * language tag.
         */
        public fun fromStrings(strings: Map<String?, String>): LocalizedString = LocalizedString(
            translations = strings
                .mapValues { (_, string) -> Translation(string = string) }
        )

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
        public fun fromJSON(json: Any?, warnings: WarningLogger? = null): LocalizedString? {
            json ?: return null

            return when (json) {
                is String -> LocalizedString(json)
                is JSONObject -> fromJSONObject(json, warnings)
                else -> {
                    warnings?.log(LocalizedString::class.java, "invalid localized string object")
                    null
                }
            }
        }

        private fun fromJSONObject(json: JSONObject, warnings: WarningLogger?): LocalizedString {
            val translations = mutableMapOf<String?, Translation>()
            for (key in json.keys()) {
                val string = json.optNullableString(key)
                if (string == null) {
                    warnings?.log(
                        LocalizedString::class.java,
                        "invalid localized string object",
                        json
                    )
                } else {
                    translations[key] = Translation(string = string)
                }
            }

            return LocalizedString(translations)
        }
    }
}
