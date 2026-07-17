/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.Parcelable
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.json.optJsonArray
import org.readium.r2.shared.util.json.optNullableString
import org.readium.r2.shared.util.json.parseObjects
import org.readium.r2.shared.util.json.putIfNotEmpty
import org.readium.r2.shared.util.json.putIfNotNull
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * https://github.com/readium/webpub-manifest/tree/master/contexts/default#subjects
 *
 * @param localizedSortAs Provides a string that a machine can sort.
 * @param scheme EPUB 3.1 opf:authority.
 * @param code EPUB 3.1 opf:term.
 * @param links Used to retrieve similar publications for the given subjects.
 */
@Parcelize
public data class Subject(
    val localizedName: LocalizedString,
    val localizedSortAs: LocalizedString? = null,
    val scheme: String? = null,
    val code: String? = null,
    val links: List<Link> = emptyList(),
) : JSONable, Parcelable {

    /**
     * Shortcut to create a [Subject] using a string as [name].
     */
    public constructor(name: String) : this(
        localizedName = LocalizedString(name)
    )

    /**
     * Returns the default translation string for the [name].
     */
    val name: String get() = localizedName.string

    /**
     * Returns the default translation string for the [localizedSortAs].
     */
    val sortAs: String? get() = localizedSortAs?.string

    /**
     * Serializes a [Subject] to its RWPM JSON representation.
     */
    override fun toJSON(): JsonObject = buildJsonObject {
        putIfNotEmpty("name", localizedName)
        putIfNotEmpty("sortAs", localizedSortAs)
        putIfNotNull("scheme", scheme)
        putIfNotNull("code", code)
        putIfNotEmpty("links", links)
    }

    public companion object {

        /**
         * Parses a [Subject] from its RWPM JSON representation.
         *
         * A subject can be parsed from a single string, or a full-fledged object.
         * If the subject can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(
            json: JsonElement?,
            warnings: WarningLogger? = null,
        ): Subject? {
            json ?: return null

            val localizedName: LocalizedString? = when {
                json is JsonPrimitive && json.isString -> LocalizedString.fromJSON(json, warnings)
                json is JsonObject -> LocalizedString.fromJSON(json["name"], warnings)
                else -> null
            }
            if (localizedName == null) {
                warnings?.log(Subject::class, "[name] is required")
                return null
            }

            val jsonObject = (json as? JsonObject) ?: JsonObject(emptyMap())
            return Subject(
                localizedName = localizedName,
                localizedSortAs = LocalizedString.fromJSON(jsonObject["sortAs"], warnings),
                scheme = jsonObject.optNullableString("scheme"),
                code = jsonObject.optNullableString("code"),
                links = Link.fromJSONArray(
                    jsonObject.optJsonArray("links"),
                    warnings
                )
            )
        }

        /**
         * Creates a list of [Subject] from its RWPM JSON representation.
         *
         * If a subject can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSONArray(
            json: JsonElement?,
            warnings: WarningLogger? = null,
        ): List<Subject> {
            return when (json) {
                is JsonPrimitive, is JsonObject ->
                    listOfNotNull(fromJSON(json, warnings))

                is JsonArray ->
                    json.parseObjects { fromJSON(it, warnings) }

                else -> emptyList()
            }
        }
    }
}
