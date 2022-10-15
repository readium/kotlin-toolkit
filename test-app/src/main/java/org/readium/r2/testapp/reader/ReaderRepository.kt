/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Activity
import android.app.Application
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferences
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferencesFilter
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.adapters.pspdfkit.navigator.PsPdfKitNavigatorFactory
import org.readium.adapters.pspdfkit.navigator.PsPdfKitPreferencesSerializer
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesFilter
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.testapp.MediaService
import org.readium.r2.testapp.utils.extensions.stateInFirst
import org.readium.r2.testapp.Readium
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.reader.preferences.PreferencesStore
import org.readium.r2.testapp.utils.extensions.combine
import java.io.File

/**
 * Open and store publications in order for them to be listened or read.
 *
 * Ensure you call [open] before any attempt to start a [ReaderActivity].
 * Pass the method result to the activity to enable it to know which current publication it must
 * retrieve from this rep+ository - media or visual.
 */
@OptIn(ExperimentalMedia2::class, ExperimentalReadiumApi::class)
class ReaderRepository(
    private val application: Application,
    private val readium: Readium,
    private val mediaBinder: MediaService.Binder,
    private val bookRepository: BookRepository,
    private val preferencesStore: PreferencesStore,
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

        mediaBinder.bindNavigator(navigator, bookId)
        return MediaReaderInitData(bookId, publication, navigator)
    }

    private suspend fun openEpub(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): EpubReaderInitData {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val serializer = EpubPreferencesSerializer()
        val preferences = getPreferences(
            EpubPreferences::class, bookId, serializer,
            coroutineScope, { EpubPreferences() }, EpubPreferences::plus
        )

        return EpubReaderInitData(
            bookId, publication, initialLocator,
            coroutineScope, preferences,
            EpubPreferencesFilter(),
            serializer,
            EpubNavigatorFactory(publication, readium.epubNavigatorConfig)
        )
    }

    private suspend fun openPdf(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): PdfReaderInitData {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val serializer = PsPdfKitPreferencesSerializer()
        val preferences = getPreferences(
            PsPdfKitPreferences::class, bookId, serializer,
            coroutineScope, { PsPdfKitPreferences() }, PsPdfKitPreferences::plus
        )
        val navigatorFactory = PdfNavigatorFactory(publication, readium.pdfEngineProvider)

        return PdfReaderInitData(
            bookId, publication, initialLocator,
            coroutineScope, preferences,
            PsPdfKitPreferencesFilter(),
            serializer,
            navigatorFactory
        )
    }

    private suspend fun <P: Configurable.Preferences> getPreferences(
        klass: KClass<P>,
        bookId: Long,
        serializer: PreferencesSerializer<P>,
        publicationScope: CoroutineScope,
        default: () -> P,
        plus: (P).(P) -> P
    ): StateFlow<P> {
        val pubPrefs = preferencesStore[klass, bookId]
            .map { json -> json?.let { serializer.deserialize(it) } ?: default() }
            .stateInFirst(publicationScope, SharingStarted.Eagerly)

        val sharedPrefs = preferencesStore[EpubPreferences::class]
            .map { json -> json?.let { serializer.deserialize(it) } ?: default() }
            .stateInFirst(publicationScope, SharingStarted.Eagerly)

        return combine(publicationScope, SharingStarted.Eagerly, sharedPrefs, pubPrefs, plus)
    }

    private fun openImage(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): ImageReaderInitData {
        return ImageReaderInitData(
            bookId = bookId,
            publication = publication,
            initialLocation = initialLocator
        )
    }

    fun close(bookId: Long) {
        when (val initData = repository.remove(bookId)) {
            is MediaReaderInitData -> {
                mediaBinder.closeNavigator()
            }
            is EpubReaderInitData -> {
                initData.publication.close()
                initData.coroutineScope.cancel()
            }
            is PdfReaderInitData -> {
                initData.publication.close()
                initData.coroutineScope.cancel()
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
