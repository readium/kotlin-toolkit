/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.html

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.optPositiveInt
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * This construct enables a serializable representation of a DOM Range.
 *
 * In a DOM Range object, the startContainer + startOffset tuple represents the [start] boundary
 * point. Similarly, the the endContainer + endOffset tuple represents the [end] boundary point.
 * In both cases, the start/endContainer property is a pointer to either a DOM text node, or a DOM
 * element (this typically depends on the mechanism from which the DOM Range instance originates,
 * for example when obtaining the currently-selected document fragment using the `window.selection`
 * API). In the case of a DOM text node, the start/endOffset corresponds to a position within the
 * character data. In the case of a DOM element node, the start/endOffset corresponds to a position
 * that designates a child text node.
 *
 * Note that [end] field is optional. When only the start field is specified, the domRange object
 * represents a "collapsed" range that has identical [start] and [end] boundary points.
 *
 * https://github.com/readium/architecture/blob/master/models/locators/extensions/html.md#the-domrange-object
 *
 * @param start A serializable representation of the "start" boundary point of the DOM Range.
 * @param end A serializable representation of the "end" boundary point of the DOM Range.
 */
@Parcelize
public data class DomRange(
    val start: Point,
    val end: Point? = null,
) : JSONable, Parcelable {

    /**
     * A serializable representation of a boundary point in a DOM Range.
     *
     * The [cssSelector] field always references a DOM element. If the original DOM Range
     * start/endContainer property references a DOM text node, the [textNodeIndex] field is used to
     * complement the CSS Selector; thereby providing a pointer to a child DOM text node; and
     * [charOffset] is used to tell a position within the character data of that DOM text node
     * (just as the DOM Range start/endOffset does). If the original DOM Range start/endContainer
     * property references a DOM Element, then the [textNodeIndex] field is used to designate the
     * child Text node (just as the DOM Range start/endOffset does), and the optional [charOffset]
     * field is not used (as there is no explicit position within the character data of the text
     * node).
     *
     * https://github.com/readium/architecture/blob/master/models/locators/extensions/html.md#the-start-and-end-object
     */
    @Parcelize
    public data class Point(
        val cssSelector: String,
        val textNodeIndex: Int,
        val charOffset: Int? = null,
    ) : JSONable, Parcelable {

        override fun toJSON(): JSONObject = JSONObject().apply {
            put("cssSelector", cssSelector)
            put("textNodeIndex", textNodeIndex)
            put("charOffset", charOffset)
        }

        public companion object {

            public fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): Point? {
                val cssSelector = json?.optNullableString("cssSelector")
                val textNodeIndex = json?.optPositiveInt("textNodeIndex")
                if (cssSelector == null || textNodeIndex == null) {
                    warnings?.log(
                        Point::class.java,
                        "[cssSelector] and [textNodeIndex] are required",
                        json
                    )
                    return null
                }

                return Point(
                    cssSelector = cssSelector,
                    textNodeIndex = textNodeIndex,
                    charOffset = json.optPositiveInt("charOffset")
                        // The model was using `offset` before, so we still parse it to ensure
                        // backward-compatibility for reading apps having persisted legacy Locator
                        // models.
                        ?: json.optPositiveInt("offset")
                )
            }
        }
    }

    override fun toJSON(): JSONObject = JSONObject().apply {
        putIfNotEmpty("start", start)
        putIfNotEmpty("end", end)
    }

    public companion object {

        public fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): DomRange? {
            val start = Point.fromJSON(json?.optJSONObject("start"))
            if (start == null) {
                warnings?.log(DomRange::class.java, "[start] is required", json)
                return null
            }

            return DomRange(
                start = start,
                end = Point.fromJSON(json?.optJSONObject("end"))
            )
        }
    }
}
