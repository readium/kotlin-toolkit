/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.domain.model.Catalog
import org.readium.r2.testapp.utils.EventChannel
import org.readium.r2.testapp.utils.extensions.downloadTo
import timber.log.Timber

class CatalogViewModel(private val application: Application) : AndroidViewModel(application) {

    val detailChannel = EventChannel(Channel<Event.DetailEvent>(Channel.BUFFERED), viewModelScope)
    val eventChannel = EventChannel(Channel<Event.FeedEvent>(Channel.BUFFERED), viewModelScope)
    lateinit var publication: Publication

    fun parseCatalog(catalog: Catalog) = viewModelScope.launch {
        var parseRequest: Try<ParseData, Exception>? = null
        catalog.href.let {
            val request = HttpRequest(it)
            try {
                parseRequest = if (catalog.type == 1) {
                    OPDS1Parser.parseRequest(request, application.readium.httpClient)
                } else {
                    OPDS2Parser.parseRequest(request, application.readium.httpClient)
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
        val filename = UUID.randomUUID().toString()
        val dest = File(application.storageDir, filename)

        getDownloadURL(publication)
            .flatMap { url ->
                url.downloadTo(
                    dest,
                    httpClient = application.readium.httpClient,
                    assetRetriever = application.readium.assetRetriever
                )
            }.flatMap {
                val opdsCover = publication.images.firstOrNull()?.href
                application.bookRepository.addLocalBook(dest, opdsCover)
            }.onSuccess {
                detailChannel.send(Event.DetailEvent.ImportPublicationSuccess)
            }.onFailure {
                detailChannel.send(Event.DetailEvent.ImportPublicationFailed)
            }
    }

    private fun getDownloadURL(publication: Publication): Try<URL, Exception> =
        publication.links
            .firstOrNull { it.mediaType.isPublication || it.mediaType == MediaType.LCP_LICENSE_DOCUMENT }
            ?.let {
                try {
                    Try.success(URL(it.href))
                } catch (e: Exception) {
                    Try.failure(e)
                }
            } ?: Try.failure(Exception("No supported link to acquire publication."))

    sealed class Event {

        sealed class FeedEvent : Event() {

            object CatalogParseFailed : FeedEvent()

            class CatalogParseSuccess(val result: ParseData) : FeedEvent()
        }

        sealed class DetailEvent : Event() {

            object ImportPublicationSuccess : DetailEvent()

            object ImportPublicationFailed : DetailEvent()
        }
    }
}
