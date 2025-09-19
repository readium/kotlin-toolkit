/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.app.Application
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.readium.r2.testapp.domain.Bookshelf
import org.readium.r2.testapp.domain.ImportError
import org.readium.r2.testapp.utils.EventChannel

data class TopBarState(
    val title: String = "Readium",
    val actions: @Composable RowScope.() -> Unit = {},
)

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app =
        getApplication<org.readium.r2.testapp.Application>()

    val channel: EventChannel<Event> =
        EventChannel(Channel(Channel.UNLIMITED), viewModelScope)

    private val _topBarState = MutableStateFlow(TopBarState())
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    fun updateTopBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
        _topBarState.update { TopBarState(title, actions) }
    }

    init {
        app.bookshelf.channel.receiveAsFlow()
            .onEach { sendImportFeedback(it) }
            .launchIn(viewModelScope)
    }

    private fun sendImportFeedback(event: Bookshelf.Event) {
        when (event) {
            is Bookshelf.Event.ImportPublicationError -> {
                channel.send(Event.ImportPublicationError(event.error))
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
            val error: ImportError,
        ) : Event()
    }
}
