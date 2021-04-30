/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import androidx.lifecycle.LiveData
import org.joda.time.DateTime
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.testapp.db.BooksDao
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.domain.model.Bookmark
import org.readium.r2.testapp.domain.model.Highlight
import org.readium.r2.testapp.utils.extensions.authorName
import org.readium.r2.navigator.epub.Highlight as NavigatorHighlight

class BookRepository(private val booksDao: BooksDao) {

    fun getBooksFromDatabase(): LiveData<List<Book>> = booksDao.getAllBooks()

    suspend fun insertBook(href: String, extension: String, publication: Publication): Long {
        val book = Book(
            creation = DateTime().toDate().time,
            title = publication.metadata.title,
            author = publication.metadata.authorName,
            href = href,
            identifier = publication.metadata.identifier ?: "",
            ext = ".$extension",
            progression = "{}"
        )
        return booksDao.insertBook(book)
    }

    suspend fun deleteBook(id: Long) = booksDao.deleteBook(id)

    suspend fun saveProgression(locator: String, bookId: Long) =
        booksDao.saveProgression(locator, bookId)

    suspend fun insertBookmark(bookId: Long, publication: Publication, locator: Locator): Long {
        val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
        val bookmark = Bookmark(
            creation = DateTime().toDate().time,
            bookId = bookId,
            publicationId = publication.metadata.identifier ?: publication.metadata.title,
            resourceIndex = resource.toLong(),
            resourceHref = locator.href,
            resourceType = locator.type,
            resourceTitle = locator.title.orEmpty(),
            location = Locator.Locations(
                progression = locator.locations.progression,
                position = locator.locations.position
            ).toJSON().toString(),
            locatorText = Locator.Text().toJSON().toString()
        )

        return booksDao.insertBookmark(bookmark)
    }

    fun getBookmarks(bookId: Long): LiveData<MutableList<Bookmark>> =
        booksDao.getBookmarksForBook(bookId)

    suspend fun deleteBookmark(bookmarkId: Long) = booksDao.deleteBookmark(bookmarkId)

    fun getHighlights(bookId: Long, href: String): LiveData<List<Highlight>> =
        booksDao.getHighlightsForBook(bookId, href)

    fun getHighlights(bookId: Long): LiveData<List<Highlight>> =
        booksDao.getHighlightsForBook(bookId)

    suspend fun insertHighlight(
        bookId: Long,
        publication: Publication,
        navigatorHighlight: NavigatorHighlight,
        progression: Double,
        annotation: String? = null
    ): Long {
        val resource =
            publication.readingOrder.indexOfFirstWithHref(navigatorHighlight.locator.href)!!

        // This is required to be able to go right to a highlight from the Outline fragment,
        // as Navigator.go doesn't support DOM ranges yet.
        val locations = navigatorHighlight.locator.locations.copy(progression = progression)

        val highlight = Highlight(
            creation = DateTime().toDate().time,
            bookId = bookId,
            highlightId = navigatorHighlight.id,
            publicationId = publication.metadata.identifier ?: publication.metadata.title,
            style = "style",
            color = navigatorHighlight.color,
            annotation = annotation ?: "",
            annotationMarkStyle = navigatorHighlight.annotationMarkStyle ?: "",
            resourceIndex = resource.toLong(),
            resourceHref = navigatorHighlight.locator.href,
            resourceType = navigatorHighlight.locator.type,
            resourceTitle = navigatorHighlight.locator.title.orEmpty(),
            location = locations.toJSON().toString(),
            locatorText = navigatorHighlight.locator.text.toJSON().toString()
        )

        return booksDao.insertHighlight(highlight)
    }

    // This will be used when highlights are redone
//    suspend fun deleteHighlight(id: Long) = booksDao.deleteHighlight(id)

    suspend fun updateHighlight(
        id: String,
        color: Int? = null,
        annotation: String? = null,
        markStyle: String? = null
    ) {
        val highlight = getHighlightByHighlightId(id)

        if (highlight != null) {
            val updatedColor = color ?: highlight.color
            val updatedAnnotation = annotation ?: highlight.annotation
            val updatedMarkStyle = markStyle ?: highlight.annotationMarkStyle

            booksDao.updateHighlight(id, updatedColor, updatedAnnotation, updatedMarkStyle)
        }
    }

    suspend fun getHighlightByHighlightId(highlightId: String): Highlight? {
        return booksDao.getHighlightByHighlightId(highlightId).firstOrNull()
    }

    suspend fun deleteHighlightByHighlightId(highlightId: String) =
        booksDao.deleteHighlightByHighlightId(highlightId)
}