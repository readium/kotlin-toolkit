/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Application
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.media2.MediaSessionNavigator
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.MediaService
import org.readium.r2.testapp.bookshelf.BookRepository
import java.io.File
import java.net.URL

/**
 * Open and store publications in order for them to be listened or read.
 *
 * Ensure you call [openBook] before any attempt to start a [ReaderActivity].
 * Pass the method result to the activity to enable it to know which current publication it must
 * retrieve from this repository - media or visual.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAudiobook::class)
class ReaderRepository(
    private val application: Application,
    private val streamer: Streamer,
    private val server: Server,
    private val mediaBinder: MediaService.Binder,
    private val bookRepository: BookRepository
) {
    var mediaReaderData: MediaReaderInitData? = null
        private set

    var visualReaderData: VisualReaderInitData? = null
        private set

    fun closeVisualPublication() {
        visualReaderData?.publication?.close()
        visualReaderData = null
    }

    fun closeMediaPublication() {
        mediaBinder.unbindNavigator()
        mediaReaderData?.mediaNavigator?.close()
        mediaReaderData?.mediaNavigator?.publication?.close()
        mediaReaderData = null
    }

    suspend fun openBook(context: Context, bookId: Long): Try<NavigatorType, Exception> =
        try {
            // NonCancellable because opened publications need to be closed.
            val type = withContext(NonCancellable) { openBookThrowing(context, bookId) }
            Try.success(type)
        } catch (e: Exception) {
            Try.failure(e)
        }

    private suspend fun openBookThrowing(context: Context, bookId: Long): NavigatorType {
        val book = bookRepository.get(bookId)
            ?: throw Exception("Cannot find book in database.")

        val file = File(book.href)
        require(file.exists())
        val asset = FileAsset(file)

        val publication = streamer.open(asset, allowUserInteraction = true, sender = context)
            .getOrThrow()

        val initialLocator = book.progression?.let { Locator.fromJSON(JSONObject(it)) }

        return if (publication.conformsTo(Publication.Profile.AUDIOBOOK)) {
            openAudiobookIfNeeded(bookId, publication, initialLocator)
            NavigatorType.Media
        } else {
            val url = prepareToServe(publication)
            closeVisualPublication()
            visualReaderData = VisualReaderInitData(bookId, publication, url, initialLocator)
            NavigatorType.Visual
        }
    }

    @OptIn(ExperimentalAudiobook::class)
    private suspend fun openAudiobookIfNeeded(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ) {
        if (mediaReaderData?.bookId == bookId) {
            return
        }

        val navigator = MediaSessionNavigator.create(
            application,
            publication,
            initialLocator
        ).getOrElse { throw Exception("Cannot open audiobook.") }

        closeMediaPublication()
        mediaReaderData = MediaReaderInitData(bookId, publication, navigator)
        mediaBinder.bindNavigator(navigator, bookId)
    }

    private fun prepareToServe(publication: Publication): URL {
        val userProperties =
            application.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json"
        val url =
            server.addPublication(publication, userPropertiesFile = File(userProperties))

        return url ?: throw Exception("Cannot add the publication to the HTTP server.")
    }
}