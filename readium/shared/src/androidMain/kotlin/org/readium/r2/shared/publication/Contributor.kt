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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableDouble
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Contributor Object for the Readium Web Publication Manifest.
 * https://readium.org/webpub-manifest/schema/contributor-object.schema.json
 *
 * @param localizedName The name of the contributor.
 * @param localizedSortAs The string used to sort the name of the contributor.
 * @param identifier An unambiguous reference to this contributor.
 * @param roles The roles of the contributor in the publication making.
 * @param position The position of the publication in this collection/series,
 *     when the contributor represents a collection.
 * @param links Used to retrieve similar publications for the given contributor.
 */
@Parcelize
public data class Contributor(
    val localizedName: LocalizedString,
    val localizedSortAs: LocalizedString? = null,
    val identifier: String? = null,
    val roles: Set<String> = emptySet(),
    val position: Double? = null,
    val links: List<Link> = emptyList(),
) : JSONable, Parcelable {

    /**
     * Shortcut to create a [Contributor] using a string as [name].
     */
    public constructor(name: String) : this(
        localizedName = LocalizedString(name)
    )

    /**
     * Returns the default translation string for the [localizedName].
     */
    val name: String get() = localizedName.string

    /**
     * Returns the default translation string for the [localizedSortAs].
     */
    val sortAs: String? get() = localizedSortAs?.string

    /**
     * Serializes a [Subject] to its RWPM JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        putIfNotEmpty("name", localizedName)
        put("identifier", identifier)
        putIfNotEmpty("sortAs", localizedSortAs)
        putIfNotEmpty("role", roles)
        put("position", position)
        putIfNotEmpty("links", links)
    }

    public companion object {

        /**
         * Parses a [Contributor] from its RWPM JSON representation.
         *
         * A contributor can be parsed from a single string, or a full-fledged object.
         * If the contributor can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(
            json: Any?,
            warnings: WarningLogger? = null,
        ): Contributor? {
            json ?: return null

            val localizedName: LocalizedString? = when (json) {
                is String -> LocalizedString.fromJSON(json, warnings)
                is JSONObject -> LocalizedString.fromJSON(json.opt("name"), warnings)
                else -> null
            }
            if (localizedName == null) {
                warnings?.log(Contributor::class.java, "[name] is required")
                return null
            }

            val jsonObject = (json as? JSONObject) ?: JSONObject()
            return Contributor(
                localizedName = localizedName,
                identifier = jsonObject.optNullableString("identifier"),
                localizedSortAs = LocalizedString.fromJSON(jsonObject.remove("sortAs"), warnings),
                roles = jsonObject.optStringsFromArrayOrSingle("role").toSet(),
                position = jsonObject.optNullableDouble("position"),
                links = Link.fromJSONArray(
                    jsonObject.optJSONArray("links"),
                    warnings
                )
            )
        }

        /**
         * Creates a list of [Contributor] from its RWPM JSON representation.
         *
         * If a contributor can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSONArray(
            json: Any?,
            warnings: WarningLogger? = null,
        ): List<Contributor> {
            return when (json) {
                is String, is JSONObject ->
                    listOf(json).mapNotNull {
                        fromJSON(
                            it,
                            warnings
                        )
                    }

                is JSONArray ->
                    json.parseObjects { fromJSON(it, warnings) }

                else -> emptyList()
            }
        }
    }
}
