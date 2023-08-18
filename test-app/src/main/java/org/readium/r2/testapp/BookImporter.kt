/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.UserException
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.asset.AssetType
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationFactory
import org.readium.r2.testapp.utils.extensions.copyToTempFile
import org.readium.r2.testapp.utils.extensions.moveTo
import org.readium.r2.testapp.utils.tryOrNull
import timber.log.Timber

class BookImporter(
    private val context: Context,
    private val bookRepository: BookRepository,
    private val storageDir: File,
    private val lcpService: Try<LcpService, UserException>,
    private val publicationFactory: PublicationFactory,
    private val assetRetriever: AssetRetriever,
    private val protectionRetriever: ContentProtectionSchemeRetriever,
) {

    private val coverDir: File =
        File(storageDir, "covers/")
            .apply { if (!exists()) mkdirs() }

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

        class ImportDatabaseFailed :
            ImportError(R.string.import_publication_unable_add_pub_database)
    }

    suspend fun importBook(
        contentUri: Uri
    ): Try<Unit, ImportError> =
        contentUri.copyToTempFile(context, storageDir)
            .mapFailure { ImportError.ImportBookFailed(it) }
            .flatMap { addLocalBook(it) }

    suspend fun addRemoteBook(
        url: Url
    ): Try<Unit, ImportError> {
        val asset = assetRetriever.retrieve(url, fileExtension = url.extension)
            ?: return Try.failure(
                ImportError.PublicationError(
                    PublicationError.UnsupportedPublication(Publication.OpeningException.UnsupportedAsset())
                )
            )
        return addBook(url, asset)
    }

    suspend fun addSharedStorageBook(
        url: Url,
        coverUrl: String? = null,
    ): Try<Unit, ImportError> {
        val asset = assetRetriever.retrieve(url)
            ?: return Try.failure(
                ImportError.PublicationError(
                    PublicationError.UnsupportedPublication(
                        Publication.OpeningException.UnsupportedAsset("Unsupported media type")
                    )
                )
            )

        return addBook(url, asset, coverUrl)
    }

    suspend fun addLocalBook(
        tempFile: File,
        coverUrl: String? = null,
    ): Try<Unit, ImportError> {
        val sourceAsset = assetRetriever.retrieve(tempFile)
            ?: return Try.failure(
                ImportError.PublicationError(
                    PublicationError.UnsupportedPublication(Publication.OpeningException.UnsupportedAsset())
                )
            )

        val (publicationTempFile, publicationTempAsset) =
            if (sourceAsset.mediaType != MediaType.LCP_LICENSE_DOCUMENT) {
                tempFile to sourceAsset
            } else {
                lcpService
                    .flatMap {
                        sourceAsset.close()
                        it.acquirePublication(tempFile)
                    }
                    .fold(
                        {
                            val file = it.localFile
                            val asset = assetRetriever.retrieve(
                                file.toUrl(),
                                assetType = AssetType.Archive,
                                mediaType = it.mediaType
                            ).getOrElse { error ->
                                return Try.failure(ImportError.PublicationError(error))
                            }
                            file to asset
                        },
                        {
                            tryOrNull { tempFile.delete() }
                            return Try.failure(ImportError.LcpAcquisitionFailed(it))
                        }
                    )
            }

        val fileName = "${UUID.randomUUID()}.${publicationTempAsset.mediaType.fileExtension}"
        val libraryFile = File(storageDir, fileName)
        val libraryUrl = libraryFile.toUrl()

        try {
            publicationTempFile.moveTo(libraryFile)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { libraryFile.delete() }
            return Try.failure(ImportError.ImportBookFailed(e))
        }

        val libraryAsset = assetRetriever.retrieve(
            libraryUrl,
            publicationTempAsset.mediaType,
            publicationTempAsset.assetType
        ).getOrElse { return Try.failure(ImportError.PublicationError(it)) }

        return addBook(
            libraryUrl, libraryAsset, coverUrl
        ).onFailure {
            tryOrNull { libraryFile.delete() }
        }
    }

    private suspend fun addBook(
        url: Url,
        asset: Asset,
        coverUrl: String? = null,
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
