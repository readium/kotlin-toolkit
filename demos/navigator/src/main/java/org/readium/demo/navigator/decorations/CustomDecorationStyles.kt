/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.decorations

import android.graphics.Color
import androidx.annotation.ColorInt
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.readium.navigator.common.Decoration
import org.readium.navigator.web.common.WebDecorationTemplate
import org.readium.navigator.web.reflowable.ReflowableWebDecorationLocation
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.html.toCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.pageList

/**
 * Decoration Style for a page margin icon.
 *
 * This is an example of a custom Decoration Style declaration.
 */
data class DecorationStyleAnnotationMark(@param:ColorInt val tint: Int) : Decoration.Style

/**
 * Decoration Style for a page number label.
 *
 * This is an example of a custom Decoration Style declaration.
 *
 * @param label Page number label as declared in the `page-list` link object.
 */
data class DecorationStylePageNumber(val label: String) : Decoration.Style

/**
 * This decoration template will display a tinted "pen" icon in the page margin to show that a highlight
 * has an associated note.
 *
 * Note that the icon is served from the app assets folder.
 */
fun annotationMarkTemplate(@ColorInt defaultTint: Int = Color.YELLOW): WebDecorationTemplate {
    val className = "demo-annotation-mark"
    val iconUrl = checkNotNull(EpubNavigatorFragment.assetUrl("annotation-icon.svg"))
    return WebDecorationTemplate(
        layout = WebDecorationTemplate.Layout.BOUNDS,
        width = WebDecorationTemplate.Width.PAGE,
        element = {
            val style = it as? DecorationStyleAnnotationMark
            val tint = style?.tint ?: defaultTint
            // Using `data-activable=1` prevents the whole decoration container from being
            // clickable. Only the icon will respond to activation events.
            """
            <div><div data-activable="1" class="$className" style="background-color: ${tint.toCss()} !important"/></div>"
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background: url('$iconUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
    )
}

/**
 * This decoration template is used to display the page number labels in the margins, when a book
 * provides a `page-list`. The label is stored in the [DecorationStylePageNumber] itself.
 *
 * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
 */
fun pageNumberTemplate(): WebDecorationTemplate {
    val className = "demo-page-number"
    return WebDecorationTemplate(
        layout = WebDecorationTemplate.Layout.BOUNDS,
        width = WebDecorationTemplate.Width.PAGE,
        element = {
            val style = it as? DecorationStylePageNumber

            // Using `var(--RS__backgroundColor)` is a trick to use the same background color as
            // the Readium theme. If we don't set it directly inline in the HTML, it might be
            // forced transparent by Readium CSS.
            """
            <div><span class="$className" style="background-color: var(--RS__backgroundColor) !important">${style?.label}</span></div>"
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                padding: 0px 4px 0px 4px;
                border: 1px solid;
                border-radius: 20%;
                box-shadow: rgba(50, 50, 93, 0.25) 0px 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
                opacity: 0.8;
            }
            """
    )
}

/**
 * Decorations to display margin labels next to page numbers in an EPUB publication with a `page-list`
 * navigation document.
 *
 * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
 */
val Publication.pageNumberDecorations: PersistentList<Decoration<ReflowableWebDecorationLocation>> get() =
    pageList
        .mapIndexedNotNull { index, link ->
            val label = link.title ?: return@mapIndexedNotNull null

            val location = locatorFromLink(link)
                ?.let { ReflowableWebDecorationLocation(it) }
                ?: return@mapIndexedNotNull null

            Decoration<ReflowableWebDecorationLocation>(
                id = Decoration.Id("page-$index"),
                location = location,
                style = DecorationStylePageNumber(label = label)
            )
        }.toPersistentList()
