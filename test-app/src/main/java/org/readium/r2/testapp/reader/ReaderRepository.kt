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
import org.json.JSONObject
import org.readium.adapters.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.media3.audio.AudioNavigatorFactory
import org.readium.r2.navigator.media3.exoplayer.ExoPlayerEngineProvider
import org.readium.r2.navigator.media3.tts.TtsNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.testapp.Readium
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.reader.preferences.AndroidTtsPreferencesManagerFactory
import org.readium.r2.testapp.reader.preferences.EpubPreferencesManagerFactory
import org.readium.r2.testapp.reader.preferences.ExoPlayerPreferencesManagerFactory
import org.readium.r2.testapp.reader.preferences.PdfiumPreferencesManagerFactory
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
    sealed class OpeningError {

        class Unavailable(val exception: Exception) : OpeningError()

        class NotFound() : OpeningError()

        class OutOfMemory(val error: OutOfMemoryError) : OpeningError()

        class UnsupportedPublication() : OpeningError()

        class Forbidden(val exception: Exception) : OpeningError()

        class Unexpected(val exception: Exception) : OpeningError()
    }

    private val repository: MutableMap<Long, ReaderInitData> =
        mutableMapOf()

    private val mediaServiceFacade: MediaServiceFacade =
        MediaServiceFacade(application)

    operator fun get(bookId: Long): ReaderInitData? =
        repository[bookId]

    suspend fun open(bookId: Long, activity: Activity): Try<Unit, OpeningError> {
        if (bookId in repository.keys) {
            return Try.success(Unit)
        }

        val book = bookRepository.get(bookId)
            ?: run {
                val exception = Exception("Cannot find book in database.")
                return Try.failure(OpeningError.Unexpected(exception))
            }

        val asset = readium.assetRetriever.retrieve(
            Url(book.href)!!, book.mediaType, book.assetType
        ).getOrElse { return Try.failure(mapError(it)) }

        val publication = readium.publicationFactory.open(
            asset,
            drmScheme = book.drm,
            allowUserInteraction = true,
            sender = activity
        ).getOrElse { return Try.failure(mapError(it)) }

        // The publication is protected with a DRM and not unlocked.
        if (publication.isRestricted) {
            val exception = publication.protectionError
                ?: Exception("Couldn't unlock publication.")
            return Try.failure(OpeningError.Forbidden(exception))
        }

        val initialLocator = book.progression
            ?.let { Locator.fromJSON(JSONObject(it)) }

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
                Try.failure(OpeningError.UnsupportedPublication())
        }

        return readerInitData.map { repository[bookId] = it }
    }

    private fun mapError(error: AssetRetriever.Error): OpeningError =
        when (error) {
            AssetRetriever.Error.ArchiveFormatNotSupported ->
                OpeningError.UnsupportedPublication()
            AssetRetriever.Error.NoArchiveFactoryForResource ->
                OpeningError.UnsupportedPublication()
            is AssetRetriever.Error.SchemeNotSupported ->
                OpeningError.UnsupportedPublication()
            AssetRetriever.Error.NotFound ->
                OpeningError.NotFound()
            is AssetRetriever.Error.Forbidden ->
                OpeningError.Forbidden(error.exception)
            is AssetRetriever.Error.OutOfMemory ->
                OpeningError.OutOfMemory(error.error)
            is AssetRetriever.Error.Unavailable ->
                OpeningError.Unavailable(error.exception)
            is AssetRetriever.Error.Unknown ->
                OpeningError.Unexpected(error.exception)
        }

    private fun mapError(error: Publication.OpeningException): OpeningError =
        when (error) {
            is Publication.OpeningException.Forbidden ->
                OpeningError.Forbidden(error)
            Publication.OpeningException.IncorrectCredentials -> TODO()
            is Publication.OpeningException.NotFound ->
                OpeningError.NotFound()
            is Publication.OpeningException.OutOfMemory ->
                OpeningError.OutOfMemory(error.cause)
            is Publication.OpeningException.ParsingFailed ->
                OpeningError.Unexpected(error)
            is Publication.OpeningException.Unavailable ->
                OpeningError.Unavailable(error)
            is Publication.OpeningException.Unexpected ->
                OpeningError.Unexpected(error)
            is Publication.OpeningException.UnsupportedAsset ->
                OpeningError.UnsupportedPublication()
        }

    private suspend fun openAudio(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): Try<MediaReaderInitData, OpeningError> {

        val preferencesManager = ExoPlayerPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val initialPreferences = preferencesManager.preferences.value

        val navigatorFactory = AudioNavigatorFactory(
            publication,
            ExoPlayerEngineProvider(application),
        ) ?: return Try.failure(OpeningError.UnsupportedPublication())

        val navigator = navigatorFactory.createNavigator(
            initialPreferences,
            initialLocator
        ) ?: return Try.failure(OpeningError.UnsupportedPublication())

        mediaServiceFacade.openSession(bookId, navigator)
        val initData = MediaReaderInitData(
            bookId,
            publication,
            navigator,
            preferencesManager,
            navigatorFactory
        )
        return Try.success(initData)
    }

    private suspend fun openEpub(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): Try<EpubReaderInitData, OpeningError> {

        val preferencesManager = EpubPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = EpubNavigatorFactory(publication)
        val ttsInitData = getTtsInitData(bookId, publication)

        val initData = EpubReaderInitData(
            bookId, publication, initialLocator,
            preferencesManager, navigatorFactory, ttsInitData
        )
        return Try.success(initData)
    }

    private suspend fun openPdf(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): Try<PdfReaderInitData, OpeningError> {

        val preferencesManager = PdfiumPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val pdfEngine = PdfiumEngineProvider()
        val navigatorFactory = PdfNavigatorFactory(publication, pdfEngine)
        val ttsInitData = getTtsInitData(bookId, publication)

        val initData = PdfReaderInitData(
            bookId, publication, initialLocator,
            preferencesManager, navigatorFactory,
            ttsInitData
        )
        return Try.success(initData)
    }

    private suspend fun openImage(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): Try<ImageReaderInitData, OpeningError> {
        val initData = ImageReaderInitData(
            bookId = bookId,
            publication = publication,
            initialLocation = initialLocator,
            ttsInitData = getTtsInitData(bookId, publication)
        )
        return Try.success(initData)
    }

    private suspend fun getTtsInitData(
        bookId: Long,
        publication: Publication,
    ): TtsInitData? {
        val preferencesManager = AndroidTtsPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = TtsNavigatorFactory(application, publication) ?: return null
        return TtsInitData(mediaServiceFacade, navigatorFactory, preferencesManager)
    }

    suspend fun close(bookId: Long) {
        Timber.v("Closing Publication $bookId.")
        when (val initData = repository.remove(bookId)) {
            is MediaReaderInitData -> {
                mediaServiceFacade.closeSession()
                initData.publication.close()
            }
            is VisualReaderInitData -> {
                mediaServiceFacade.closeSession()
                initData.publication.close()
            }
            null, is DummyReaderInitData -> {
                // Do nothing
            }
        }
    }
}
