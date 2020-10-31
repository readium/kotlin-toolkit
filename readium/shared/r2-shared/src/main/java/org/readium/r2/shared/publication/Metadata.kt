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
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import java.util.*

/**
 * https://readium.org/webpub-manifest/schema/metadata.schema.json
 *
 * @param readingProgression WARNING: This contains the reading progression as declared in the
 *     publication, so it might be [AUTO]. To lay out the content, use [effectiveReadingProgression]
 *     to get the calculated reading progression from the declared direction and the language.
 * @param otherMetadata Additional metadata for extensions, as a JSON dictionary.
 */
@Parcelize
data class Metadata(
    val identifier: String? = null, // URI
    val type: String? = null, // URI (@type)
    val localizedTitle: LocalizedString,
    val localizedSubtitle: LocalizedString? = null,
    val localizedSortAs: LocalizedString? = null,
    val modified: Date? = null,
    val published: Date? = null,
    val languages: List<String> = emptyList(), // BCP 47 tag
    val subjects: List<Subject> = emptyList(),
    val authors: List<Contributor> = emptyList(),
    val translators: List<Contributor> = emptyList(),
    val editors: List<Contributor> = emptyList(),
    val artists: List<Contributor> = emptyList(),
    val illustrators: List<Contributor> = emptyList(),
    val letterers: List<Contributor> = emptyList(),
    val pencilers: List<Contributor> = emptyList(),
    val colorists: List<Contributor> = emptyList(),
    val inkers: List<Contributor> = emptyList(),
    val narrators: List<Contributor> = emptyList(),
    val contributors: List<Contributor> = emptyList(),
    val publishers: List<Contributor> = emptyList(),
    val imprints: List<Contributor> = emptyList(),
    val readingProgression: ReadingProgression = ReadingProgression.AUTO,
    val description: String? = null,
    val duration: Double? = null,
    val numberOfPages: Int? = null,
    val belongsToCollections: List<Collection> = emptyList(),
    val belongsToSeries: List<Collection> = emptyList(),
    val otherMetadata: @WriteWith<JSONParceler> Map<String, Any> = mapOf()
) : JSONable, Parcelable {

    /**
     * Returns the default translation string for the [localizedTitle].
     */
    val title: String get() = localizedTitle.string


    /**
     * Returns the default translation string for the [localizedSortAs].
     */
    val sortAs: String? get() = localizedSortAs?.string

    /**
     * Computes a [ReadingProgression] when the value of [readingProgression] is set to
     * auto, using the publication language.
     *
     * See this issue for more details: https://github.com/readium/architecture/issues/113
     */
    @IgnoredOnParcel
    val effectiveReadingProgression: ReadingProgression get() {
        if (readingProgression != ReadingProgression.AUTO) {
            return readingProgression
        }

        // https://github.com/readium/readium-css/blob/develop/docs/CSS16-internationalization.md#missing-page-progression-direction
        if (languages.size != 1) {
            return ReadingProgression.LTR
        }

        var language = languages.first().toLowerCase(Locale.ROOT)

        if (language == "zh-hant" || language == "zh-tw") {
            return ReadingProgression.RTL
        }

        // The region is ignored for ar, fa and he.
        language = language.split("-", limit = 2).first()
        return when (language) {
            "ar", "fa", "he" -> ReadingProgression.RTL
            else -> ReadingProgression.LTR
        }
    }

    /**
     * Serializes a [Metadata] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject(otherMetadata).apply {
        put("identifier", identifier)
        put("@type", type)
        putIfNotEmpty("title", localizedTitle)
        putIfNotEmpty("subtitle", localizedSubtitle)
        put("modified", modified?.toIso8601String())
        put("published", published?.toIso8601String())
        putIfNotEmpty("language", languages)
        putIfNotEmpty("sortAs", localizedSortAs)
        putIfNotEmpty("subject", subjects)
        putIfNotEmpty("author", authors)
        putIfNotEmpty("translator", translators)
        putIfNotEmpty("editor", editors)
        putIfNotEmpty("artist", artists)
        putIfNotEmpty("illustrator", illustrators)
        putIfNotEmpty("letterer", letterers)
        putIfNotEmpty("penciler", pencilers)
        putIfNotEmpty("colorist", colorists)
        putIfNotEmpty("inker", inkers)
        putIfNotEmpty("narrator", narrators)
        putIfNotEmpty("contributor", contributors)
        putIfNotEmpty("publisher", publishers)
        putIfNotEmpty("imprint", imprints)
        put("readingProgression", readingProgression.value)
        put("description", description)
        put("duration", duration)
        put("numberOfPages", numberOfPages)
        putIfNotEmpty("belongsTo", JSONObject().apply {
            putIfNotEmpty("collection", belongsToCollections)
            putIfNotEmpty("series", belongsToSeries)
        })
    }

    /**
     * Syntactic sugar to access the [otherMetadata] values by subscripting [Metadata] directly.
     * `metadata["layout"] == metadata.otherMetadata["layout"]`
     */
    operator fun get(key: String): Any? = otherMetadata[key]

    companion object {

        /**
         * Parses a [Metadata] from its RWPM JSON representation.
         *
         * If the metadata can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): Metadata? {
            json ?: return null
            val localizedTitle = LocalizedString.fromJSON(json.remove("title"), warnings)
            if (localizedTitle == null) {
                warnings?.log(Metadata::class.java, "[title] is required", json)
                return null
            }

            val identifier = json.remove("identifier") as? String
            val type = json.remove("@type") as? String
            val localizedSubtitle = LocalizedString.fromJSON(json.remove("subtitle"), warnings)
            val modified = (json.remove("modified") as? String)?.iso8601ToDate()
            val published = (json.remove("published") as? String)?.iso8601ToDate()
            val languages = json.optStringsFromArrayOrSingle("language", remove = true)
            val localizedSortAs = LocalizedString.fromJSON(json.remove("sortAs"), warnings)
            val subjects = Subject.fromJSONArray(json.remove("subject"), normalizeHref, warnings)
            val authors = Contributor.fromJSONArray(json.remove("author"), normalizeHref, warnings)
            val translators = Contributor.fromJSONArray(json.remove("translator"), normalizeHref, warnings)
            val editors = Contributor.fromJSONArray(json.remove("editor"), normalizeHref, warnings)
            val artists = Contributor.fromJSONArray(json.remove("artist"), normalizeHref, warnings)
            val illustrators = Contributor.fromJSONArray(json.remove("illustrator"), normalizeHref, warnings)
            val letterers = Contributor.fromJSONArray(json.remove("letterer"), normalizeHref, warnings)
            val pencilers = Contributor.fromJSONArray(json.remove("penciler"), normalizeHref, warnings)
            val colorists = Contributor.fromJSONArray(json.remove("colorist"), normalizeHref, warnings)
            val inkers = Contributor.fromJSONArray(json.remove("inker"), normalizeHref, warnings)
            val narrators = Contributor.fromJSONArray(json.remove("narrator"), normalizeHref, warnings)
            val contributors = Contributor.fromJSONArray(json.remove("contributor"), normalizeHref, warnings)
            val publishers = Contributor.fromJSONArray(json.remove("publisher"), normalizeHref, warnings)
            val imprints = Contributor.fromJSONArray(json.remove("imprint"), normalizeHref, warnings)
            val readingProgression = ReadingProgression(json.remove("readingProgression") as? String)
            val description = json.remove("description") as? String
            val duration = json.optPositiveDouble("duration", remove = true)
            val numberOfPages = json.optPositiveInt("numberOfPages", remove = true)
            val belongsTo = json.remove("belongsTo") as? JSONObject
                ?: json.remove("belongs_to") as? JSONObject
            val belongsToCollections = Collection.fromJSONArray(belongsTo?.opt("collection"), normalizeHref, warnings)
            val belongsToSeries = Collection.fromJSONArray(belongsTo?.opt("series"), normalizeHref, warnings)

            return Metadata(
                identifier = identifier,
                type = type,
                localizedTitle = localizedTitle,
                localizedSubtitle = localizedSubtitle,
                localizedSortAs = localizedSortAs,
                modified = modified,
                published = published,
                languages = languages,
                subjects = subjects,
                authors = authors,
                translators = translators,
                editors = editors,
                artists = artists,
                illustrators = illustrators,
                letterers = letterers,
                pencilers = pencilers,
                colorists = colorists,
                inkers = inkers,
                narrators = narrators,
                contributors = contributors,
                publishers = publishers,
                imprints = imprints,
                readingProgression = readingProgression,
                description = description,
                duration = duration,
                numberOfPages = numberOfPages,
                belongsToCollections = belongsToCollections,
                belongsToSeries = belongsToSeries,
                otherMetadata = json.toMap()
            )
        }

    }

    @Deprecated("Use [type] instead", ReplaceWith("type"))
    val rdfType: String? get() = type

    @Deprecated("Use [localizeTitle] instead.", ReplaceWith("localizedTitle"))
    val multilanguageTitle: LocalizedString?
        get() = localizedTitle

    @Deprecated("Use [localizedTitle.get] instead", ReplaceWith("localizedTitle.translationForLanguage(key)?.string"))
    fun titleForLang(key: String): String? =
        localizedTitle.getOrFallback(key)?.string

    @Deprecated("Use [readingProgression] instead.", ReplaceWith("readingProgression"))
    val direction: String
        get() = readingProgression.value

    @Deprecated("Use [published] instead", ReplaceWith("published?.toIso8601String()"))
    val publicationDate: String?
        get() = published?.toIso8601String()

    @Deprecated("Use [presentation] instead", ReplaceWith("presentation", "org.readium.r2.shared.publication.presentation.presentation"))
    val rendition: Presentation
        get() = presentation

    @Deprecated("Access from [otherMetadata] instead", ReplaceWith("otherMetadata[\"source\"] as? String"))
    val source: String?
        get() = otherMetadata["source"] as? String

    @Deprecated("Not used anymore", ReplaceWith("null"))
    val rights: String? get() = null

    @Deprecated("Use either [belongsToCollections] or [belongsToSeries] instead", ReplaceWith("belongsToCollections"))
    val belongsTo: Unit
        get() = Unit

    @Deprecated("Renamed into [toJSON]", ReplaceWith("toJSON()"))
    fun writeJSON(): JSONObject = toJSON()

}