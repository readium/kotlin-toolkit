/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log

/**
 * https://github.com/readium/webpub-manifest/tree/master/contexts/default#subjects
 *
 * @param sortAs Provides a string that a machine can sort.
 * @param scheme EPUB 3.1 opf:authority.
 * @param code EPUB 3.1 opf:term.
 * @param links Used to retrieve similar publications for the given subjects.
 */
@Parcelize
data class Subject(
    val localizedName: LocalizedString,
    val localizedSortAs: LocalizedString? = null,
    val scheme: String? = null,
    val code: String? = null,
    val links: List<Link> = emptyList()
) : JSONable, Parcelable {

    /**
     * Shortcut to create a [Subject] using a string as [name].
     */
    constructor(name: String): this(
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
    override fun toJSON() = JSONObject().apply {
        putIfNotEmpty("name", localizedName)
        putIfNotEmpty("sortAs", localizedSortAs)
        put("scheme", scheme)
        put("code", code)
        putIfNotEmpty("links", links)
    }

    companion object {

        /**
         * Parses a [Subject] from its RWPM JSON representation.
         *
         * A subject can be parsed from a single string, or a full-fledged object.
         * The [links]' href and their children's will be normalized recursively using the
         * provided [normalizeHref] closure.
         * If the subject can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(
            json: Any?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): Subject? {
            json ?: return null

            val localizedName: LocalizedString? = when(json) {
                is String -> LocalizedString.fromJSON(json, warnings)
                is JSONObject -> LocalizedString.fromJSON(json.opt("name"), warnings)
                else -> null
            }
            if (localizedName == null) {
                warnings?.log(Subject::class.java, "[name] is required")
                return null
            }

            val jsonObject = (json as? JSONObject) ?: JSONObject()
            return Subject(
                localizedName = localizedName,
                localizedSortAs = LocalizedString.fromJSON(jsonObject.remove("sortAs"), warnings),
                scheme = jsonObject.optNullableString("scheme"),
                code = jsonObject.optNullableString("code"),
                links = Link.fromJSONArray(jsonObject.optJSONArray("links"), normalizeHref, warnings)
            )
        }

        /**
         * Creates a list of [Subject] from its RWPM JSON representation.
         *
         * The [links]' href and their children's will be normalized recursively using the
         * provided [normalizeHref] closure.
         * If a subject can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSONArray(
            json: Any?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): List<Subject> {
            return when(json) {
                is String, is JSONObject ->
                    listOf(json).mapNotNull { fromJSON(it, normalizeHref, warnings) }

                is JSONArray ->
                    json.parseObjects { fromJSON(it, normalizeHref, warnings) }

                else -> emptyList()
            }
        }

    }

}
