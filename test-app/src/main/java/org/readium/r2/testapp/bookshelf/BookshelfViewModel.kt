/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Url
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.R
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.reader.ReaderRepository
import org.readium.r2.testapp.utils.EventChannel
import org.readium.r2.testapp.utils.extensions.copyToTempFile

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() =
        getApplication<org.readium.r2.testapp.Application>()

    private val preferences =
        application.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val books = app.bookRepository.books()

    init {
        copySamplesFromAssetsToStorage()
    }

    private fun copySamplesFromAssetsToStorage() = viewModelScope.launch(Dispatchers.IO) {
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
                        app.bookRepository.addLocalBook(file)
                    else if (BuildConfig.DEBUG)
                        error("Unable to load sample into the library")
                }
                preferences.edit().putBoolean("samples", true).apply()
            }
        }
    }

    fun deletePublication(book: Book) =
        viewModelScope.launch {
            app.bookRepository.deleteBook(book)
        }

    fun importPublicationFromUri(uri: Uri) =
        viewModelScope.launch {
            app.bookRepository
                .importBook(uri)
                .exceptionOrNull()
                .let { sendImportFeedback(it) }
        }

    fun addSharedStoragePublication(uri: Uri) =
        viewModelScope.launch {
            app.bookRepository
                .addSharedStorageBook(Url(uri.toString())!!)
                .exceptionOrNull()
                .let { sendImportFeedback(it) }
        }

    fun addRemotePublication(url: Url) {
        viewModelScope.launch {
            val exception =
                if (!url.scheme.startsWith("http")) {
                    BookRepository.ImportException.UnsupportedProtocol(url.scheme)
                } else {
                    app.bookRepository
                        .addRemoteBook(url)
                        .exceptionOrNull()
                }
            sendImportFeedback(exception)
        }
    }

    private fun sendImportFeedback(exception: BookRepository.ImportException?) {
        if (exception == null) {
            channel.send(Event.ImportPublicationSuccess)
        } else {
            val errorMessage = when (exception) {
                is BookRepository.ImportException.UnableToOpenPublication ->
                    exception.exception.getUserMessage(app)
                BookRepository.ImportException.ImportDatabaseFailed ->
                    app.getString(R.string.unable_add_pub_database)
                is BookRepository.ImportException.LcpAcquisitionFailed ->
                    "Error: " + exception.message
                BookRepository.ImportException.IOException ->
                    app.getString(R.string.unexpected_io_exception)
                is BookRepository.ImportException.UnsupportedProtocol ->
                    app.getString(R.string.unsupported_protocol)
            }
            channel.send(Event.ImportPublicationError(errorMessage))
        }
    }

    fun openPublication(
        bookId: Long,
        activity: Activity
    ) = viewModelScope.launch {
        val readerRepository = app.readerRepository.await()
        readerRepository.open(bookId, activity)
            .onFailure { error ->
                val messageId = when (error) {
                    is ReaderRepository.OpeningError.Forbidden ->
                        R.string.r2_shared_publication_opening_exception_forbidden
                    is ReaderRepository.OpeningError.NotFound ->
                        R.string.r2_shared_publication_opening_exception_not_found
                    is ReaderRepository.OpeningError.OutOfMemory ->
                        R.string.r2_shared_resource_exception_out_of_memory
                    is ReaderRepository.OpeningError.Unavailable ->
                        R.string.r2_shared_publication_opening_exception_unavailable
                    is ReaderRepository.OpeningError.Unexpected ->
                        R.string.r2_shared_resource_exception_unexpected
                    is ReaderRepository.OpeningError.UnsupportedPublication ->
                        R.string.r2_shared_publication_opening_exception_unsupported_format
                }
                val message = activity.getString(messageId)
                channel.send(Event.OpenPublicationError(message))
            }
            .onSuccess {
                val arguments = ReaderActivityContract.Arguments(bookId)
                channel.send(Event.LaunchReader(arguments))
            }
    }

    sealed class Event {

        object ImportPublicationSuccess : Event()

        class ImportPublicationError(
            val errorMessage: String
        ) : Event()

        class OpenPublicationError(
            val errorMessage: String?
        ) : Event()

        class LaunchReader(
            val arguments: ReaderActivityContract.Arguments
        ) : Event()
    }
}
