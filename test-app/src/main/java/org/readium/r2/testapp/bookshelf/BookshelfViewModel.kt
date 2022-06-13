/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.reader.ReaderRepository
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
import kotlin.time.ExperimentalTime

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val books = app.bookRepository.books()

    private val app get() = getApplication<org.readium.r2.testapp.Application>()

    private val preferences =
        application.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

    init {
        copySamplesFromAssetsToStorage()
    }

    fun deleteBook(book: Book) = viewModelScope.launch {
        book.id?.let { app.bookRepository.deleteBook(it) }
        tryOrNull { File(book.href).delete() }
        tryOrNull { File(app.storageDir, "covers/${book.id}.png").delete() }
    }

    private suspend fun addPublicationToDatabase(
        href: String,
        mediaType: MediaType,
        publication: Publication
    ): Long {
        val id = app.bookRepository.insertBook(href, mediaType, publication)
        storeCoverImage(publication, id.toString())
        return id
    }

    fun copySamplesFromAssetsToStorage() = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.IO) {
            if (!preferences.contains("samples")) {
                val dir = app.storageDir
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val samples = app.assets.list("Samples")?.filterNotNull().orEmpty()
                for (element in samples) {
                    val file =
                        app.assets.open("Samples/$element").copyToTempFile(app.storageDir)
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
        uri: Uri
    ) = viewModelScope.launch {
        uri.copyToTempFile(app, app.storageDir)
            ?.let {
                importPublication(it)
            }
    }

    private suspend fun importPublication(
        sourceFile: File
    ) {
        val sourceMediaType = sourceFile.mediaType()
        val publicationAsset: FileAsset =
            if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT)
                FileAsset(sourceFile, sourceMediaType)
            else {
                app.readium.lcpService
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
                            channel.send(Event.ImportPublicationFailed(it.message))
                            return
                        }
                    )
            }

        val mediaType = publicationAsset.mediaType()
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryAsset = FileAsset(File(app.storageDir, fileName), mediaType)

        try {
            publicationAsset.file.moveTo(libraryAsset.file)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { publicationAsset.file.delete() }
            channel.send(Event.UnableToMovePublication)
            return
        }

        app.readium.streamer.open(libraryAsset, allowUserInteraction = false)
            .onSuccess {
                addPublicationToDatabase(libraryAsset.file.path, libraryAsset.mediaType(), it).let { id ->

                    if (id != -1L)
                        channel.send(Event.ImportPublicationSuccess)
                    else
                        channel.send(Event.ImportDatabaseFailed)
                }
            }
            .onFailure {
                tryOrNull { libraryAsset.file.delete() }
                Timber.d(it)
                channel.send(Event.ImportPublicationFailed(it.getUserMessage(app)))
            }
    }

    @OptIn(ExperimentalTime::class)
    fun openBook(
        bookId: Long,
        activity: Activity
    ) = viewModelScope.launch {
        val readerRepository = app.readerRepository.await()
        readerRepository.open(bookId, activity)
            .onFailure { exception ->
                if (exception is ReaderRepository.CancellationException)
                    return@launch

                val message = when (exception) {
                    is UserException -> exception.getUserMessage(app)
                    else -> exception.message
                }
                channel.send(Event.OpenBookError(message))
            }
            .onSuccess {
                val arguments = ReaderActivityContract.Arguments(bookId)
                channel.send(Event.LaunchReader(arguments))
            }
    }

    fun closeBook(bookId: Long) = viewModelScope.launch {
        val readerRepository = app.readerRepository.await()
        readerRepository.close(bookId)
    }

    private fun storeCoverImage(publication: Publication, imageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            // TODO Figure out where to store these cover images
            val coverImageDir = File(app.storageDir, "covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }
            val coverImageFile = File(app.storageDir, "covers/${imageName}.png")

            val bitmap: Bitmap? = publication.cover()

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

        class LaunchReader(val arguments: ReaderActivityContract.Arguments) : Event()
    }
}