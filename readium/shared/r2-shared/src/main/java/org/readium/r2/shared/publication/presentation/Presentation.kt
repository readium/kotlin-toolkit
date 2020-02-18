/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.shared.publication.presentation

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableBoolean
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.util.MapCompanion

/**
 * The Presentation Hints extension defines a number of hints for User Agents about the way content
 * should be presented to the user.
 *
 * https://readium.org/webpub-manifest/extensions/presentation.html
 * https://readium.org/webpub-manifest/schema/extensions/presentation/metadata.schema.json
 *
 * These properties are nullable to avoid having default values when it doesn't make sense for a
 * given [Publication]. If a navigator needs a default value when not specified,
 * Presentation.DEFAULT_X and Presentation.X.DEFAULT can be used.
 *
 * @param clipped Specifies whether or not the parts of a linked resource that flow out of the
 *     viewport are clipped.
 * @param continuous Indicates how the progression between resources from the [readingOrder] should
 *     be handled.
 * @param fit Suggested method for constraining a resource inside the viewport.
 * @param orientation Suggested orientation for the device when displaying the linked resource.
 * @param overflow Suggested method for handling overflow while displaying the linked resource.
 * @param spread Indicates the condition to be met for the linked resource to be rendered within a
 *     synthetic spread.
 * @param layout Hints how the layout of the resource should be presented (EPUB extension).
 */
@Parcelize
data class Presentation(
    val clipped: Boolean? = null,
    val continuous: Boolean? = null,
    val fit: Fit? = null,
    val orientation: Orientation? = null,
    val overflow: Overflow? = null,
    val spread: Spread? = null,
    val layout: EpubLayout? = null
) : JSONable, Parcelable {

    /**
     * Serializes a [Presentation] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("clipped", clipped)
        put("continuous", continuous)
        put("fit", fit?.value)
        put("orientation", orientation?.value)
        put("overflow", overflow?.value)
        put("spread", spread?.value)
        put("layout", layout?.value)
    }

    companion object {

        /**
         * Default value for [clipped], if not specified.
         */
        const val DEFAULT_CLIPPED = false

        /**
         * Default value for [continuous], if not specified.
         */
        const val DEFAULT_CONTINUOUS = true

        /**
         * Creates a [Properties] from its RWPM JSON representation.
         */
        fun fromJSON(json: JSONObject?): Presentation {
            if (json == null) {
                return Presentation()
            }
            return Presentation(
                clipped = json.optNullableBoolean("clipped"),
                continuous = json.optNullableBoolean("continuous"),
                fit = Fit(json.optString("fit")),
                orientation = Orientation(json.optString("orientation")),
                overflow = Overflow(json.optString("overflow")),
                spread = Spread(json.optString("spread")),
                layout = EpubLayout(json.optString("layout"))
            )
        }

    }

    /**
     * Suggested method for constraining a resource inside the viewport.
     */
    @Parcelize
    enum class Fit(val value: String) : Parcelable {
        WIDTH("width"),
        HEIGHT("height"),
        CONTAIN("contain"),
        COVER("cover");

        companion object : MapCompanion<String, Fit>(values(), Fit::value) {

            /**
             * Default value for [Fit], if not specified.
             */
            val DEFAULT = CONTAIN
        }
    }

    /**
     * Suggested orientation for the device when displaying the linked resource.
     */
    @Parcelize
    enum class Orientation(val value: String) : Parcelable {
        AUTO("auto"),
        LANDSCAPE("landscape"),
        PORTRAIT("portrait");

        companion object : MapCompanion<String, Orientation>(values(), Orientation::value) {

            /**
             * Default value for [Orientation], if not specified.
             */
            val DEFAULT = AUTO

            @Deprecated("Renamed to [AUTO]", ReplaceWith("Orientation.AUTO"))
            val Auto: Orientation = AUTO
            @Deprecated("Renamed to [LANDSCAPE]", ReplaceWith("Orientation.LANDSCAPE"))
            val Landscape: Orientation = LANDSCAPE
            @Deprecated("Renamed to [PORTRAIT]", ReplaceWith("Orientation.PORTRAIT"))
            val Portrait: Orientation = PORTRAIT
        }
    }

    /**
     * Suggested method for handling overflow while displaying the linked resource.
     */
    @Parcelize
    enum class Overflow(val value: String) : Parcelable {
        AUTO("auto"),
        PAGINATED("paginated"),
        SCROLLED("scrolled");

        companion object : MapCompanion<String, Overflow>(values(), Overflow::value) {

            /**
             * Default value for [Overflow], if not specified.
             */
            val DEFAULT = AUTO

            @Deprecated("Renamed to [PAGINATED]", ReplaceWith("Overflow.PAGINATED"))
            val Paginated: Overflow = PAGINATED
            @Deprecated("Use [presentation.continuous] instead", ReplaceWith("presentation.continuous"))
            val Continuous: Overflow = SCROLLED
            @Deprecated("Renamed to [SCROLLED]", ReplaceWith("Overflow.SCROLLED"))
            val Document: Overflow = SCROLLED
        }
    }

    /**
     * Indicates how the linked resource should be displayed in a reading environment that displays
     * synthetic spreads.
     */
    @Parcelize
    enum class Page(val value: String) : Parcelable {
        LEFT("left"),
        RIGHT("right"),
        CENTER("center");

        companion object : MapCompanion<String, Page>(values(), Page::value)
    }

    /**
     * Indicates the condition to be met for the linked resource to be rendered within a synthetic
     * spread.
     */
    @Parcelize
    enum class Spread(val value: String) : Parcelable {
        AUTO("auto"),
        BOTH("both"),
        NONE("none"),
        LANDSCAPE("landscape");

        companion object : MapCompanion<String, Spread>(values(), Spread::value) {

            /**
             * Default value for [Spread], if not specified.
             */
            val DEFAULT = AUTO

            @Deprecated("Renamed to [AUTO]", ReplaceWith("Spread.AUTO"))
            val Auto: Spread = AUTO
            @Deprecated("Renamed to [LANDSCAPE]", ReplaceWith("Spread.LANDSCAPE"))
            val Landscape: Spread = LANDSCAPE
            @Deprecated("Renamed to [BOTH]", ReplaceWith("Spread.BOTH"))
            val Portrait: Spread = BOTH
            @Deprecated("Renamed to [BOTH]", ReplaceWith("Spread.BOTH"))
            val Both: Spread = BOTH
            @Deprecated("Renamed to [NONE]", ReplaceWith("Spread.NONE"))
            val None: Spread = NONE
        }
    }

    @Deprecated("Use [toJSON] instead", ReplaceWith("toJSON()"))
    fun getJSON(): JSONObject = toJSON()

    @Deprecated("Use [overflow] instead", ReplaceWith("overflow"))
    val flow: Overflow? get() = overflow

}
