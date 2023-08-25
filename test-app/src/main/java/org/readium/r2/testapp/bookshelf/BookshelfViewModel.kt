/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.Url
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.EventChannel

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() =
        getApplication<org.readium.r2.testapp.Application>()

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val books = app.bookRepository.books()

    fun deletePublication(book: Book) =
        viewModelScope.launch {
            app.bookshelf.deleteBook(book)
        }

    fun copyPublicationToAppStorage(uri: Uri) {
        viewModelScope.launch {
            app.bookshelf.copyPublicationToAppStorage(uri)
        }
    }

    fun addPublicationFromSharedStorage(uri: Uri) {
        viewModelScope.launch {
            app.bookshelf.addPublicationFromSharedStorage(Url(uri.toString())!!)
        }
    }

    fun addPublicationFromTheWeb(url: Url) {
        viewModelScope.launch {
            app.bookshelf.addPublicationFromTheWeb(url)
        }
    }

    fun openPublication(
        bookId: Long,
        activity: Activity
    ) {
        viewModelScope.launch {
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
    }

    sealed class Event {

        class OpenPublicationError(
            val errorMessage: String
        ) : Event()

        class LaunchReader(
            val arguments: ReaderActivityContract.Arguments
        ) : Event()
    }
}
