/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Activity
import android.app.Application
import org.json.JSONObject
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.testapp.MediaService
import org.readium.r2.testapp.Readium
import org.readium.r2.testapp.bookshelf.BookRepository
import java.io.File

/**
 * Open and store publications in order for them to be listened or read.
 *
 * Ensure you call [open] before any attempt to start a [ReaderActivity].
 * Pass the method result to the activity to enable it to know which current publication it must
 * retrieve from this repository - media or visual.
 */
@OptIn(ExperimentalMedia2::class)
class ReaderRepository(
    private val application: Application,
    private val readium: Readium,
    private val mediaBinder: MediaService.Binder,
    private val bookRepository: BookRepository
) {
    object CancellationException : Exception()

    private val repository: MutableMap<Long, ReaderInitData> =
        mutableMapOf()

    operator fun get(bookId: Long): ReaderInitData? =
        repository[bookId]

    suspend fun open(bookId: Long, activity: Activity): Try<Unit, Exception> {
        return try {
            openThrowing(bookId, activity)
            Try.success(Unit)
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    private suspend fun openThrowing(bookId: Long, activity: Activity) {
        if (bookId in repository.keys) {
            return
        }

        val book = bookRepository.get(bookId)
            ?: throw Exception("Cannot find book in database.")

        val file = File(book.href)
        require(file.exists())
        val asset = FileAsset(file)

        val publication = readium.streamer.open(asset, allowUserInteraction = true, sender = activity)
            .getOrThrow()

        // The publication is protected with a DRM and not unlocked.
        if (publication.isRestricted) {
            throw publication.protectionError
                ?: CancellationException
        }

        val initialLocator = book.progression?.let { Locator.fromJSON(JSONObject(it)) }

        val readerInitData = when {
            publication.conformsTo(Publication.Profile.AUDIOBOOK) ->
                openAudio(bookId, publication, initialLocator)
            else ->
                openVisual(bookId, publication, initialLocator)
        }

        repository[bookId] = readerInitData
    }

    private fun openVisual(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): VisualReaderInitData {
        return VisualReaderInitData(bookId, publication, initialLocator)
    }

    @OptIn(ExperimentalMedia2::class)
    private suspend fun openAudio(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): MediaReaderInitData {
        val navigator = MediaNavigator.create(
            application,
            publication,
            initialLocator
        ).getOrElse { throw Exception("Cannot open audiobook.") }

        mediaBinder.bindNavigator(navigator, bookId)
        return MediaReaderInitData(bookId, publication, navigator)
    }

    fun close(bookId: Long) {
        when (val initData = repository.remove(bookId)) {
            is MediaReaderInitData -> {
                mediaBinder.closeNavigator()
            }
            is VisualReaderInitData -> {
                initData.publication.close()
            }
            null, is DummyReaderInitData -> {
                // Do nothing
            }
        }
    }
}