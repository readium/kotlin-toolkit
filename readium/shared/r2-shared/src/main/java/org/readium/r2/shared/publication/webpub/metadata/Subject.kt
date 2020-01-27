/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.metadata

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.publication.webpub.LocalizedString
import org.readium.r2.shared.publication.webpub.link.Link
import java.io.Serializable

/**
 * https://github.com/readium/webpub-manifest/tree/master/contexts/default#subjects
 *
 * @param sortAs Provides a string that a machine can sort.
 * @param scheme EPUB 3.1 opf:authority.
 * @param code EPUB 3.1 opf:term.
 * @param links Used to retrieve similar publications for the given subjects.
 */
data class Subject(
    val localizedName: LocalizedString,
    val sortAs: String? = null,
    val scheme: String? = null,
    val code: String? = null,
    val links: List<Link> = emptyList()
) : JSONable, Serializable {

    /**
     * Returns the default translation string for the [name].
     */
    val name: String get() = localizedName.string

    /**
     * Serializes a [Subject] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        putIfNotEmpty("name", localizedName)
        put("sortAs", sortAs)
        put("scheme", scheme)
        put("code", code)
        putIfNotEmpty("links", links)
    }

    companion object {

        /**
         * Parses a [Subject] from its RWPM JSON representation.
         * A subject can be parsed from a single string, or a full-fledged object.
         * If the subject can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: Any?, warnings: WarningLogger? = null): Subject? {
            json ?: return null

            val localizedName: LocalizedString? = when(json) {
                is String -> LocalizedString.fromJSON(json, warnings)
                is JSONObject -> LocalizedString.fromJSON(json.opt("name"), warnings)
                else -> null
            }
            if (localizedName == null) {
                warnings?.log(Warning.JsonParsing(Subject::class.java, "[name] is required"))
                return null
            }

            val json = (json as? JSONObject) ?: JSONObject()
            return Subject(
                localizedName = localizedName,
                sortAs = json.optNullableString("sortAs"),
                scheme = json.optNullableString("scheme"),
                code = json.optNullableString("code"),
                links = Link.fromJSONArray(json.optJSONArray("links"))
            )
        }

        /**
         * Creates a list of [Subject] from its RWPM JSON representation.
         * If a subject can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSONArray(json: Any?, warnings: WarningLogger? = null): List<Subject> {
            return when(json) {
                is String, is JSONObject ->
                    listOf(json).mapNotNull { fromJSON(it, warnings) }

                is JSONArray ->
                    json.parseObjects { fromJSON(it, warnings) }

                else -> emptyList()
            }
        }


    }

}
