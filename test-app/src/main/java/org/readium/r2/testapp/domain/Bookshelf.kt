/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.downloads.DownloadManager
import org.readium.downloads.DownloadManagerProvider
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpPublicationRetriever
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.UserException
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.asset.AssetType
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationFactory
import org.readium.r2.testapp.PublicationError
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.db.DownloadDatabase
import org.readium.r2.testapp.utils.extensions.copyToTempFile
import org.readium.r2.testapp.utils.extensions.moveTo
import org.readium.r2.testapp.utils.tryOrNull
import timber.log.Timber

class Bookshelf(
    private val context: Context,
    private val bookRepository: BookRepository,
    private val storageDir: File,
    private val lcpService: Try<LcpService, UserException>,
    private val publicationFactory: PublicationFactory,
    private val assetRetriever: AssetRetriever,
    private val protectionRetriever: ContentProtectionSchemeRetriever,
    private val formatRegistry: FormatRegistry,
    private val downloadManagerProvider: DownloadManagerProvider
) {
    sealed class ImportError(
        content: Content,
        cause: Exception?
    ) : UserException(content, cause) {

        constructor(@StringRes userMessageId: Int) :
            this(Content(userMessageId), null)

        constructor(cause: UserException) :
            this(Content(cause), cause)

        class LcpAcquisitionFailed(
            override val cause: UserException
        ) : ImportError(cause)

        class PublicationError(
            override val cause: UserException
        ) : ImportError(cause) {

            companion object {

                operator fun invoke(
                    error: AssetRetriever.Error
                ): ImportError = PublicationError(org.readium.r2.testapp.PublicationError(error))

                operator fun invoke(
                    error: Publication.OpeningException
                ): ImportError = PublicationError(org.readium.r2.testapp.PublicationError(error))
            }
        }

        class ImportBookFailed(
            override val cause: Throwable
        ) : ImportError(R.string.import_publication_unexpected_io_exception)

        class DownloadFailed(
            val error: DownloadManager.Error
        ) : ImportError(R.string.import_publication_download_failed)

        class OpdsError(
            override val cause: Throwable
        ) : ImportError(R.string.import_publication_no_acquisition)

        class ImportDatabaseFailed :
            ImportError(R.string.import_publication_unable_add_pub_database)
    }

    sealed class Event {
        object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val error: ImportError
        ) : Event()
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val coverDir: File =
        File(storageDir, "covers/")
            .apply { if (!exists()) mkdirs() }

    private val opdsDownloader =
        DownloadDatabase.getDatabase(context).downloadsDao()
            .let { dao -> OpdsDownloader(dao, downloadManagerProvider, OpdsDownloaderListener()) }

    private inner class OpdsDownloaderListener : OpdsDownloader.Listener {
        override fun onDownloadCompleted(publication: String, cover: String?) {
            coroutineScope.launch {
                addLocalBook(File(publication), cover)
            }
        }

        override fun onDownloadFailed(error: DownloadManager.Error) {
            coroutineScope.launch {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.DownloadFailed(error)
                    )
                )
            }
        }
    }

    private val lcpPublicationRetriever = lcpService.map {
        it.publicationRetriever(
            downloadManagerProvider,
            LcpRetrieverListener()
        )
    }

    private inner class LcpRetrieverListener : LcpPublicationRetriever.Listener {
        override fun onAcquisitionCompleted(
            requestId: LcpPublicationRetriever.RequestId,
            acquiredPublication: LcpService.AcquiredPublication
        ) {
            coroutineScope.launch {
                addLocalBook(acquiredPublication.localFile)
            }
        }

        override fun onAcquisitionProgressed(
            requestId: LcpPublicationRetriever.RequestId,
            downloaded: Long,
            total: Long
        ) {
        }

        override fun onAcquisitionFailed(
            requestId: LcpPublicationRetriever.RequestId,
            error: LcpException
        ) {
            coroutineScope.launch {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.LcpAcquisitionFailed(error)
                    )
                )
            }
        }
    }

    val channel: Channel<Event> =
        Channel(Channel.BUFFERED)

    suspend fun importBook(
        contentUri: Uri
    ) {
        contentUri.copyToTempFile(context, storageDir)
            .mapFailure { ImportError.ImportBookFailed(it) }
            .map { addLocalBook(it) }
    }

    suspend fun importOpdsPublication(
        publication: Publication
    ) {
        opdsDownloader.download(publication)
            .getOrElse {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.OpdsError(it)
                    )
                )
            }
    }

    suspend fun addRemoteBook(
        url: Url
    ) {
        val asset = assetRetriever.retrieve(url)
            ?: run {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.PublicationError(
                            PublicationError.UnsupportedPublication(
                                Publication.OpeningException.UnsupportedAsset()
                            )
                        )
                    )
                )
                return
            }

        addBook(url, asset)
            .onSuccess { channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    suspend fun addSharedStorageBook(
        url: Url,
        coverUrl: String? = null
    ) {
        val asset = assetRetriever.retrieve(url)
            ?: run {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.PublicationError(
                            PublicationError.UnsupportedPublication(
                                Publication.OpeningException.UnsupportedAsset(
                                    "Unsupported media type"
                                )
                            )
                        )
                    )
                )
                return
            }

        addBook(url, asset, coverUrl)
            .onSuccess { channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    suspend fun addLocalBook(
        tempFile: File,
        coverUrl: String? = null
    ) {
        val sourceAsset = assetRetriever.retrieve(tempFile)
            ?: run {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.PublicationError(
                            PublicationError.UnsupportedPublication(
                                Publication.OpeningException.UnsupportedAsset()
                            )
                        )
                    )
                )
                return
            }

        if (
            sourceAsset is Asset.Resource &&
            sourceAsset.mediaType.matches(MediaType.LCP_LICENSE_DOCUMENT)
        ) {
            acquireLcpPublication(sourceAsset)
            return
        }

        val fileExtension = formatRegistry.fileExtension(sourceAsset.mediaType) ?: "epub"
        val fileName = "${UUID.randomUUID()}.$fileExtension"
        val libraryFile = File(storageDir, fileName)

        try {
            tempFile.moveTo(libraryFile)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { libraryFile.delete() }
            channel.send(
                Event.ImportPublicationError(
                    ImportError.ImportBookFailed(e)
                )
            )
            return
        }

        addActualLocalBook(
            libraryFile,
            sourceAsset.mediaType,
            sourceAsset.assetType,
            coverUrl
        ).onSuccess {
            channel.send(Event.ImportPublicationSuccess)
        }.onFailure {
            tryOrNull { libraryFile.delete() }
            channel.send(Event.ImportPublicationError(it))
        }
    }

    private suspend fun acquireLcpPublication(licenceAsset: Asset.Resource) {
        val lcpRetriever = lcpPublicationRetriever
            .getOrElse {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.LcpAcquisitionFailed(it)
                    )
                )
                return
            }

        val license = licenceAsset.resource.read()
            .getOrElse {
                channel.send(
                    Event.ImportPublicationError(
                        ImportError.LcpAcquisitionFailed(it)
                    )
                )
                return
            }

        lcpRetriever.retrieve(license, "LCP Publication", "Downloading")
    }

    private suspend fun addActualLocalBook(
        libraryFile: File,
        mediaType: MediaType,
        assetType: AssetType,
        coverUrl: String?
    ): Try<Unit, ImportError> {
        val libraryUrl = libraryFile.toUrl()
        val libraryAsset = assetRetriever.retrieve(
            libraryUrl,
            mediaType,
            assetType
        ).getOrElse { return Try.failure(ImportError.PublicationError(it)) }

        return addBook(
            libraryUrl,
            libraryAsset,
            coverUrl
        )
    }

    private suspend fun addBook(
        url: Url,
        asset: Asset,
        coverUrl: String? = null
    ): Try<Unit, ImportError> {
        val drmScheme =
            protectionRetriever.retrieve(asset)

        publicationFactory.open(
            asset,
            contentProtectionScheme = drmScheme,
            allowUserInteraction = false
        ).onSuccess { publication ->
            val coverBitmap: Bitmap? = coverUrl
                ?.let { getBitmapFromURL(it) }
                ?: publication.cover()
            val coverFile =
                try {
                    storeCover(coverBitmap)
                } catch (e: Exception) {
                    return Try.failure(ImportError.ImportBookFailed(e))
                }

            val id = bookRepository.insertBookIntoDatabase(
                url.toString(),
                asset.mediaType,
                asset.assetType,
                drmScheme,
                publication,
                coverFile.path
            )
            if (id == -1L) {
                coverFile.delete()
                return Try.failure(ImportError.ImportDatabaseFailed())
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
}
