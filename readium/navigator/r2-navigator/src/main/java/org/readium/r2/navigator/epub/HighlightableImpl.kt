/*
 * Module: r2-navigator-kotlin
 * Developers: Taehyun Kim, Seongjin Kim
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.epub

import android.graphics.Rect
import org.readium.r2.shared.Locator

interface HighlightableImpl {
    fun showHighlight(highlight: Highlight)

    fun showHighlights(highlights: Array<Highlight>)

    fun hideHighlightWithID(id: String)

    fun hideAllHighlights()

    fun rectangleForHighlightWithID(id: String, callback: (Rect?) -> Unit)

    fun rectangleForHighlightAnnotationMarkWithID(id: String): Rect?

    fun registerHighlightAnnotationMarkStyle(name: String, css: String)

    fun highlightActivated(id: String)

    fun highlightAnnotationMarkActivated(id: String)
}

data class Highlight(
        val id: String,
        val locator: Locator,
        val color: Int,
        val style: Style,
        val annotationMarkStyle: String? = null
)

enum class Style {
    highlight, underline, strikethrough
}