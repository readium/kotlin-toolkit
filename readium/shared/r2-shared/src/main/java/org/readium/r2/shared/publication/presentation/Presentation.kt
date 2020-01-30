/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.shared.publication.presentation

import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.Properties
import java.io.Serializable

/**
 * The Presentation Hints extension defines a number of hints for User Agents about the way content
 * should be presented to the user.
 *
 * https://readium.org/webpub-manifest/extensions/presentation.html
 * https://readium.org/webpub-manifest/schema/extensions/presentation/metadata.schema.json
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
data class Presentation(
    val clipped: Boolean = false,
    val continuous: Boolean = true,
    val fit: Fit = Fit.CONTAIN,
    val orientation: Orientation = Orientation.AUTO,
    val overflow: Overflow = Overflow.AUTO,
    val spread: Spread = Spread.AUTO,
    val layout: EpubLayout? = null
) : JSONable, Serializable {

    /**
     * Serializes a [Presentation] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("clipped", clipped)
        put("continuous", continuous)
        put("fit", fit.value)
        put("orientation", orientation.value)
        put("overflow", overflow.value)
        put("spread", spread.value)
        put("layout", layout?.value)
    }

    companion object {

        /**
         * Creates a [Properties] from its RWPM JSON representation.
         */
        fun fromJSON(json: JSONObject?): Presentation {
            if (json == null) {
                return Presentation()
            }
            return Presentation(
                clipped = json.optBoolean("clipped", false),
                continuous = json.optBoolean("continuous", true),
                fit = Fit.from(json.optString("fit")),
                orientation = Orientation.from(json.optString("orientation")),
                overflow = Overflow.from(json.optString("overflow")),
                spread = Spread.from(json.optString("spread")),
                layout = EpubLayout.from(json.optString("layout"))
            )
        }

    }

    /**
     * Suggested method for constraining a resource inside the viewport.
     */
    enum class Fit(val value: String) {
        WIDTH("width"),
        HEIGHT("height"),
        CONTAIN("contain"),
        COVER("cover");

        companion object {
            fun from(value: String?): Fit =
                Fit.values().firstOrNull { it.value == value } ?: CONTAIN
        }
    }

    /**
     * Suggested orientation for the device when displaying the linked resource.
     */
    enum class Orientation(val value: String) {
        AUTO("auto"),
        LANDSCAPE("landscape"),
        PORTRAIT("portrait");

        companion object {
            fun from(value: String?): Orientation =
                Orientation.values().firstOrNull { it.value == value } ?: AUTO
        }
    }

    /**
     * Suggested method for handling overflow while displaying the linked resource.
     */
    enum class Overflow(val value: String) {
        AUTO("auto"),
        PAGINATED("paginated"),
        SCROLLED("scrolled"),
        SCROLLED_CONTINUOUS("scrolled-continuous");

        companion object {
            fun from(value: String?): Overflow =
                Overflow.values().firstOrNull { it.value == value } ?: AUTO
        }
    }

    /**
     * Indicates how the linked resource should be displayed in a reading environment that displays
     * synthetic spreads.
     */
    enum class Page(val value: String) {
        LEFT("left"),
        RIGHT("right"),
        CENTER("center");

        companion object {
            fun from(value: String?): Page? =
                Page.values().firstOrNull { it.value == value }
        }
    }

    /**
     * Indicates the condition to be met for the linked resource to be rendered within a synthetic
     * spread.
     */
    enum class Spread(val value: String) {
        AUTO("auto"),
        BOTH("both"),
        NONE("none"),
        LANDSCAPE("landscape");

        companion object {
            fun from(value: String?): Spread =
                Spread.values().firstOrNull { it.value == value } ?: AUTO
        }
    }

    @Deprecated("Use [toJSON] instead", ReplaceWith("toJSON()"))
    fun getJSON(): JSONObject = toJSON()

    @Deprecated("Use [overflow] instead", ReplaceWith("overflow"))
    var flow: Overflow? = overflow

}
