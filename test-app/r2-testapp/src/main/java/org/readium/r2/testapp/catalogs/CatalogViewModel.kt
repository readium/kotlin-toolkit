/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.catalogs

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.testapp.R2App
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.domain.model.Catalog
import org.readium.r2.testapp.opds.OPDSDownloader
import org.readium.r2.testapp.utils.EventChannel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val bookDao = BookDatabase.getDatabase(application).booksDao()
    private val bookRepository = BookRepository(bookDao)
    private var opdsDownloader = OPDSDownloader(application.applicationContext)
    private var r2Directory = R2App.R2DIRECTORY
    val detailChannel = EventChannel(Channel<Event.DetailEvent>(Channel.BUFFERED), viewModelScope)
    val eventChannel = EventChannel(Channel<Event.FeedEvent>(Channel.BUFFERED), viewModelScope)
    val parseData = MutableLiveData<ParseData>()
    val showProgressBar = ObservableBoolean()

    fun parseCatalog(catalog: Catalog) = viewModelScope.launch {
        var parseRequest: Try<ParseData, Exception>? = null
        catalog.href.let {
            val request = HttpRequest(it)
            try {
                parseRequest = if (catalog.type == 1) {
                    OPDS1Parser.parseRequest(request)
                } else {
                    OPDS2Parser.parseRequest(request)
                }
            } catch (e: MalformedURLException) {
                eventChannel.send(Event.FeedEvent.CatalogParseFailed)
            }
        }
        parseRequest?.onSuccess {
            parseData.postValue(it)
        }
        parseRequest?.onFailure {
            Timber.e(it)
            eventChannel.send(Event.FeedEvent.CatalogParseFailed)
        }
    }

    fun downloadPublication(publication: Publication) = viewModelScope.launch {
        showProgressBar.set(true)
        val downloadUrl = getDownloadURL(publication)
        val publicationUrl = opdsDownloader.publicationUrl(downloadUrl.toString())
        publicationUrl.onSuccess {
            val id = addPublicationToDatabase(it.first, "epub", publication)
            if (id != -1L) {
                detailChannel.send(Event.DetailEvent.ImportPublicationSuccess)
            } else {
                detailChannel.send(Event.DetailEvent.ImportPublicationFailed)
            }
        }
            .onFailure {
                detailChannel.send(Event.DetailEvent.ImportPublicationFailed)
            }

        showProgressBar.set(false)
    }

    private fun getDownloadURL(publication: Publication): URL? {
        var url: URL? = null
        val links = publication.links
        for (link in links) {
            val href = link.href
            if (href.contains(Publication.EXTENSION.EPUB.value) || href.contains(Publication.EXTENSION.LCPL.value)) {
                url = URL(href)
                break
            }
        }
        return url
    }

    private suspend fun addPublicationToDatabase(
        href: String,
        extension: String,
        publication: Publication
    ): Long {
        val id = bookRepository.insertBook(href, extension, publication)
        storeCoverImage(publication, id.toString())
        return id
    }

    private fun storeCoverImage(publication: Publication, imageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            // TODO Figure out where to store these cover images
            val coverImageDir = File("${r2Directory}covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }
            val coverImageFile = File("${r2Directory}covers/${imageName}.png")

            val bitmap: Bitmap? =
                publication.cover() ?: getBitmapFromURL(publication.images.first().href)

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

        sealed class FeedEvent : Event() {

            object CatalogParseFailed : FeedEvent()
        }

        sealed class DetailEvent : Event() {

            object ImportPublicationSuccess : DetailEvent()

            object ImportPublicationFailed : DetailEvent()
        }
    }
}