/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.domain.model.Highlight
import org.readium.r2.testapp.utils.EventChannel
import org.readium.r2.navigator.epub.Highlight as NavigatorHighlight

class ReaderViewModel(context: Context, arguments: ReaderContract.Input) : ViewModel() {

    val publication: Publication = arguments.publication
    val initialLocation: Locator? = arguments.initialLocator
    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val fragmentChannel = EventChannel(Channel<FeedbackEvent>(Channel.BUFFERED), viewModelScope)
    val bookId = arguments.bookId
    private val repository: BookRepository

    init {
        val booksDao = BookDatabase.getDatabase(context).booksDao()
        repository = BookRepository(booksDao)
    }

    fun saveProgression(locator: String) = viewModelScope.launch {
        repository.saveProgression(locator, bookId)
    }

    fun getBookmarks() = repository.getBookmarks(bookId)

    fun insertBookmark(locator: Locator) = viewModelScope.launch {
        val id = repository.insertBookmark(bookId, publication, locator)
        if (id != -1L) {
            fragmentChannel.send(FeedbackEvent.BookmarkSuccessfullyAdded)
        } else {
            fragmentChannel.send(FeedbackEvent.BookmarkFailed)
        }
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch {
        repository.deleteBookmark(id)
    }

    fun getHighlights(href: String? = null): LiveData<List<Highlight>> {
        return if (href == null)
            repository.getHighlights(bookId)
        else
            repository.getHighlights(bookId, href)
    }

    suspend fun getHighlightByHighlightId(highlightId: String): Highlight? {
        return repository.getHighlightByHighlightId(highlightId)
    }

    fun insertHighlight(navigatorHighlight: NavigatorHighlight, progression: Double, annotation: String? = null) = viewModelScope.launch {
        repository.insertHighlight(bookId, publication, navigatorHighlight, progression, annotation)
    }

    fun updateHighlight(id: String, color: Int? = null, annotation: String? = null, markStyle: String? = null) = viewModelScope.launch {
        repository.updateHighlight(id, color, annotation, markStyle)
    }

    fun deleteHighlightByHighlightId(highlightId: String) = viewModelScope.launch {
        repository.deleteHighlightByHighlightId(highlightId)
    }

    class Factory(private val context: Context, private val arguments: ReaderContract.Input)
        : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            modelClass.getDeclaredConstructor(Context::class.java, ReaderContract.Input::class.java)
                .newInstance(context.applicationContext, arguments)
    }

    sealed class Event {

        object OpenOutlineRequested : Event()

        object OpenDrmManagementRequested : Event()
    }

    sealed class FeedbackEvent {

        object BookmarkSuccessfullyAdded : FeedbackEvent()

        object BookmarkFailed : FeedbackEvent()
    }
}

