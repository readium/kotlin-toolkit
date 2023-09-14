/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationFactory
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.DownloadRepository
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.utils.tryOrLog
import timber.log.Timber

/**
 * The [Bookshelf] supports two different processes:
 * - directly _adding_ the url to a remote asset or an asset from shared storage to the database
 * - _importing_ an asset, that is downloading or copying the publication the asset points to to the app storage
 *   before adding it to the database
 */
class Bookshelf(
    context: Context,
    private val bookRepository: BookRepository,
    downloadRepository: DownloadRepository,
    storageDir: File,
    private val coverStorage: CoverStorage,
    private val publicationFactory: PublicationFactory,
    private val assetRetriever: AssetRetriever,
    private val protectionRetriever: ContentProtectionSchemeRetriever,
    formatRegistry: FormatRegistry,
    lcpService: Try<LcpService, LcpException>,
    downloadManager: DownloadManager
) {
    val channel: Channel<Event> =
        Channel(Channel.UNLIMITED)

    sealed class Event {
        data object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val error: ImportError
        ) : Event()
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val publicationRetriever: PublicationRetriever =
        PublicationRetriever(
            context,
            storageDir,
            assetRetriever,
            formatRegistry,
            downloadRepository,
            downloadManager,
            lcpService.map { it.publicationRetriever() },
            PublicationRetrieverListener()
        )

    private inner class PublicationRetrieverListener : PublicationRetriever.Listener {
        override fun onSuccess(publication: File, coverUrl: String?) {
            coroutineScope.launch {
                val url = publication.toUrl()
                addBookFeedback(url, coverUrl)
            }
        }

        override fun onError(error: ImportError) {
            coroutineScope.launch {
                channel.send(Event.ImportPublicationError(error))
            }
        }
    }

    fun importPublicationFromStorage(
        uri: Uri
    ) {
        publicationRetriever.retrieveFromStorage(uri)
    }

    fun importPublicationFromOpds(
        publication: Publication
    ) {
        publicationRetriever.retrieveFromOpds(publication)
    }

    fun addPublicationFromWeb(
        url: Url
    ) {
        coroutineScope.launch {
            addBookFeedback(url)
        }
    }

    fun addPublicationFromStorage(
        url: Url
    ) {
        coroutineScope.launch {
            addBookFeedback(url)
        }
    }

    private suspend fun addBookFeedback(
        url: Url,
        coverUrl: String? = null
    ) {
        addBook(url, coverUrl)
            .onSuccess { channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    private suspend fun addBook(
        url: Url,
        coverUrl: String? = null
    ): Try<Unit, ImportError> {
        val asset =
            assetRetriever.retrieve(url)
                ?: return Try.failure(
                    ImportError.PublicationError(PublicationError.UnsupportedAsset())
                )

        val drmScheme =
            protectionRetriever.retrieve(asset)

        publicationFactory.open(
            asset,
            contentProtectionScheme = drmScheme,
            allowUserInteraction = false
        ).onSuccess { publication ->
            val coverFile =
                coverStorage.storeCover(publication, coverUrl)
                    .getOrElse {
                        return Try.failure(ImportError.StorageError(it))
                    }

            val id = bookRepository.insertBook(
                url.toString(),
                asset.mediaType,
                asset.assetType,
                drmScheme,
                publication,
                coverFile.path
            )
            if (id == -1L) {
                coverFile.delete()
                return Try.failure(ImportError.DatabaseError())
            }
        }
            .onFailure {
                Timber.d("Cannot open publication: $it.")
                return Try.failure(
                    ImportError.PublicationError(PublicationError(it))
                )
            }

        return Try.success(Unit)
    }

    suspend fun deleteBook(book: Book) {
        val id = book.id!!
        bookRepository.deleteBook(id)
        val url = Url(book.href)!!
        if (url.scheme == "file") {
            tryOrLog { File(url.path).delete() }
        }
        tryOrLog { File(book.cover).delete() }
    }
}
