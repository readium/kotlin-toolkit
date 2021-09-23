/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.shared.Search
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationId
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.SearchTry
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.Try
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.domain.model.Highlight
import org.readium.r2.testapp.search.SearchPagingSource
import org.readium.r2.testapp.utils.EventChannel

@OptIn(Search::class, ExperimentalDecorator::class)
class ReaderViewModel(context: Context, arguments: ReaderContract.Input) : ViewModel() {

    val publication: Publication = arguments.publication
    val initialLocation: Locator? = arguments.initialLocator
    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val fragmentChannel = EventChannel(Channel<FeedbackEvent>(Channel.BUFFERED), viewModelScope)
    val bookId = arguments.bookId
    private val repository: BookRepository

    val publicationId: PublicationId get() = bookId.toString()

    init {
        val booksDao = BookDatabase.getDatabase(context).booksDao()
        repository = BookRepository(booksDao)
    }

    fun saveProgression(locator: Locator) = viewModelScope.launch {
        repository.saveProgression(locator, bookId)
    }

    fun getBookmarks() = repository.bookmarksForBook(bookId)

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

    // Highlights

    val highlights: Flow<List<Highlight>> by lazy {
        repository.highlightsForBook(bookId)
    }

    /**
     * Database ID of the active highlight for the current highlight pop-up. This is used to show
     * the highlight decoration in an "active" state.
     */
    var activeHighlightId = MutableStateFlow<Long?>(null)

    /**
     * Current state of the highlight decorations.
     *
     * It will automatically be updated when the highlights database table or the current
     * [activeHighlightId] change.
     */
    val highlightDecorations: Flow<List<Decoration>> by lazy {
        highlights.combine(activeHighlightId) { highlights, activeId ->
            highlights.flatMap { highlight ->
                highlight.toDecorations(isActive = (highlight.id == activeId))
            }
        }
    }

    /**
     * Creates a list of [Decoration] for the receiver [Highlight].
     */
    private fun Highlight.toDecorations(isActive: Boolean): List<Decoration> {
        fun createDecoration(idSuffix: String, style: Decoration.Style) = Decoration(
            id = "$id-$idSuffix",
            locator = locator,
            style = style,
            extras = Bundle().apply {
                // We store the highlight's database ID in the extras bundle, for easy retrieval
                // later. You can store arbitrary information in the bundle.
                putLong("id", id)
            }
        )

        return listOfNotNull(
            // Decoration for the actual highlight / underline.
            createDecoration(
                idSuffix = "highlight",
                style = when (style) {
                    Highlight.Style.HIGHLIGHT -> Decoration.Style.Highlight(tint = tint, isActive = isActive)
                    Highlight.Style.UNDERLINE -> Decoration.Style.Underline(tint = tint, isActive = isActive)
                }
            ),
            // Additional page margin icon decoration, if the highlight has an associated note.
            annotation.takeIf { it.isNotEmpty() }?.let {
                createDecoration(
                    idSuffix = "annotation",
                    style = DecorationStyleAnnotationMark(tint = tint),
                )
            }
        )
    }

    suspend fun highlightById(id: Long): Highlight? =
        repository.highlightById(id)

    fun addHighlight(locator: Locator, style: Highlight.Style, @ColorInt tint: Int, annotation: String = "") = viewModelScope.launch {
        repository.addHighlight(bookId, style, tint, locator, annotation)
    }

    fun updateHighlightAnnotation(id: Long, annotation: String) = viewModelScope.launch {
        repository.updateHighlightAnnotation(id, annotation)
    }

    fun updateHighlightStyle(id: Long, style: Highlight.Style, @ColorInt tint: Int) = viewModelScope.launch {
        repository.updateHighlightStyle(id, style, tint)
    }

    fun deleteHighlight(id: Long) = viewModelScope.launch {
        repository.deleteHighlight(id)
    }

    fun search(query: String) = viewModelScope.launch {
        if (query == lastSearchQuery) return@launch
        lastSearchQuery = query
        _searchLocators.value = emptyList()
        searchIterator = publication.search(query)
            .onFailure { channel.send(Event.Failure(it)) }
            .getOrNull()
        pagingSourceFactory.invalidate()
        channel.send(Event.StartNewSearch)
    }

    fun cancelSearch() = viewModelScope.launch {
        _searchLocators.value = emptyList()
        searchIterator?.close()
        searchIterator = null
        pagingSourceFactory.invalidate()
    }

    val searchLocators: StateFlow<List<Locator>> get() = _searchLocators
    private var _searchLocators = MutableStateFlow<List<Locator>>(emptyList())

    /**
     * Maps the current list of search result locators into a list of [Decoration] objects to
     * underline the results in the navigator.
     */
    val searchDecorations: Flow<List<Decoration>> by lazy {
        searchLocators.map {
            it.mapIndexed { index, locator ->
                Decoration(
                    // The index in the search result list is a suitable Decoration ID, as long as
                    // we clear the search decorations between two searches.
                    id = index.toString(),
                    locator = locator,
                    style = Decoration.Style.Underline(tint = Color.RED)
                )
            }
        }
    }

    private var lastSearchQuery: String? = null

    private var searchIterator: SearchIterator? = null

    private val pagingSourceFactory = InvalidatingPagingSourceFactory {
        SearchPagingSource(listener = PagingSourceListener())
    }

    inner class PagingSourceListener : SearchPagingSource.Listener {
        override suspend fun next(): SearchTry<LocatorCollection?> {
            val iterator = searchIterator ?: return Try.success(null)
            return iterator.next().onSuccess {
                _searchLocators.value += (it?.locators ?: emptyList())
            }
        }
    }

    val searchResult: Flow<PagingData<Locator>> =
        Pager(PagingConfig(pageSize = 20), pagingSourceFactory = pagingSourceFactory)
            .flow.cachedIn(viewModelScope)

    class Factory(private val context: Context, private val arguments: ReaderContract.Input)
        : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            modelClass.getDeclaredConstructor(Context::class.java, ReaderContract.Input::class.java)
                .newInstance(context.applicationContext, arguments)
    }

    sealed class Event {
        object OpenOutlineRequested : Event()
        object OpenDrmManagementRequested : Event()
        object StartNewSearch : Event()
        class Failure(val error: UserException) : Event()
    }

    sealed class FeedbackEvent {
        object BookmarkSuccessfullyAdded : FeedbackEvent()
        object BookmarkFailed : FeedbackEvent()
    }
}
