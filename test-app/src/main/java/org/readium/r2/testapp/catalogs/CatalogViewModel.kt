/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.net.MalformedURLException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.testapp.Bookshelf
import org.readium.r2.testapp.domain.model.Catalog
import org.readium.r2.testapp.utils.EventChannel
import timber.log.Timber

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    val detailChannel = EventChannel(Channel<Event.DetailEvent>(Channel.BUFFERED), viewModelScope)
    val eventChannel = EventChannel(Channel<Event.FeedEvent>(Channel.BUFFERED), viewModelScope)

    lateinit var publication: Publication
    private val app = getApplication<org.readium.r2.testapp.Application>()

    init {
        app.bookshelf.channel.receiveAsFlow()
            .onEach { sendImportFeedback(it) }
            .launchIn(viewModelScope)
    }

    fun parseCatalog(catalog: Catalog) = viewModelScope.launch {
        var parseRequest: Try<ParseData, Exception>? = null
        catalog.href.let {
            val request = HttpRequest(it)
            try {
                parseRequest = if (catalog.type == 1) {
                    OPDS1Parser.parseRequest(request, app.readium.httpClient)
                } else {
                    OPDS2Parser.parseRequest(request, app.readium.httpClient)
                }
            } catch (e: MalformedURLException) {
                eventChannel.send(Event.FeedEvent.CatalogParseFailed)
            }
        }
        parseRequest?.onSuccess {
            eventChannel.send(Event.FeedEvent.CatalogParseSuccess(it))
        }
        parseRequest?.onFailure {
            Timber.e(it)
            eventChannel.send(Event.FeedEvent.CatalogParseFailed)
        }
    }

    fun downloadPublication(publication: Publication) = viewModelScope.launch {
        app.bookshelf.importOpdsPublication(publication)
    }

    private fun sendImportFeedback(event: Bookshelf.Event) {
        when (event) {
            is Bookshelf.Event.ImportPublicationError -> {
                val errorMessage = event.error.getUserMessage(app)
                detailChannel.send(Event.DetailEvent.ImportPublicationFailed(errorMessage))
            }

            Bookshelf.Event.ImportPublicationSuccess -> {
                detailChannel.send(Event.DetailEvent.ImportPublicationSuccess)
            }
        }
    }

    sealed class Event {

        sealed class FeedEvent : Event() {

            object CatalogParseFailed : FeedEvent()

            class CatalogParseSuccess(val result: ParseData) : FeedEvent()
        }

        sealed class DetailEvent : Event() {

            object ImportPublicationSuccess : DetailEvent()

            class ImportPublicationFailed(
                private val message: String
            ) : DetailEvent()
        }
    }
}
