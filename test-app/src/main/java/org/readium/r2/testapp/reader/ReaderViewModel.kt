/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.SearchTry
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.model.Highlight
import org.readium.r2.testapp.domain.toUserError
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel
import org.readium.r2.testapp.reader.tts.TtsViewModel
import org.readium.r2.testapp.search.SearchPagingSource
import org.readium.r2.testapp.utils.EventChannel
import org.readium.r2.testapp.utils.UserError
import org.readium.r2.testapp.utils.createViewModelFactory
import org.readium.r2.testapp.utils.extensions.toHtml
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
class ReaderViewModel(
    private val bookId: Long,
    private val readerRepository: ReaderRepository,
    private val bookRepository: BookRepository,
) : ViewModel(),
    EpubNavigatorFragment.Listener,
    ImageNavigatorFragment.Listener,
    PdfNavigatorFragment.Listener {

    val readerInitData =
        try {
            checkNotNull(readerRepository[bookId])
        } catch (e: Exception) {
            // Fallbacks on a dummy Publication to avoid crashing the app until the Activity finishes.
            DummyReaderInitData(bookId)
        }

    val publication: Publication =
        readerInitData.publication

    val activityChannel: EventChannel<ActivityCommand> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val fragmentChannel: EventChannel<FragmentFeedback> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val visualFragmentChannel: EventChannel<VisualFragmentCommand> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val searchChannel: EventChannel<SearchCommand> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val tts: TtsViewModel? = TtsViewModel(
        viewModelScope = viewModelScope,
        readerInitData = readerInitData
    )

    val settings: UserPreferencesViewModel<*, *>? = UserPreferencesViewModel(
        viewModelScope = viewModelScope,
        readerInitData = readerInitData
    )

    override fun onCleared() {
        // When the ReaderViewModel is disposed of, we want to close the publication to avoid
        // using outdated information (such as the initial location) if the `ReaderActivity` is
        // opened again with the same book.
        readerRepository.close(bookId)
    }

    fun saveProgression(locator: Locator) = viewModelScope.launch {
        Timber.v("Saving locator for book $bookId: $locator.")
        bookRepository.saveProgression(locator, bookId)
    }

    fun getBookmarks() = bookRepository.bookmarksForBook(bookId)

    fun insertBookmark(locator: Locator) = viewModelScope.launch {
        val id = bookRepository.insertBookmark(bookId, publication, locator)
        if (id != -1L) {
            fragmentChannel.send(FragmentFeedback.BookmarkSuccessfullyAdded)
        } else {
            fragmentChannel.send(FragmentFeedback.BookmarkFailed)
        }
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch {
        bookRepository.deleteBookmark(id)
    }

    // Highlights

    val highlights: Flow<List<Highlight>> by lazy {
        bookRepository.highlightsForBook(bookId)
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
            extras = mapOf(
                // We store the highlight's database ID in the extras map, for easy retrieval
                // later. You can store arbitrary information in the map.
                "id" to id
            )
        )

        return listOfNotNull(
            // Decoration for the actual highlight / underline.
            createDecoration(
                idSuffix = "highlight",
                style = when (style) {
                    Highlight.Style.HIGHLIGHT -> Decoration.Style.Highlight(
                        tint = tint,
                        isActive = isActive
                    )
                    Highlight.Style.UNDERLINE -> Decoration.Style.Underline(
                        tint = tint,
                        isActive = isActive
                    )
                }
            ),
            // Additional page margin icon decoration, if the highlight has an associated note.
            annotation.takeIf { it.isNotEmpty() }?.let {
                createDecoration(
                    idSuffix = "annotation",
                    style = DecorationStyleAnnotationMark(tint = tint)
                )
            }
        )
    }

    suspend fun highlightById(id: Long): Highlight? =
        bookRepository.highlightById(id)

    fun addHighlight(
        locator: Locator,
        style: Highlight.Style,
        @ColorInt tint: Int,
        annotation: String = "",
    ) = viewModelScope.launch {
        bookRepository.addHighlight(bookId, style, tint, locator, annotation)
    }

    fun updateHighlightAnnotation(id: Long, annotation: String) = viewModelScope.launch {
        bookRepository.updateHighlightAnnotation(id, annotation)
    }

    fun updateHighlightStyle(id: Long, style: Highlight.Style, @ColorInt tint: Int) = viewModelScope.launch {
        bookRepository.updateHighlightStyle(id, style, tint)
    }

    fun deleteHighlight(id: Long) = viewModelScope.launch {
        bookRepository.deleteHighlight(id)
    }

    // Search

    fun search(query: String) = viewModelScope.launch {
        if (query == lastSearchQuery) return@launch
        lastSearchQuery = query
        _searchLocators.value = emptyList()
        searchIterator = publication.search(query)
            ?: run {
                activityChannel.send(
                    ActivityCommand.ToastError(
                        UserError(R.string.search_error_not_searchable, cause = null)
                    )
                )
                null
            }
        pagingSourceFactory.invalidate()
        searchChannel.send(SearchCommand.StartNewSearch)
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

    // Navigator.Listener

    override fun onResourceLoadFailed(href: Url, error: ReadError) {
        activityChannel.send(
            ActivityCommand.ToastError(error.toUserError())
        )
    }

    // HyperlinkNavigator.Listener
    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        activityChannel.send(ActivityCommand.OpenExternalLink(url))
    }

    override fun shouldFollowInternalLink(
        link: Link,
        context: HyperlinkNavigator.LinkContext?,
    ): Boolean =
        when (context) {
            is HyperlinkNavigator.FootnoteContext -> {
                val text =
                    if (link.mediaType?.isHtml == true) {
                        context.noteContent.toHtml()
                    } else {
                        context.noteContent
                    }

                val command = VisualFragmentCommand.ShowPopup(text)
                visualFragmentChannel.send(command)
                false
            }
            else -> true
        }

    // Search

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

    // Events

    sealed class ActivityCommand {
        object OpenOutlineRequested : ActivityCommand()
        object OpenDrmManagementRequested : ActivityCommand()
        class OpenExternalLink(val url: AbsoluteUrl) : ActivityCommand()
        class ToastError(val error: UserError) : ActivityCommand()
    }

    sealed class FragmentFeedback {
        object BookmarkSuccessfullyAdded : FragmentFeedback()
        object BookmarkFailed : FragmentFeedback()
    }

    sealed class VisualFragmentCommand {
        class ShowPopup(val text: CharSequence) : VisualFragmentCommand()
    }

    sealed class SearchCommand {
        object StartNewSearch : SearchCommand()
    }

    companion object {
        fun createFactory(application: Application, arguments: ReaderActivityContract.Arguments) =
            createViewModelFactory {
                ReaderViewModel(
                    arguments.bookId,
                    application.readerRepository,
                    application.bookRepository
                )
            }
    }
}
