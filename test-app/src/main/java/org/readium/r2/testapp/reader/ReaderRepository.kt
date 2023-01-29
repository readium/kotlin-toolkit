/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Activity
import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences as JetpackPreferences
import java.io.File
import org.json.JSONObject
import org.readium.adapters.pdfium.navigator.PdfiumEngineProvider
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.media3.tts.TtsNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.testapp.Readium
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.reader.preferences.AndroidTtsPreferencesManagerFactory
import org.readium.r2.testapp.reader.preferences.EpubPreferencesManagerFactory
import org.readium.r2.testapp.reader.preferences.PdfiumPreferencesManagerFactory
import org.readium.r2.testapp.reader.tts.TtsServiceFacade
import timber.log.Timber

/**
 * Open and store publications in order for them to be listened or read.
 *
 * Ensure you call [open] before any attempt to start a [ReaderActivity].
 * Pass the method result to the activity to enable it to know which current publication it must
 * retrieve from this repository - media or visual.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderRepository(
    private val application: Application,
    private val readium: Readium,
    private val bookRepository: BookRepository,
    private val preferencesDataStore: DataStore<JetpackPreferences>,
) {
    object CancellationException : Exception()

    private val repository: MutableMap<Long, ReaderInitData> =
        mutableMapOf()

    private val ttsServiceFacade: TtsServiceFacade =
        TtsServiceFacade(application)

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
            publication.conformsTo(Publication.Profile.EPUB) ->
                openEpub(bookId, publication, initialLocator)
            publication.conformsTo(Publication.Profile.PDF) ->
                openPdf(bookId, publication, initialLocator)
            publication.conformsTo(Publication.Profile.DIVINA) ->
                openImage(bookId, publication, initialLocator)
            else ->
                throw Exception("Publication is not supported.")
        }

        repository[bookId] = readerInitData
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

        MediaService.start(application)
        val mediaBinder = MediaService.bind(application)
        mediaBinder.bindNavigator(navigator, bookId)
        return MediaReaderInitData(bookId, publication, navigator, mediaBinder)
    }

    /* private suspend fun openAudio(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): MediaReaderInitData {

        val preferencesManager = ExoPlayerPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val mediaEngine = ExoPlayerEngineProvider(application)
        val initialPreferences = preferencesManager.preferences.value
        val actualInitialLocator = initialLocator
            ?: publication.locatorFromLink(publication.readingOrder[0])!!

        val navigatorFactory = PlayerNavigatorFactory(
            publication,
            mediaEngine,
            DefaultMetadataProvider(),
            initialPreferences,
            actualInitialLocator,
        )

        val navigator = navigatorFactory.getMediaNavigator()
            .getOrElse { throw Exception("Cannot open audiobook.") }

        val navigator = MediaNavigator.create(
            application,
            publication,
            initialLocator
        ).getOrElse { throw Exception("Cannot open audiobook.") }

        mediaBinder.bindNavigator(navigator, bookId)
        return MediaReaderInitData(bookId, publication,, preferencesManager, navigatorFactory)
    } */

    private suspend fun openEpub(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): EpubReaderInitData {

        val preferencesManager = EpubPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = EpubNavigatorFactory(publication)
        val ttsInitData = getTtsInitData(bookId, publication)

        return EpubReaderInitData(
            bookId, publication, initialLocator,
            preferencesManager, navigatorFactory, ttsInitData
        )
    }

    private suspend fun openPdf(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): PdfReaderInitData {

        val preferencesManager = PdfiumPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val pdfEngine = PdfiumEngineProvider()
        val navigatorFactory = PdfNavigatorFactory(publication, pdfEngine)
        val ttsInitData = getTtsInitData(bookId, publication)

        return PdfReaderInitData(
            bookId, publication, initialLocator,
            preferencesManager, navigatorFactory,
            ttsInitData
        )
    }

    private suspend fun openImage(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): ImageReaderInitData {
        return ImageReaderInitData(
            bookId = bookId,
            publication = publication,
            initialLocation = initialLocator,
            ttsInitData = getTtsInitData(bookId, publication)
        )
    }

    private suspend fun getTtsInitData(
        bookId: Long,
        publication: Publication,
    ): TtsInitData? {
        val preferencesManager = AndroidTtsPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = TtsNavigatorFactory(application, publication) ?: return null
        return TtsInitData(ttsServiceFacade, navigatorFactory, preferencesManager)
    }

    suspend fun close(bookId: Long) {
        Timber.d("Closing Publication")
        when (val initData = repository.remove(bookId)) {
            is MediaReaderInitData -> {
                initData.sessionBinder.closeNavigator()
                MediaService.stop(application)
                initData.publication.close()
            }
            is VisualReaderInitData -> {
                initData.ttsInitData?.ttsServiceFacade?.closeSession()
                initData.publication.close()
            }
            null, is DummyReaderInitData -> {
                // Do nothing
            }
        }
    }
}
