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
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.asset.AssetType
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUrl
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
    private val streamer: Streamer,
    private val assetRetriever: AssetRetriever,
) {
    private val coverDir: File =
        File(storageDir, "covers/")
            .apply { if (!exists()) mkdirs() }

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
        assetType: AssetType,
        publication: Publication,
        cover: String
    ): Long {
        val book = Book(
            creation = DateTime().toDate().time,
            title = publication.metadata.title,
            author = publication.metadata.authorName,
            href = href,
            identifier = publication.metadata.identifier ?: "",
            mediaType = mediaType,
            assetType = assetType,
            progression = "{}",
            cover = cover
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

    suspend fun addContentBook(
        contentUri: Uri
    ): Try<Unit, ImportException> =
        contentUri.copyToTempFile(context, storageDir)
            .mapFailure { ImportException.IOException }
            .flatMap { addLocalBook(it) }

    suspend fun addRemoteBook(
        url: Url
    ): Try<Unit, ImportException> {
        val asset = assetRetriever.ofUrl(url, fileExtension = url.extension)
            ?: return Try.failure(
                ImportException.UnableToOpenPublication(Publication.OpeningException.UnsupportedFormat())
            )
        return addBook(url, asset)
    }

    suspend fun addLocalBook(
        tempFile: File,
        coverUrl: String? = null
    ): Try<Unit, ImportException> {
        val sourceAssetDescription = assetRetriever.ofFile(tempFile)
            ?: return Try.failure(
                ImportException.UnableToOpenPublication(Publication.OpeningException.UnsupportedFormat())
            )

        val (publicationFile, assetDescription) =
            if (sourceAssetDescription.mediaType != MediaType.LCP_LICENSE_DOCUMENT) {
                tempFile to sourceAssetDescription
            } else {
                lcpService
                    .flatMap {
                        it.acquirePublication(tempFile)
                    }
                    .fold(
                        {
                            val file = it.localFile
                            val assetDescription = assetRetriever.ofFile(file, fileExtension = File(it.suggestedFilename).extension)
                            file to assetDescription
                        },
                        {
                            tryOrNull { tempFile.delete() }
                            return Try.failure(ImportException.LcpAcquisitionFailed(it))
                        }
                    )
            }

        if (assetDescription == null) {
            val exception = Publication.OpeningException.UnsupportedFormat(
                Exception("Unsupported media type")
            )
            return Try.failure(ImportException.UnableToOpenPublication(exception))
        }

        val fileName = "${UUID.randomUUID()}.${sourceAssetDescription.mediaType.fileExtension}"
        val libraryFile = File(storageDir, fileName)
        val libraryUrl = libraryFile.toUrl()

        try {
            publicationFile.moveTo(libraryFile)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { libraryFile.delete() }
            return Try.failure(ImportException.IOException)
        }

        return addBook(
            libraryUrl, assetDescription, coverUrl
        ).onFailure {
            tryOrNull { libraryFile.delete() }
        }
    }

    private suspend fun addBook(
        url: Url,
        asset: Asset,
        coverUrl: String? = null,
    ): Try<Unit, ImportException> {
        streamer.open(asset, allowUserInteraction = false)
            .onSuccess { publication ->
                val coverBitmap: Bitmap? = coverUrl
                    ?.let { getBitmapFromURL(it) }
                    ?: publication.cover()
                val coverFile =
                    tryOrNull { storeCover(coverBitmap) }
                        ?: return Try.failure(ImportException.IOException)

                val id = insertBookIntoDatabase(
                    url.toString(),
                    asset.mediaType,
                    asset.assetType,
                    publication,
                    coverFile.path
                )
                if (id == -1L) {
                    coverFile.delete()
                    return Try.failure(ImportException.ImportDatabaseFailed)
                }
            }
            .onFailure {
                Timber.d(it)
                return Try.failure(ImportException.UnableToOpenPublication(it))
            }

        return Try.success(Unit)
    }

    private suspend fun storeCover(cover: Bitmap?): File =
        withContext(Dispatchers.IO) {
            val coverImageFile = File(coverDir, "${UUID.randomUUID()}.png")
            val resized = cover?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
            coverImageFile
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
        val id = book.id!!
        val url = URL(book.href)
        if (url.protocol == "file") {
            tryOrLog { File(url.path).delete() }
        }
        File(book.cover).delete()
        deleteBookFromDatabase(id)
    }
}
