/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.presentation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableBoolean
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.MapCompanion

/**
 * The Presentation Hints extension defines a number of hints for User Agents about the way content
 * should be presented to the user.
 *
 * https://readium.org/webpub-manifest/extensions/presentation.html
 * https://readium.org/webpub-manifest/schema/extensions/presentation/metadata.schema.json
 *
 * These properties are nullable to avoid having default values when it doesn't make sense for a
 * given publication. If a navigator needs a default value when not specified,
 * Presentation.DEFAULT_X and Presentation.X.DEFAULT can be used.
 *
 * @param clipped Specifies whether or not the parts of a linked resource that flow out of the
 *     viewport are clipped.
 * @param continuous Indicates how the progression between resources from the reading order should
 *     be handled.
 * @param fit Suggested method for constraining a resource inside the viewport.
 * @param orientation Suggested orientation for the device when displaying the linked resource.
 * @param overflow Suggested method for handling overflow while displaying the linked resource.
 * @param spread Indicates the condition to be met for the linked resource to be rendered within a
 *     synthetic spread.
 * @param layout Hints how the layout of the resource should be presented (EPUB extension).
 */
@Parcelize
public data class Presentation(
    val clipped: Boolean? = null,
    val continuous: Boolean? = null,
    val fit: Fit? = null,
    val orientation: Orientation? = null,
    val overflow: Overflow? = null,
    val spread: Spread? = null,
    val layout: EpubLayout? = null,
) : JSONable, Parcelable {

    /**
     * Serializes a [Presentation] to its RWPM JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("clipped", clipped)
        put("continuous", continuous)
        put("fit", fit?.value)
        put("orientation", orientation?.value)
        put("overflow", overflow?.value)
        put("spread", spread?.value)
        put("layout", layout?.value)
    }

    public companion object {

        /**
         * Default value for [clipped], if not specified.
         */
        public const val DEFAULT_CLIPPED: Boolean = false

        /**
         * Default value for [continuous], if not specified.
         */
        public const val DEFAULT_CONTINUOUS: Boolean = true

        /**
         * Creates a [Properties] from its RWPM JSON representation.
         */
        public fun fromJSON(json: JSONObject?): Presentation {
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
    @Serializable
    public enum class Fit(public val value: String) : Parcelable {
        @SerialName("width")
        WIDTH("width"),

        @SerialName("height")
        HEIGHT("height"),

        @SerialName("contain")
        CONTAIN("contain"),

        @SerialName("cover")
        COVER("cover"),
        ;

        public companion object : MapCompanion<String, Fit>(entries.toTypedArray(), Fit::value) {

            /**
             * Default value for [Fit], if not specified.
             */
            public val DEFAULT: Fit = CONTAIN
        }
    }

    /**
     * Suggested orientation for the device when displaying the linked resource.
     */
    @Parcelize
    @Serializable
    public enum class Orientation(public val value: String) : Parcelable {
        @SerialName("auto")
        AUTO("auto"),

        @SerialName("landscape")
        LANDSCAPE("landscape"),

        @SerialName("portrait")
        PORTRAIT("portrait"),
        ;

        public companion object : MapCompanion<String, Orientation>(
            entries.toTypedArray(),
            Orientation::value
        ) {

            /**
             * Default value for [Orientation], if not specified.
             */
            public val DEFAULT: Orientation = AUTO
        }
    }

    /**
     * Suggested method for handling overflow while displaying the linked resource.
     */
    @Parcelize
    @Serializable
    public enum class Overflow(public val value: String) : Parcelable {
        @SerialName("auto")
        AUTO("auto"),

        @SerialName("paginated")
        PAGINATED("paginated"),

        @SerialName("scrolled")
        SCROLLED("scrolled"),
        ;

        public companion object : MapCompanion<String, Overflow>(
            entries.toTypedArray(),
            Overflow::value
        ) {

            /**
             * Default value for [Overflow], if not specified.
             */
            public val DEFAULT: Overflow = AUTO
        }
    }

    /**
     * Indicates how the linked resource should be displayed in a reading environment that displays
     * synthetic spreads.
     */
    @Parcelize
    @Serializable
    public enum class Page(public val value: String) : Parcelable {
        @SerialName("left")
        LEFT("left"),

        @SerialName("right")
        RIGHT("right"),

        @SerialName("center")
        CENTER("center"),
        ;

        public companion object : MapCompanion<String, Page>(entries.toTypedArray(), Page::value)
    }

    /**
     * Indicates the condition to be met for the linked resource to be rendered within a synthetic
     * spread.
     */
    @Parcelize
    @Serializable
    public enum class Spread(public val value: String) : Parcelable {
        @SerialName("auto")
        AUTO("auto"),

        @SerialName("both")
        BOTH("both"),

        @SerialName("none")
        NONE("none"),

        @SerialName("landscape")
        LANDSCAPE("landscape"),
        ;

        public companion object : MapCompanion<String, Spread>(
            entries.toTypedArray(),
            Spread::value
        ) {

            /**
             * Default value for [Spread], if not specified.
             */
            public val DEFAULT: Spread = AUTO
        }
    }
}
