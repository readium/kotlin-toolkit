/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.domain.Bookshelf
import org.readium.r2.testapp.utils.EventChannel

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app =
        getApplication<org.readium.r2.testapp.Application>()

    val channel: EventChannel<Event> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)
    init {
        app.bookshelf.channel.receiveAsFlow()
            .onEach { sendImportFeedback(it) }
            .launchIn(viewModelScope)
    }
    fun importPublicationFromUri(uri: Uri) =
        viewModelScope.launch {
            app.bookshelf.copyPublicationToAppStorage(uri)
        }

    private fun sendImportFeedback(event: Bookshelf.Event) {
        when (event) {
            is Bookshelf.Event.ImportPublicationError -> {
                val errorMessage = event.error.getUserMessage(app)
                channel.send(Event.ImportPublicationError(errorMessage))
            }
            Bookshelf.Event.ImportPublicationSuccess -> {
                channel.send(Event.ImportPublicationSuccess)
            }
        }
    }

    sealed class Event {

        object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val errorMessage: String
        ) : Event()
    }
}
