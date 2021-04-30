/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.R2App
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.utils.EventChannel
import org.readium.r2.testapp.utils.extensions.copyToTempFile
import org.readium.r2.testapp.utils.extensions.moveTo
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val r2Application = application
    private val booksDao = BookDatabase.getDatabase(application).booksDao()
    private val repository = BookRepository(booksDao)
    private val preferences =
        application.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
    private var server: Server = R2App.server
    private var lcpService = LcpService(application)
        ?.let { Try.success(it) }
        ?: Try.failure(Exception("liblcp is missing on the classpath"))
    private var streamer = Streamer(
        application,
        contentProtections = listOfNotNull(
            lcpService.getOrNull()?.contentProtection()
        )
    )
    private var r2Directory: String = R2App.R2DIRECTORY
    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val showProgressBar = ObservableBoolean()

    val books = repository.getBooksFromDatabase()

    fun deleteBook(book: Book) = viewModelScope.launch {
        book.id?.let { repository.deleteBook(it) }
        tryOrNull { File(book.href).delete() }
        tryOrNull { File("${R2App.R2DIRECTORY}covers/${book.id}.png").delete() }
    }

    private suspend fun addPublicationToDatabase(
        href: String,
        extension: String,
        publication: Publication
    ): Long {
        val id = repository.insertBook(href, extension, publication)
        storeCoverImage(publication, id.toString())
        return id
    }

    fun copySamplesFromAssetsToStorage() = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.IO) {
            if (!preferences.contains("samples")) {
                val dir = File(r2Directory)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val samples = r2Application.assets.list("Samples")?.filterNotNull().orEmpty()
                for (element in samples) {
                    val file =
                        r2Application.assets.open("Samples/$element").copyToTempFile(r2Directory)
                    if (file != null)
                        importPublication(file)
                    else if (BuildConfig.DEBUG)
                        error("Unable to load sample into the library")
                }
                preferences.edit().putBoolean("samples", true).apply()
            }
        }
    }

    fun importPublicationFromUri(
        uri: Uri,
        sourceUrl: String? = null
    ) = viewModelScope.launch {
        showProgressBar.set(true)
        uri.copyToTempFile(r2Application, r2Directory)
            ?.let {
                importPublication(it, sourceUrl)
            }
    }

    private suspend fun importPublication(
        sourceFile: File,
        sourceUrl: String? = null
    ) {
        val sourceMediaType = sourceFile.mediaType()
        val publicationAsset: FileAsset =
            if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT)
                FileAsset(sourceFile, sourceMediaType)
            else {
                lcpService
                    .flatMap { it.acquirePublication(sourceFile) }
                    .fold(
                        {
                            val mediaType =
                                MediaType.of(fileExtension = File(it.suggestedFilename).extension)
                            FileAsset(it.localFile, mediaType)
                        },
                        {
                            tryOrNull { sourceFile.delete() }
                            Timber.d(it)
                            showProgressBar.set(false)
                            channel.send(Event.ImportPublicationFailed(it.message))
                            return
                        }
                    )
            }

        val mediaType = publicationAsset.mediaType()
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryAsset = FileAsset(File(r2Directory + fileName), mediaType)

        try {
            publicationAsset.file.moveTo(libraryAsset.file)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { publicationAsset.file.delete() }
            showProgressBar.set(false)
            channel.send(Event.UnableToMovePublication)
            return
        }

        val extension = libraryAsset.let {
            it.mediaType().fileExtension ?: it.file.extension
        }

        val isRwpm = libraryAsset.mediaType().isRwpm

        val bddHref =
            if (!isRwpm)
                libraryAsset.file.path
            else
                sourceUrl ?: run {
                    Timber.e("Trying to add a RWPM to the database from a file without sourceUrl.")
                    showProgressBar.set(false)
                    return
                }

        streamer.open(libraryAsset, allowUserInteraction = false, sender = r2Application)
            .onSuccess {
                addPublicationToDatabase(bddHref, extension, it).let { id ->

                    showProgressBar.set(false)
                    if (id != -1L)
                        channel.send(Event.ImportPublicationSuccess)
                    else
                        channel.send(Event.ImportDatabaseFailed)
                    if (id != -1L && isRwpm)
                        tryOrNull { libraryAsset.file.delete() }
                }
            }
            .onFailure {
                tryOrNull { libraryAsset.file.delete() }
                Timber.d(it)
                showProgressBar.set(false)
                channel.send(Event.ImportPublicationFailed(it.getUserMessage(r2Application)))
            }
    }

    fun openBook(
        book: Book, context: Context,
        callback: (file: FileAsset, mediaType: MediaType?, publication: Publication, remoteAsset: FileAsset?, url: URL?) -> Unit
    ) = viewModelScope.launch {

        val remoteAsset: FileAsset? =
            tryOrNull { URL(book.href).copyToTempFile(r2Directory)?.let { FileAsset(it) } }
        val asset = remoteAsset // remote file
            ?: FileAsset(File(book.href)) // local file

        streamer.open(asset, allowUserInteraction = true, sender = context)
            .onFailure {
                Timber.d(it)
                channel.send(Event.OpenBookError(it.getUserMessage(r2Application)))
            }
            .onSuccess {
                if (it.isRestricted) {
                    it.protectionError?.let { error ->
                        Timber.d(error)
                        channel.send(Event.OpenBookError(error.getUserMessage(r2Application)))
                    }
                } else {
                    val url = prepareToServe(it, asset)
                    callback.invoke(asset, asset.mediaType(), it, remoteAsset, url)
                }
            }
    }

    private fun prepareToServe(publication: Publication, asset: PublicationAsset): URL? {
        val userProperties =
            r2Application.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json"
        return server.addPublication(publication, userPropertiesFile = File(userProperties))
    }

    private fun storeCoverImage(publication: Publication, imageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            // TODO Figure out where to store these cover images
            val coverImageDir = File("${r2Directory}covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }
            val coverImageFile = File("${r2Directory}covers/${imageName}.png")

            var bitmap: Bitmap? = null
            if (publication.cover() == null) {
                publication.coverLink?.let { link ->
                    bitmap = getBitmapFromURL(link.href)
                } ?: run {
                    if (publication.images.isNotEmpty()) {
                        bitmap = getBitmapFromURL(publication.images.first().href)
                    }
                }
            } else {
                bitmap = publication.cover()
            }

            val resized = bitmap?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
        }

    private fun getBitmapFromURL(src: String): Bitmap? {
        return try {
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

    sealed class Event {

        class ImportPublicationFailed(val errorMessage: String?) : Event()

        object UnableToMovePublication : Event()

        object ImportPublicationSuccess : Event()

        object ImportDatabaseFailed : Event()

        class OpenBookError(val errorMessage: String?) : Event()
    }
}