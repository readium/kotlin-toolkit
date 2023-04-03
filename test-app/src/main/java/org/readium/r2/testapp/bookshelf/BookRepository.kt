/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.Streamer
import org.readium.r2.testapp.db.BooksDao
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.domain.model.Bookmark
import org.readium.r2.testapp.domain.model.Highlight
import org.readium.r2.testapp.utils.extensions.authorName
import org.readium.r2.testapp.utils.extensions.copyToTempFile
import org.readium.r2.testapp.utils.extensions.moveTo
import timber.log.Timber

class BookRepository(
    private val context: Context,
    private val booksDao: BooksDao,
    private val storageDir: File,
    private val lcpService: Try<LcpService, Exception>,
    private val streamer: Streamer
) {

    fun books(): LiveData<List<Book>> = booksDao.getAllBooks()

    suspend fun get(id: Long) = booksDao.get(id)

    suspend fun saveProgression(locator: Locator, bookId: Long) =
        booksDao.saveProgression(locator.toJSON().toString(), bookId)

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
            location = locator.locations.toJSON().toString(),
            locatorText = Locator.Text().toJSON().toString()
        )

        return booksDao.insertBookmark(bookmark)
    }

    fun bookmarksForBook(bookId: Long): LiveData<List<Bookmark>> =
        booksDao.getBookmarksForBook(bookId)

    suspend fun deleteBookmark(bookmarkId: Long) = booksDao.deleteBookmark(bookmarkId)

    suspend fun highlightById(id: Long): Highlight? =
        booksDao.getHighlightById(id)

    fun highlightsForBook(bookId: Long): Flow<List<Highlight>> =
        booksDao.getHighlightsForBook(bookId)

    suspend fun addHighlight(
        bookId: Long,
        style: Highlight.Style,
        @ColorInt tint: Int,
        locator: Locator,
        annotation: String
    ): Long =
        booksDao.insertHighlight(Highlight(bookId, style, tint, locator, annotation))

    suspend fun deleteHighlight(id: Long) = booksDao.deleteHighlight(id)

    suspend fun updateHighlightAnnotation(id: Long, annotation: String) {
        booksDao.updateHighlightAnnotation(id, annotation)
    }

    suspend fun updateHighlightStyle(id: Long, style: Highlight.Style, @ColorInt tint: Int) {
        booksDao.updateHighlightStyle(id, style, tint)
    }

    private suspend fun insertBookIntoDatabase(
        href: String,
        mediaType: MediaType,
        publication: Publication
    ): Long {
        val book = Book(
            creation = DateTime().toDate().time,
            title = publication.metadata.title,
            author = publication.metadata.authorName,
            href = href,
            identifier = publication.metadata.identifier ?: "",
            type = mediaType.toString(),
            progression = "{}"
        )
        return booksDao.insertBook(book)
    }

    private suspend fun deleteBookFromDatabase(id: Long) =
        booksDao.deleteBook(id)

    sealed class ImportException(
        message: String? = null,
        cause: Throwable? = null
    ) : Exception(message, cause) {

        class LcpAcquisitionFailed(
            cause: Throwable
        ) : ImportException(cause = cause)

        object IOException : ImportException()

        object ImportDatabaseFailed : ImportException()

        class UnableToOpenPublication(
            val exception: Publication.OpeningException
        ) : ImportException(cause = exception)

        class UnsupportedProtocol(
            private val protocol: String
        ) : ImportException()
    }

    suspend fun addBook(
        contentUri: Uri
    ): Try<Unit, ImportException> =
        contentUri.copyToTempFile(context, storageDir)
            .mapFailure { ImportException.IOException }
            .map { addBook(it) }

    suspend fun addBook(
        url: URL
    ): Try<Unit, ImportException> {
        if (!url.protocol.startsWith("http")) {
            return Try.failure(ImportException.UnsupportedProtocol(url.protocol))
        }

        val bytes = { url.readBytes() }
        val mediaType = MediaType.ofBytes(bytes, fileExtension = url.extension)
                ?: MediaType.BINARY

        streamer.open(url, mediaType, allowUserInteraction = false)
            .onSuccess { publication ->
                val id = insertBookIntoDatabase(
                    url.toString(),
                    mediaType,
                    publication
                )
                if (id == -1L)
                    return Try.failure(ImportException.ImportDatabaseFailed)

                Try.success(Unit)
            }
            .onFailure {
                Timber.e(it)
                return Try.failure(ImportException.UnableToOpenPublication(it))
            }

        return Try.success(Unit)
    }

    suspend fun addBook(
        tempFile: File,
        coverUrl: String? = null
    ): Try<Unit, ImportException> {
        val sourceMediaType = MediaType.ofFile(tempFile)

        val (publicationFile, mediaType) =
            if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT) {
                tempFile to sourceMediaType
            } else {
                lcpService
                    .flatMap {
                       it.acquirePublication(tempFile)
                    }
                    .fold(
                        {
                            val file = it.localFile
                            val mediaType = MediaType.of(fileExtension = File(it.suggestedFilename).extension)
                            file to mediaType
                        },
                        {
                            tryOrNull { tempFile.delete() }
                            return Try.failure(ImportException.LcpAcquisitionFailed(it))
                        }
                    )
            }

        if (mediaType == null) {
            val exception = Publication.OpeningException.UnsupportedFormat(
                Exception("Unsupported media type")
            )
            return Try.failure(ImportException.UnableToOpenPublication(exception))
        }

        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryFile = File(storageDir, fileName)
        val libraryUrl = libraryFile.toURI().toURL()!!

        try {
            publicationFile.moveTo(libraryFile)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { libraryFile.delete() }
            return Try.failure(ImportException.IOException)
        }

        streamer.open(libraryUrl, mediaType, allowUserInteraction = false)
            .onSuccess { publication ->
                val id = insertBookIntoDatabase(
                    libraryUrl.toString(),
                    mediaType,
                    publication
                )
                if (id == -1L)
                    return Try.failure(ImportException.ImportDatabaseFailed)

                val cover: Bitmap? = coverUrl
                    ?.let { getBitmapFromURL(it) }
                    ?: publication.cover()
                storeCoverImage(cover, id.toString())
                Try.success(Unit)
            }
            .onFailure {
                tryOrNull { libraryFile.delete() }
                Timber.d(it)
                return Try.failure(ImportException.UnableToOpenPublication(it))
            }

        return Try.success(Unit)
    }

    private suspend fun storeCoverImage(cover: Bitmap?, imageName: String) =
        withContext(Dispatchers.IO) {
            // TODO Figure out where to store these cover images
            val coverImageDir = File(storageDir, "covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }
            val coverImageFile = File(storageDir, "covers/$imageName.png")

            val resized = cover?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
        }

    private suspend fun getBitmapFromURL(src: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(src)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

    suspend fun deleteBook(book: Book) {
        book.id?.let { deleteBookFromDatabase(it) }
        tryOrNull { File(book.href).delete() }
        tryOrNull { File(storageDir, "covers/${book.id}.png").delete() }
    }
}
