/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.R
import org.readium.r2.shared.Search
import org.readium.r2.shared.UserException
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpException

@Search
typealias SearchTry<SuccessT> = Try<SuccessT, SearchException>

/**
 * Represents an error which might occur during a search activity.
 */
@Search
sealed class SearchException(content: Content, cause: Throwable? = null) : UserException(content, cause) {
    constructor(@StringRes userMessageId: Int, vararg args: Any, cause: Throwable? = null) :
        this(Content(userMessageId, *args), cause)
    constructor(cause: UserException) :
        this(Content(cause), cause)

    /**
     * The publication is not searchable.
     */
    object PublicationNotSearchable : SearchException(R.string.r2_shared_search_exception_publication_not_searchable)

    /**
     * The provided search query cannot be handled by the service.
     */
    class BadQuery(cause: UserException) : SearchException(cause)

    /**
     * An error occurred while accessing one of the publication's resources.
     */
    class ResourceError(cause: Resource.Exception) : SearchException(cause)

    /**
     * An error occurred while performing an HTTP request.
     */
    class NetworkError(cause: HttpException) : SearchException(cause)

    /**
     * The search was cancelled by the caller.
     *
     * For example, when a coroutine or a network request is cancelled.
     */
    object Cancelled : SearchException(R.string.r2_shared_search_exception_cancelled)

    /** For any other custom service error. */
    class Other(cause: Throwable) : SearchException(R.string.r2_shared_search_exception_other, cause = cause)

    companion object {
        fun wrap(e: Throwable): SearchException =
            when (e) {
                is SearchException -> e
                is CancellationException, is Resource.Exception.Cancelled -> Cancelled
                is Resource.Exception -> ResourceError(e)
                is HttpException ->
                    if (e.kind == HttpException.Kind.Cancelled) {
                        Cancelled
                    } else {
                        NetworkError(e)
                    }
                else -> Other(e)
            }
    }
}

/**
 * Provides a way to search terms in a publication.
 */
@Search
interface SearchService : Publication.Service {

    /**
     * Holds the available search options and their current values.
     *
     * @param caseSensitive Whether the search will differentiate between capital and lower-case
     *        letters.
     * @param diacriticSensitive Whether the search will differentiate between letters with accents
     *        or not.
     * @param wholeWord Whether the query terms will match full words and not parts of a word.
     * @param exact Matches results exactly as stated in the query terms, taking into account stop
     *        words, order and spelling.
     * @param language BCP 47 language code overriding the publication's language.
     * @param regularExpression The search string is treated as a regular expression. The particular
     *        flavor of regex depends on the service.
     * @param otherOptions Map of custom options implemented by a Search Service which are not
     *        officially recognized by Readium.
     */
    @Parcelize
    data class Options(
        val caseSensitive: Boolean? = null,
        val diacriticSensitive: Boolean? = null,
        val wholeWord: Boolean? = null,
        val exact: Boolean? = null,
        val language: String? = null,
        val regularExpression: Boolean? = null,
        val otherOptions: Map<String, String> = emptyMap()
    ) : Parcelable {
        /**
         * Syntactic sugar to access the [otherOptions] values by subscripting [Options] directly.
         */
        operator fun get(key: String): String? = otherOptions[key]
    }

    /**
     * Default value for the search options of this service.
     *
     * If an option does not have a value, it is not supported by the service.
     */
    val options: Options

    /**
     * Starts a new search through the publication content, with the given [query].
     *
     * If an option is nil when calling search(), its value is assumed to be the default one.
     */
    suspend fun search(query: String, options: Options? = null): SearchTry<SearchIterator>
}

/**
 * Indicates whether the content of this publication can be searched.
 */
@Search
val Publication.isSearchable get() =
    findService(SearchService::class) != null

/**
 * Default value for the search options of this publication.
 */
@Search
val Publication.searchOptions: SearchService.Options get() =
    findService(SearchService::class)?.options ?: SearchService.Options()

/**
 * Starts a new search through the publication content, with the given [query].
 *
 * If an option is nil when calling [search], its value is assumed to be the default one for the
 * search service.
 */
@Search
suspend fun Publication.search(query: String, options: SearchService.Options? = null): SearchTry<SearchIterator> =
    findService(SearchService::class)?.search(query, options)
        ?: Try.failure(SearchException.PublicationNotSearchable)

/** Factory to build a [SearchService] */
@Search
var Publication.ServicesBuilder.searchServiceFactory: ServiceFactory?
    get() = get(SearchService::class)
    set(value) = set(SearchService::class, value)

/**
 * Iterates through search results.
 */
@Search
interface SearchIterator : SuspendingCloseable {

    /**
     * Number of matches for this search, if known.
     *
     * Depending on the search algorithm, it may not be possible to know the result count until
     * reaching the end of the publication.
     *
     * The count might be updated after each call to [next].
     */
    val resultCount: Int? get() = null

    /**
     * Retrieves the next page of results.
     *
     * @return Null when reaching the end of the publication, or an error in case of failure.
     */
    suspend fun next(): SearchTry<LocatorCollection?>

    /**
     * Closes any resources allocated for the search query, such as a cursor.
     * To be called when the user dismisses the search.
     */
    override suspend fun close() {}

    /**
     * Performs the given operation on each result page of this [SearchIterator].
     */
    suspend fun forEach(action: (LocatorCollection) -> Unit): SearchTry<Unit> {
        while (true) {
            val res = next()
            res
                .onSuccess { locators ->
                    if (locators != null) {
                        action(locators)
                    } else {
                        return Try.success(Unit)
                    }
                }
                .onFailure {
                    return Try.failure(it)
                }
        }
    }
}
