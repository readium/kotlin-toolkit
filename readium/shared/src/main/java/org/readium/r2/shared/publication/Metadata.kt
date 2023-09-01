/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import java.util.Date
import java.util.Locale
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.optPositiveDouble
import org.readium.r2.shared.extensions.optPositiveInt
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.toIso8601String
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * https://readium.org/webpub-manifest/schema/metadata.schema.json
 *
 * @param readingProgression WARNING: This contains the reading progression as declared in the
 *     publication, so it might be [AUTO]. To lay out the content, use [effectiveReadingProgression]
 *     to get the calculated reading progression from the declared direction and the language.
 * @param otherMetadata Additional metadata for extensions, as a JSON dictionary.
 */
@Parcelize
public data class Metadata(
    val identifier: String? = null, // URI
    val type: String? = null, // URI (@type)
    val conformsTo: Set<Publication.Profile> = emptySet(),
    val localizedTitle: LocalizedString? = null,
    val localizedSubtitle: LocalizedString? = null,
    val localizedSortAs: LocalizedString? = null,
    val modified: Date? = null,
    val published: Date? = null,
    val accessibility: Accessibility? = null,
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
    val readingProgression: ReadingProgression? = null,
    val description: String? = null,
    val duration: Double? = null,
    val numberOfPages: Int? = null,
    val belongsTo: Map<String, List<Collection>> = emptyMap(),
    val otherMetadata: @WriteWith<JSONParceler> Map<String, Any> = mapOf()
) : JSONable, Parcelable {

    public constructor(
        identifier: String? = null, // URI
        type: String? = null, // URI (@type)
        conformsTo: Set<Publication.Profile> = emptySet(),
        localizedTitle: LocalizedString? = null,
        localizedSubtitle: LocalizedString? = null,
        localizedSortAs: LocalizedString? = null,
        modified: Date? = null,
        published: Date? = null,
        accessibility: Accessibility? = null,
        languages: List<String> = emptyList(), // BCP 47 tag
        subjects: List<Subject> = emptyList(),
        authors: List<Contributor> = emptyList(),
        translators: List<Contributor> = emptyList(),
        editors: List<Contributor> = emptyList(),
        artists: List<Contributor> = emptyList(),
        illustrators: List<Contributor> = emptyList(),
        letterers: List<Contributor> = emptyList(),
        pencilers: List<Contributor> = emptyList(),
        colorists: List<Contributor> = emptyList(),
        inkers: List<Contributor> = emptyList(),
        narrators: List<Contributor> = emptyList(),
        contributors: List<Contributor> = emptyList(),
        publishers: List<Contributor> = emptyList(),
        imprints: List<Contributor> = emptyList(),
        readingProgression: ReadingProgression? = null,
        description: String? = null,
        duration: Double? = null,
        numberOfPages: Int? = null,
        belongsTo: Map<String, List<Collection>> = emptyMap(),
        belongsToCollections: List<Collection> = emptyList(),
        belongsToSeries: List<Collection> = emptyList(),
        otherMetadata: Map<String, Any> = mapOf()
    ) : this(
        identifier = identifier,
        type = type,
        conformsTo = conformsTo,
        localizedTitle = localizedTitle,
        localizedSubtitle = localizedSubtitle,
        localizedSortAs = localizedSortAs,
        modified = modified,
        published = published,
        accessibility = accessibility,
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
        belongsTo = belongsTo
            .toMutableMap()
            .apply {
                if (belongsToCollections.isNotEmpty()) {
                    this["collection"] = belongsToCollections
                }
                if (belongsToSeries.isNotEmpty()) {
                    this["series"] = belongsToSeries
                }
            }
            .toMap(),
        otherMetadata = otherMetadata
    )

    /**
     * Returns the default translation string for the [localizedTitle].
     */
    val title: String? get() = localizedTitle?.string

    /**
     * Returns the default translation string for the [localizedSortAs].
     */
    val sortAs: String? get() = localizedSortAs?.string

    val belongsToCollections: List<Collection> get() =
        belongsTo["collection"] ?: emptyList()

    val belongsToSeries: List<Collection> get() =
        belongsTo["series"] ?: emptyList()

    /**
     * Returns the [Language] resolved from the declared BCP 47 primary language.
     */
    @IgnoredOnParcel
    val language: Language? by lazy {
        languages.firstOrNull()?.let { Language(it) }
    }

    /**
     * Computes a [ReadingProgression] when the value of [readingProgression] is set to
     * auto, using the publication language.
     *
     * See this issue for more details: https://github.com/readium/architecture/issues/113
     */
    @Deprecated(
        "You should resolve [ReadingProgression.AUTO] by yourself.",
        level = DeprecationLevel.WARNING
    )
    @IgnoredOnParcel
    val effectiveReadingProgression: ReadingProgression get() {
        if (readingProgression != null) {
            return readingProgression
        }

        // https://github.com/readium/readium-css/blob/develop/docs/CSS16-internationalization.md#missing-page-progression-direction
        if (languages.size != 1) {
            return ReadingProgression.LTR
        }

        var language = languages.first().lowercase(Locale.ROOT)

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
    override fun toJSON(): JSONObject = JSONObject(otherMetadata).apply {
        put("identifier", identifier)
        put("@type", type)
        putIfNotEmpty("conformsTo", conformsTo.map { it.uri })
        putIfNotEmpty("title", localizedTitle)
        putIfNotEmpty("subtitle", localizedSubtitle)
        put("modified", modified?.toIso8601String())
        put("published", published?.toIso8601String())
        put("accessibility", accessibility?.toJSON())
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
        put("readingProgression", readingProgression?.value ?: "auto")
        put("description", description)
        put("duration", duration)
        put("numberOfPages", numberOfPages)
        putIfNotEmpty("belongsTo", belongsTo)
    }

    /**
     * Syntactic sugar to access the [otherMetadata] values by subscripting [Metadata] directly.
     * `metadata["layout"] == metadata.otherMetadata["layout"]`
     */
    public operator fun get(key: String): Any? = otherMetadata[key]

    @ExperimentalReadiumApi
    public fun copy(transformer: ManifestTransformer): Metadata =
        transformer.transform(
            copy(
                subjects = subjects.copy(transformer),
                authors = authors.copy(transformer),
                translators = translators.copy(transformer),
                editors = editors.copy(transformer),
                artists = artists.copy(transformer),
                illustrators = illustrators.copy(transformer),
                letterers = letterers.copy(transformer),
                pencilers = pencilers.copy(transformer),
                colorists = colorists.copy(transformer),
                inkers = inkers.copy(transformer),
                narrators = narrators.copy(transformer),
                contributors = contributors.copy(transformer),
                publishers = publishers.copy(transformer),
                imprints = imprints.copy(transformer),
                belongsTo = belongsTo.copy(transformer)
            )
        )

    public companion object {

        /**
         * Parses a [Metadata] from its RWPM JSON representation.
         *
         * If the metadata can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(
            json: JSONObject?,
            mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever(),
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
            val conformsTo = json.optStringsFromArrayOrSingle("conformsTo", remove = true)
                .map { Publication.Profile(it) }
                .toSet()
            val localizedSubtitle = LocalizedString.fromJSON(json.remove("subtitle"), warnings)
            val modified = (json.remove("modified") as? String)?.iso8601ToDate()
            val published = (json.remove("published") as? String)?.iso8601ToDate()
            val accessibility = Accessibility.fromJSON(json.remove("accessibility"))
            val languages = json.optStringsFromArrayOrSingle("language", remove = true)
            val localizedSortAs = LocalizedString.fromJSON(json.remove("sortAs"), warnings)
            val subjects = Subject.fromJSONArray(
                json.remove("subject"),
                mediaTypeRetriever,
                warnings
            )
            val authors = Contributor.fromJSONArray(
                json.remove("author"),
                mediaTypeRetriever,
                warnings
            )
            val translators = Contributor.fromJSONArray(
                json.remove("translator"),
                mediaTypeRetriever,
                warnings
            )
            val editors = Contributor.fromJSONArray(
                json.remove("editor"),
                mediaTypeRetriever,
                warnings
            )
            val artists = Contributor.fromJSONArray(
                json.remove("artist"),
                mediaTypeRetriever,
                warnings
            )
            val illustrators = Contributor.fromJSONArray(
                json.remove("illustrator"),
                mediaTypeRetriever,
                warnings
            )
            val letterers = Contributor.fromJSONArray(
                json.remove("letterer"),
                mediaTypeRetriever,
                warnings
            )
            val pencilers = Contributor.fromJSONArray(
                json.remove("penciler"),
                mediaTypeRetriever,
                warnings
            )
            val colorists = Contributor.fromJSONArray(
                json.remove("colorist"),
                mediaTypeRetriever,
                warnings
            )
            val inkers = Contributor.fromJSONArray(
                json.remove("inker"),
                mediaTypeRetriever,
                warnings
            )
            val narrators = Contributor.fromJSONArray(
                json.remove("narrator"),
                mediaTypeRetriever,
                warnings
            )
            val contributors = Contributor.fromJSONArray(
                json.remove("contributor"),
                mediaTypeRetriever,
                warnings
            )
            val publishers = Contributor.fromJSONArray(
                json.remove("publisher"),
                mediaTypeRetriever,
                warnings
            )
            val imprints = Contributor.fromJSONArray(
                json.remove("imprint"),
                mediaTypeRetriever,
                warnings
            )
            val readingProgression = ReadingProgression(
                json.remove("readingProgression") as? String
            )
            val description = json.remove("description") as? String
            val duration = json.optPositiveDouble("duration", remove = true)
            val numberOfPages = json.optPositiveInt("numberOfPages", remove = true)

            val belongsToJson = (
                json.remove("belongsTo") as? JSONObject
                    ?: json.remove("belongs_to") as? JSONObject
                    ?: JSONObject()
                )

            val belongsTo = mutableMapOf<String, List<Collection>>()
            for (key in belongsToJson.keys()) {
                if (!belongsToJson.isNull(key)) {
                    val value = belongsToJson.get(key)
                    belongsTo[key] = Collection.fromJSONArray(
                        value,
                        mediaTypeRetriever,
                        warnings
                    )
                }
            }

            return Metadata(
                identifier = identifier,
                type = type,
                conformsTo = conformsTo,
                localizedTitle = localizedTitle,
                localizedSubtitle = localizedSubtitle,
                localizedSortAs = localizedSortAs,
                modified = modified,
                published = published,
                accessibility = accessibility,
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
                belongsTo = belongsTo.toMap(),
                otherMetadata = json.toMap()
            )
        }
    }

    @Deprecated("Use [type] instead", ReplaceWith("type"), level = DeprecationLevel.ERROR)
    val rdfType: String? get() = type

    @Deprecated(
        "Use [localizeTitle] instead.",
        ReplaceWith("localizedTitle"),
        level = DeprecationLevel.ERROR
    )
    val multilanguageTitle: LocalizedString?
        get() = localizedTitle

    @Deprecated(
        "Use [localizedTitle.get] instead",
        ReplaceWith("localizedTitle.translationForLanguage(key)?.string"),
        level = DeprecationLevel.ERROR
    )
    public fun titleForLang(key: String): String? =
        localizedTitle?.getOrFallback(key)?.string

    @Deprecated(
        "Use [readingProgression] instead.",
        ReplaceWith("readingProgression"),
        level = DeprecationLevel.ERROR
    )
    val direction: String
        get() {
            throw NotImplementedError()
        }

    @Deprecated(
        "Use [published] instead",
        ReplaceWith("published?.toIso8601String()"),
        level = DeprecationLevel.ERROR
    )
    val publicationDate: String?
        get() = published?.toIso8601String()

    @Deprecated(
        "Use [presentation] instead",
        ReplaceWith("presentation", "org.readium.r2.shared.publication.presentation.presentation"),
        level = DeprecationLevel.ERROR
    )
    val rendition: Presentation
        get() = presentation

    @Deprecated(
        "Access from [otherMetadata] instead",
        ReplaceWith("otherMetadata[\"source\"] as? String"),
        level = DeprecationLevel.ERROR
    )
    val source: String?
        get() = otherMetadata["source"] as? String

    @Deprecated("Not used anymore", ReplaceWith("null"), level = DeprecationLevel.ERROR)
    val rights: String? get() = null

    @Deprecated("Renamed into [toJSON]", ReplaceWith("toJSON()"), level = DeprecationLevel.ERROR)
    public fun writeJSON(): JSONObject = toJSON()
}
