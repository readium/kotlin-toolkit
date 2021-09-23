/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.domain.model.Catalog
import org.readium.r2.testapp.utils.EventChannel
import java.net.URL

class CatalogFeedListViewModel(application: Application) : AndroidViewModel(application) {

    private val catalogDao = BookDatabase.getDatabase(application).catalogDao()
    private val repository = CatalogRepository(catalogDao)
    val eventChannel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)

    val catalogs = repository.getCatalogsFromDatabase()

    fun insertCatalog(catalog: Catalog) = viewModelScope.launch {
        repository.insertCatalog(catalog)
    }

    fun deleteCatalog(id: Long) = viewModelScope.launch {
        repository.deleteCatalog(id)
    }

    fun parseCatalog(url: String, title: String) = viewModelScope.launch {
        val parseData = parseURL(URL(url))
        parseData.onSuccess { data ->
            val catalog = Catalog(
                title = title,
                href = url,
                type = data.type
            )
            insertCatalog(catalog)
        }
        parseData.onFailure {
            eventChannel.send(Event.FeedListEvent.CatalogParseFailed)
        }
    }

    private suspend fun parseURL(url: URL): Try<ParseData, Exception> {
        return DefaultHttpClient().fetchWithDecoder(HttpRequest(url.toString())) {
            val result = it.body
            if (isJson(result)) {
                OPDS2Parser.parse(result, url)
            } else {
                OPDS1Parser.parse(result, url)
            }
        }
    }

    private fun isJson(byteArray: ByteArray): Boolean {
        return try {
            JSONObject(String(byteArray))
            true
        } catch (e: Exception) {
            false
        }
    }

    sealed class Event {

        sealed class FeedListEvent : Event() {

            object CatalogParseFailed : FeedListEvent()
        }
    }
}