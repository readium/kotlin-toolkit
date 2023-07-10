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
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.reader.ReaderActivityContract
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
                .failureOrNull()
                .let { sendImportFeedback(it) }
        }

    fun addSharedStoragePublication(uri: Uri) =
        viewModelScope.launch {
            app.bookRepository
                .addSharedStorageBook(Url(uri.toString())!!)
                .failureOrNull()
                .let { sendImportFeedback(it) }
        }

    fun addRemotePublication(url: Url) {
        viewModelScope.launch {
            val exception = app.bookRepository
                .addRemoteBook(url)
                .failureOrNull()
            sendImportFeedback(exception)
        }
    }

    private fun sendImportFeedback(error: BookRepository.ImportError?) {
        if (error == null) {
            channel.send(Event.ImportPublicationSuccess)
        } else {
            val errorMessage = error.getUserMessage(app)
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
                val message = error.getUserMessage(app)
                channel.send(Event.OpenPublicationError(message))
            }
            .onSuccess {
                val arguments = ReaderActivityContract.Arguments(bookId)
                channel.send(Event.LaunchReader(arguments))
            }
    }

    sealed class Event {

        object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val errorMessage: String
        ) : Event()

        class OpenPublicationError(
            val errorMessage: String
        ) : Event()

        class LaunchReader(
            val arguments: ReaderActivityContract.Arguments
        ) : Event()
    }
}
