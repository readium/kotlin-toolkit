/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Provides a precise location in a publication in a format that can be stored and shared.
 *
 * There are many different use cases for locators:
 *  - getting back to the last position in a publication
 *  - bookmarks
 *  - highlights & annotations
 *  - search results
 *  - human-readable (and shareable) reference in a publication
 *
 * https://github.com/readium/architecture/tree/master/locators
 */
@Parcelize
data class Locator(
    val href: String,
    val type: String,
    val title: String? = null,
    val locations: Locations = Locations(),
    val text: Text = Text()
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
    data class Locations(
        val fragments: List<String> = emptyList(),
        val progression: Double? = null,
        val position: Int? = null,
        val totalProgression: Double? = null,
        val otherLocations: @WriteWith<JSONParceler> Map<String, Any> = emptyMap()
    ) : JSONable, Parcelable {

        override fun toJSON() = JSONObject(otherLocations).apply {
            putIfNotEmpty("fragments", fragments)
            put("progression", progression)
            put("position", position)
            put("totalProgression", totalProgression)
        }

        /**
         * Syntactic sugar to access the [otherLocations] values by subscripting [Locations] directly.
         * `locations["cssSelector"] == locations.otherLocations["cssSelector"]`
         */
        operator fun get(key: String): Any? = otherLocations[key]

        companion object {

            fun fromJSON(json: JSONObject?): Locations {
                val fragments = json?.optStringsFromArrayOrSingle("fragments", remove = true)?.takeIf { it.isNotEmpty() }
                    ?: json?.optStringsFromArrayOrSingle("fragment", remove = true)
                    ?: emptyList()

                val progression = json?.optNullableDouble("progression", remove = true)
                    ?.takeIf { it in 0.0..1.0 }

                val position = json?.optNullableInt("position", remove = true)
                    ?.takeIf { it > 0 }

                val totalProgression = json?.optNullableDouble("totalProgression", remove = true)
                    ?.takeIf { it in 0.0..1.0 }

                return Locations(
                    fragments = fragments,
                    progression = progression,
                    position = position,
                    totalProgression = totalProgression,
                    otherLocations = json?.toMap() ?: emptyMap()
                )
            }

        }

        @Deprecated("Renamed to [fragments]", ReplaceWith("fragments"))
        val fragment: String? get() = fragments.firstOrNull()

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
    data class Text(
        val before: String? = null,
        val highlight: String? = null,
        val after: String? = null
    ) : JSONable, Parcelable {

        override fun toJSON() = JSONObject().apply {
            put("before", before)
            put("highlight", highlight)
            put("after", after)
        }

        companion object {

            fun fromJSON(json: JSONObject?) = Text(
                before = json?.optNullableString("before"),
                highlight = json?.optNullableString("highlight"),
                after = json?.optNullableString("after")
            )

        }

    }

    /**
     * Shortcut to get a copy of the [Locator] with different [Locations] sub-properties.
     */
    fun copyWithLocations(
        fragments: List<String> = locations.fragments,
        progression: Double? = locations.progression,
        position: Int? = locations.position,
        totalProgression: Double? = locations.totalProgression,
        otherLocations: Map<String, Any> = locations.otherLocations
    ) = copy(
        locations = locations.copy(
            fragments = fragments,
            progression = progression,
            position = position,
            totalProgression = totalProgression,
            otherLocations = otherLocations
        )
    )

    override fun toJSON() = JSONObject().apply {
        put("href", href)
        put("type", type)
        put("title", title)
        putIfNotEmpty("locations", locations)
        putIfNotEmpty("text", text)
    }

    companion object {

        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): Locator? {
            val href = json?.optNullableString("href")
            val type = json?.optNullableString("type")
            if (href == null || type == null) {
                warnings?.log(Locator::class.java, "[href] and [type] are required", json)
                return null
            }

            return Locator(
                href = href,
                type = type,
                title = json.optNullableString("title"),
                locations = Locations.fromJSON(json.optJSONObject("locations")),
                text = Text.fromJSON(json.optJSONObject("text"))
            )
        }

    }

}

/**
 * Creates a [Locator] from a reading order [Link].
 */
fun Link.toLocator(): Locator {
    val components = href.split("#", limit = 2)
    return Locator(
        href = components.firstOrNull() ?: href,
        type = type ?: "",
        title = title,
        locations = Locator.Locations(
            fragments = listOfNotNull(components.getOrNull(1))
        )
    )
}
