package org.readium.r2.testapp.reader

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.readium.r2.lcp.LcpService
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.media2.MediaSessionNavigator
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.MediaService
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import java.io.File
import java.net.URL

class PublicationRepository(
    private val application: Application,
    private val streamer: Streamer,
    private val server: Server,
    private val mediaServiceBinder: Deferred<MediaService.Binder>,
    private val bookRepository: BookRepository
) {
    data class ReaderArguments(
        val bookId: Long,
        val publication: Publication,
        val baseUrl: URL? = null,
        val initialLocation: Locator? = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @ExperimentalAudiobook
    val mediaNavigator: MediaSessionNavigator?
        get() = mediaServiceBinder.getCompleted().mediaNavigator

    suspend fun openBook(context: Context, bookId: Long): Try<ReaderArguments, Exception> =
        try {
            val arguments = openBookThrowing(context, bookId)
            Try.success(arguments)
        } catch (e: Exception) {
            Try.failure(e)
        }

    private suspend fun openBookThrowing(
        context: Context,
        bookId: Long
    ): ReaderArguments {
        val book = bookRepository.get(bookId)
            ?: throw Exception("Cannot find book in database.")

        val file = File(book.href)
        require(file.exists())
        val asset = FileAsset(file)

        val publication = streamer.open(asset, allowUserInteraction = true, sender = context)
            .getOrThrow()

        val initialLocator = book.progression?.let { Locator.fromJSON(JSONObject(it)) }

        return if (publication.conformsTo(Publication.Profile.AUDIOBOOK)) {
            openAudiobookIfNeeded(bookId, publication, initialLocator).getOrThrow()
            ReaderArguments(bookId, publication)
        } else {
            val url = prepareToServe(publication).getOrThrow()
            ReaderArguments(bookId, publication, url, initialLocator)
        }
    }

    @OptIn(ExperimentalAudiobook::class)
    private suspend fun openAudiobookIfNeeded(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): Try<Unit, Exception> {
        val binder = mediaServiceBinder.await()
        return if (binder.mediaSession?.id != bookId.toString()) {
            binder.openPublication(bookId, publication, initialLocator)
                .mapFailure { Exception("Cannot open audiobook.") }
        } else
            Try.success(Unit)
    }

    private fun prepareToServe(publication: Publication): Try<URL, Exception> {
        val userProperties =
            application.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json"
        val url =
            server.addPublication(publication, userPropertiesFile = File(userProperties))
        return if (url == null) {
            Try.failure(Exception("Cannot add the publication to the HTTP server."))
        } else {
            Try.success(url)
        }
    }

    companion object {

        fun create(
            application: Application,
            server: Server,
            mediaService: Deferred<MediaService.Binder>
        ): PublicationRepository {

            val lcpService = LcpService(application)
                ?.let { Try.success(it) }
                ?: Try.failure(Exception("liblcp is missing on the classpath"))

            val streamer = Streamer(
                application,
                contentProtections = listOfNotNull(
                    lcpService.getOrNull()?.contentProtection()
                )
            )

            val booksDao = BookDatabase.getDatabase(application).booksDao()
            val bookRepository = BookRepository(booksDao)

            return PublicationRepository(
                application,
                streamer,
                server,
                mediaService,
                bookRepository
            )
        }
    }
}