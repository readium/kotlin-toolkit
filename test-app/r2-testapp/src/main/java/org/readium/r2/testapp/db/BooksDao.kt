/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.domain.model.Bookmark
import org.readium.r2.testapp.domain.model.Highlight


@Dao
interface BooksDao {

    /**
     * Inserts a book
     * @param book The book to insert
     * @return ID of the book that was added (primary key)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    /**
     * Deletes a book
     * @param bookId The ID of the book
     */
    @Query("DELETE FROM " + Book.TABLE_NAME + " WHERE " + Book.ID + " = :bookId")
    suspend fun deleteBook(bookId: Long)

    /**
     * Retrieve all books
     * @return List of books as LiveData
     */
    @Query("SELECT * FROM " + Book.TABLE_NAME + " ORDER BY " + Book.CREATION_DATE + " desc")
    fun getAllBooks(): LiveData<List<Book>>

    /**
     * Retrieve all bookmarks for a specific book
     * @param bookId The ID of the book
     * @return List of bookmarks for the book as LiveData
     */
    @Query("SELECT * FROM " + Bookmark.TABLE_NAME + " WHERE " + Bookmark.BOOK_ID + " = :bookId")
    fun getBookmarksForBook(bookId: Long): LiveData<MutableList<Bookmark>>

    /**
     * Retrieve all highlights for a specific book
     * @param bookId The ID of the book
     * @param href
     * @return List of highlights for the book as LiveData
     */
    @Query("SELECT * FROM " + Highlight.TABLE_NAME + " WHERE " + Highlight.BOOK_ID + " = :bookId AND " + Highlight.RESOURCE_HREF + " = :href")
    fun getHighlightsForBook(bookId: Long, href: String): LiveData<List<Highlight>>

    /**
     * Retrieve all highlights for a specific book
     * @param bookId The ID of the book
     * @return List of highlights for the book as LiveData
     */
    @Query("SELECT * FROM " + Highlight.TABLE_NAME + " WHERE " + Highlight.BOOK_ID + " = :bookId")
    fun getHighlightsForBook(bookId: Long): LiveData<List<Highlight>>

    /**
     * Retrieve all highlights for a specific book
     * @param highlightId The ID of the highlight
     * @return List of highlights that match highlightId
     */
    @Query("SELECT * FROM " + Highlight.TABLE_NAME + " WHERE " + Highlight.HIGHLIGHT_ID + " = :highlightId")
    suspend fun getHighlightByHighlightId(highlightId: String): MutableList<Highlight>

    /**
     * Inserts a bookmark
     * @param bookmark The bookmark to insert
     * @return The ID of the bookmark that was added (primary key)
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    /**
     * Inserts a highlight
     * @param highlight The highlight to insert
     * @return The ID of the highlight that was added (primary key)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long

    /**
     * Updates a highlight
     * @param highlightId The navigator highlight ID
     * @param color Updated color
     * @param annotation Updated annotation
     * @param annotationMarkStyle Updated mark style
     * @return The ID of the highlight that was added (primary key)
     */
    @Query("UPDATE " + Highlight.TABLE_NAME + " SET " + Highlight.COLOR + " = :color, " + Highlight.ANNOTATION + " = :annotation, " + Highlight.ANNOTATION_MARK_STYLE + " =:annotationMarkStyle WHERE " + Highlight.HIGHLIGHT_ID + "= :highlightId")
    suspend fun updateHighlight(
        highlightId: String,
        color: Int,
        annotation: String,
        annotationMarkStyle: String
    )

    /**
     * Deletes a bookmark
     * @param id The id of the bookmark to delete
     */
    @Query("DELETE FROM " + Bookmark.TABLE_NAME + " WHERE " + Bookmark.ID + " = :id")
    suspend fun deleteBookmark(id: Long)

    /**
     * Deletes a highlight
     * @param id The ID of the highlight to delete
     */
    @Query("DELETE FROM " + Highlight.TABLE_NAME + " WHERE " + Highlight.ID + " = :id")
    suspend fun deleteHighlight(id: Long)

    /**
     * Deletes a highlight by the highlightId
     * @param highlightId The highlightId of the highlight to delete
     */
    @Query("DELETE FROM " + Highlight.TABLE_NAME + " WHERE " + Highlight.HIGHLIGHT_ID + " = :highlightId")
    suspend fun deleteHighlightByHighlightId(highlightId: String)

    /**
     * Saves book progression
     * @param locator Location of the book
     * @param id The book to update
     */
    @Query("UPDATE " + Book.TABLE_NAME + " SET " + Book.PROGRESSION + " = :locator WHERE " + Book.ID + "= :id")
    suspend fun saveProgression(locator: String, id: Long)
}