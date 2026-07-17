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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.Parcelable
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.WriteWith
import org.readium.r2.shared.util.fromLegacyHref
import org.readium.r2.shared.util.json.JsonMapParceler
import org.readium.r2.shared.util.json.optJsonArray
import org.readium.r2.shared.util.json.optJsonObject
import org.readium.r2.shared.util.json.optNullableDouble
import org.readium.r2.shared.util.json.optNullableInt
import org.readium.r2.shared.util.json.optNullableString
import org.readium.r2.shared.util.json.optPositiveInt
import org.readium.r2.shared.util.json.optStringsFromArrayOrSingle
import org.readium.r2.shared.util.json.parseObjects
import org.readium.r2.shared.util.json.putAll
import org.readium.r2.shared.util.json.putIfNotEmpty
import org.readium.r2.shared.util.json.putIfNotNull
import org.readium.r2.shared.util.json.toMap
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents a precise location in a publication in a format that can be stored and shared.
 *
 * There are many different use cases for locators:
 *  - getting back to the last position in a publication
 *  - bookmarks
 *  - highlights & annotations
 *  - search results
 *  - human-readable (and shareable) reference in a publication
 *
 * https://readium.org/architecture/models/locators/
 */
@Parcelize
public data class Locator(
    val href: Url,
    val mediaType: MediaType,
    val title: String? = null,
    val locations: Locations = Locations(),
    val text: Text = Text(),
) : JSONable, Parcelable {

    /**
     * One or more alternative expressions of the location.
     * https://github.com/readium/architecture/tree/master/models/locators#the-location-object
     *
     * @param fragments Contains one or more fragment in the resource referenced by the [Locator].
     * @param progression Progression in the resource expressed as a percentage (between 0 and 1).
     * @param position An index in the publication (>= 1).
     * @param totalProgression Progression in the publication expressed as a percentage (between 0
     *        and 1).
     * @param otherLocations Additional locations for extensions.
     */
    @Parcelize
    public data class Locations(
        val fragments: List<String> = emptyList(),
        val progression: Double? = null,
        val position: Int? = null,
        val totalProgression: Double? = null,
        val otherLocations: @WriteWith<JsonMapParceler> Map<String, Any> = emptyMap(),
    ) : JSONable, Parcelable {

        override fun toJSON(): JsonObject = buildJsonObject {
            // Like with the legacy org.json serialization, the keys handled by this serializer
            // always shadow (or remove) a value with the same key in [otherLocations].
            putAll(
                otherLocations.filterKeys {
                    it !in setOf("fragments", "progression", "position", "totalProgression")
                }
            )
            putIfNotEmpty("fragments", fragments)
            putIfNotNull("progression", progression)
            putIfNotNull("position", position)
            putIfNotNull("totalProgression", totalProgression)
        }

        /**
         * Syntactic sugar to access the [otherLocations] values by subscripting [Locations] directly.
         * `locations["cssSelector"] == locations.otherLocations["cssSelector"]`
         */
        public operator fun get(key: String): Any? = otherLocations[key]

        public companion object {

            public fun fromJSON(
                json: JsonObject?,
            ): Locations {
                val map = json?.toMutableMap()

                val fragments = map?.optStringsFromArrayOrSingle("fragments", remove = true)?.takeIf { it.isNotEmpty() }
                    ?: map?.optStringsFromArrayOrSingle("fragment", remove = true)
                    ?: emptyList()

                val progression = map?.optNullableDouble("progression", remove = true)
                    ?.takeIf { it in 0.0..1.0 }

                val position = map?.optNullableInt("position", remove = true)
                    ?.takeIf { it > 0 }

                val totalProgression = map?.optNullableDouble("totalProgression", remove = true)
                    ?.takeIf { it in 0.0..1.0 }

                return Locations(
                    fragments = fragments,
                    progression = progression,
                    position = position,
                    totalProgression = totalProgression,
                    otherLocations = map?.let { JsonObject(it).toMap() } ?: emptyMap()
                )
            }
        }
    }

    /**
     * Textual context of the locator.
     *
     * A Locator Text Object contains multiple text fragments, useful to give a context to the
     * [Locator] or for highlights.
     * https://github.com/readium/architecture/tree/master/models/locators#the-text-object
     *
     * @param before The text before the locator.
     * @param highlight The text at the locator.
     * @param after The text after the locator.
     */
    @Parcelize
    public data class Text(
        val before: String? = null,
        val highlight: String? = null,
        val after: String? = null,
    ) : JSONable, Parcelable {

        override fun toJSON(): JsonObject = buildJsonObject {
            putIfNotNull("before", before)
            putIfNotNull("highlight", highlight)
            putIfNotNull("after", after)
        }

        public fun substring(range: IntRange): Text {
            if (highlight.isNullOrBlank()) return this

            val fixedRange = range.first.coerceIn(0, highlight.length)..range.last.coerceIn(
                0,
                highlight.length - 1
            )
            return copy(
                before = (before ?: "") + highlight.substring(0, fixedRange.first),
                highlight = highlight.substring(fixedRange),
                after = highlight.substring((fixedRange.last + 1).coerceAtMost(highlight.length)) + (after ?: "")
            )
        }

        public companion object {

            public fun fromJSON(json: JsonObject?): Text = Text(
                before = json?.optNullableString("before"),
                highlight = json?.optNullableString("highlight"),
                after = json?.optNullableString("after")
            )
        }
    }

    /**
     * Shortcut to get a copy of the [Locator] with different [Locations] sub-properties.
     */
    public fun copyWithLocations(
        fragments: List<String> = locations.fragments,
        progression: Double? = locations.progression,
        position: Int? = locations.position,
        totalProgression: Double? = locations.totalProgression,
        otherLocations: Map<String, Any> = locations.otherLocations,
    ): Locator = copy(
        locations = locations.copy(
            fragments = fragments,
            progression = progression,
            position = position,
            totalProgression = totalProgression,
            otherLocations = otherLocations
        )
    )

    override fun toJSON(): JsonObject = buildJsonObject {
        put("href", href.toString())
        put("type", mediaType.toString())
        putIfNotNull("title", title)
        putIfNotEmpty("locations", locations)
        putIfNotEmpty("text", text)
    }

    public companion object {

        /**
         * Creates a [Locator] from its JSON representation.
         */
        public fun fromJSON(
            json: JsonObject?,
            warnings: WarningLogger? = null,
        ): Locator? =
            fromJSON(json, warnings, withLegacyHref = false)

        /**
         * Creates a [Locator] from its legacy JSON representation.
         *
         * Only use this API when you are upgrading to Readium 3.x and migrating the [Locator]
         * objects stored in your database. See the migration guide for more information.
         */
        @DelicateReadiumApi
        public fun fromLegacyJSON(
            json: JsonObject?,
            warnings: WarningLogger? = null,
        ): Locator? =
            fromJSON(json, warnings, withLegacyHref = true)

        @OptIn(DelicateReadiumApi::class)
        private fun fromJSON(
            json: JsonObject?,
            warnings: WarningLogger? = null,
            withLegacyHref: Boolean = false,
        ): Locator? {
            val href = json?.optNullableString("href")
            val type = json?.optNullableString("type")
            if (href == null || type == null) {
                warnings?.log(Locator::class, "[href] and [type] are required", json)
                return null
            }

            val url = (
                if (withLegacyHref) {
                    Url.fromLegacyHref(href)
                } else {
                    Url(href)
                }
                ) ?: run {
                warnings?.log(Locator::class, "[href] is not a valid URL", json)
                return null
            }

            val mediaType = MediaType(type) ?: run {
                warnings?.log(Locator::class, "[type] is not a valid media type", json)
                return null
            }

            return Locator(
                href = url,
                mediaType = mediaType,
                title = json.optNullableString("title"),
                locations = Locations.fromJSON(json.optJsonObject("locations")),
                text = Text.fromJSON(json.optJsonObject("text"))
            )
        }

        public fun fromJSONArray(
            json: JsonArray?,
            warnings: WarningLogger? = null,
        ): List<Locator> {
            return json.parseObjects { fromJSON(it as? JsonObject, warnings) }
        }
    }
}

/**
 * Represents a sequential list of `Locator` objects.
 *
 * For example, a search result or a list of positions.
 */
@Parcelize
public data class LocatorCollection(
    val metadata: Metadata = Metadata(),
    val links: List<Link> = emptyList(),
    val locators: List<Locator> = emptyList(),
) : JSONable, Parcelable {

    /**
     * Holds the metadata of a `LocatorCollection`.
     *
     * @param numberOfItems Indicates the total number of locators in the collection.
     */
    @Parcelize
    public data class Metadata(
        val localizedTitle: LocalizedString? = null,
        val numberOfItems: Int? = null,
        val otherMetadata: @WriteWith<JsonMapParceler> Map<String, Any> = mapOf(),
    ) : JSONable, Parcelable {

        /**
         * Returns the default translation string for the [localizedTitle].
         */
        val title: String? get() = localizedTitle?.string

        override fun toJSON(): JsonObject = buildJsonObject {
            // Like with the legacy org.json serialization, the keys handled by this serializer
            // always shadow (or remove) a value with the same key in [otherMetadata].
            putAll(otherMetadata.filterKeys { it !in setOf("title", "numberOfItems") })
            putIfNotEmpty("title", localizedTitle)
            putIfNotNull("numberOfItems", numberOfItems)
        }

        public companion object {

            public fun fromJSON(json: JsonObject?, warnings: WarningLogger? = null): Metadata {
                json ?: return Metadata()

                @Suppress("NAME_SHADOWING")
                val json = json.toMutableMap()
                val localizedTitle = LocalizedString.fromJSON(json.remove("title"), warnings)
                val numberOfItems = json.optPositiveInt("numberOfItems", remove = true)

                return Metadata(
                    localizedTitle = localizedTitle,
                    numberOfItems = numberOfItems,
                    otherMetadata = JsonObject(json).toMap()
                )
            }
        }
    }

    override fun toJSON(): JsonObject = buildJsonObject {
        putIfNotEmpty("metadata", metadata.toJSON())
        putIfNotEmpty("links", links.toJSON())
        put("locators", locators.toJSON())
    }

    public companion object {

        public fun fromJSON(
            json: JsonObject?,
            warnings: WarningLogger? = null,
        ): LocatorCollection {
            return LocatorCollection(
                metadata = Metadata.fromJSON(json?.optJsonObject("metadata"), warnings),
                links = Link.fromJSONArray(
                    json?.optJsonArray("links"),
                    warnings = warnings
                ),
                locators = Locator.fromJSONArray(
                    json?.optJsonArray("locators"),
                    warnings
                )
            )
        }
    }
}
