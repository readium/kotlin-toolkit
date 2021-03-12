/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import org.readium.r2.navigator.epub.Style
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.testapp.db.Bookmark
import org.readium.r2.testapp.db.BookmarksDatabase
import org.readium.r2.testapp.db.BooksDatabase
import org.readium.r2.testapp.db.Highlight
import org.readium.r2.testapp.db.HighlightsDatabase

class BookData(context: Context, private val bookId: Long, private val publication: Publication) {

    private val pubId: String = publication.metadata.identifier ?: publication.metadata.title
    private val booksDb = BooksDatabase(context)
    private val bookmarksDb = BookmarksDatabase(context)
    private val highlightsDb = HighlightsDatabase(context)

    var savedLocation: Locator?
        get() = booksDb.books.currentLocator(bookId)
        set(locator) { booksDb.books.saveProgression(locator, bookId) }

    fun addBookmark(locator: Locator): Boolean {
        val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
        val bookmark = Bookmark(bookId, pubId, resource.toLong(), locator)
        return bookmarksDb.bookmarks.insert(bookmark) != null
    }

    fun removeBookmark(id: Long) {
        bookmarksDb.bookmarks.delete(id)
    }

    fun getBookmarks(comparator: Comparator<Bookmark>): List<Bookmark> {
        return bookmarksDb.bookmarks.list(bookId).sortedWith(comparator)
    }

    fun addHighlight(navigatorHighlight: org.readium.r2.navigator.epub.Highlight, progression: Double, annotation: String? = null) {
        val resource = publication.readingOrder.indexOfFirstWithHref(navigatorHighlight.locator.href)!!

        // This is required to be able to go right to a highlight from the Outline fragment,
        // as Navigator.go doesn't support DOM ranges yet.
        val locations = navigatorHighlight.locator.locations.copy(progression = progression)

        val highlight = Highlight(
            navigatorHighlight.id,
            pubId,
            "style",
            navigatorHighlight.color,
            annotation.orEmpty(),
            navigatorHighlight.annotationMarkStyle.orEmpty(),
            resource.toLong(),
            navigatorHighlight.locator.href,
            navigatorHighlight.locator.type,
            navigatorHighlight.locator.title.orEmpty(),
            locations,
            navigatorHighlight.locator.text,
            bookID = bookId
        )
        highlightsDb.highlights.insert(highlight)
    }

    @Suppress("NAME_SHADOWING")
    fun updateHighlight(id: String, color: Int? = null, annotation: String? = null, markStyle: String? = null) {
        val highlight = getHighlight(id)
        val progression = highlight.location.progression!!
        val color = color ?: highlight.color
        val annotation = annotation ?: highlight.annotation
        val markStyle = markStyle ?: highlight.annotationMarkStyle

        addHighlight(
            highlight.toNavigatorHighlight().copy(color = color, annotationMarkStyle = markStyle),
            progression = progression,
            annotation = annotation
        )
    }

    fun removeHighlight(id: String) {
        highlightsDb.highlights.delete(id)
    }

    fun getHighlight(id: String): Highlight {
        return checkNotNull(highlightsDb.highlights.list(id).firstOrNull())
    }

    fun getHighlights(href: String? = null, comparator: Comparator<Highlight>? = null): List<Highlight> {
        val highlights =
            if (href == null)
                highlightsDb.highlights.listAll(bookId)
            else
                highlightsDb.highlights.listAll(bookId, href)

        if (comparator != null) {
            highlights.sortWith(comparator)
        }
        return highlights
    }
}
