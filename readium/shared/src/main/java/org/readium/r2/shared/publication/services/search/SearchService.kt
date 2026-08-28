/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError

@ExperimentalReadiumApi
public typealias SearchTry<SuccessT> = Try<SuccessT, SearchError>

/**
 * Represents an error which might occur during a search activity.
 */
@ExperimentalReadiumApi
public sealed class SearchError(
    override val message: String,
    override val cause: Error? = null,
) : Error {

    /**
     * An error occurred while accessing one of the publication's resources.
     */
    public class Reading(override val cause: ReadError) :
        SearchError(
            "An error occurred while accessing one of the publication's resources.",
            cause
        )

    /**
     * An error occurring in the search engine.
     */
    public class Engine(cause: Error) :
        SearchError("An error occurred while searching.", cause)
}

/**
 * Provides a way to search terms in a publication.
 */
@ExperimentalReadiumApi
public interface SearchService : Publication.Service {

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
    public data class Options(
        val caseSensitive: Boolean? = null,
        val diacriticSensitive: Boolean? = null,
        val wholeWord: Boolean? = null,
        val exact: Boolean? = null,
        val language: String? = null,
        val regularExpression: Boolean? = null,
        val otherOptions: Map<String, String> = emptyMap(),
    ) : Parcelable {
        /**
         * Syntactic sugar to access the [otherOptions] values by subscripting [Options] directly.
         */
        public operator fun get(key: String): String? = otherOptions[key]
    }

    /**
     * Default value for the search options of this service.
     *
     * If an option does not have a value, it is not supported by the service.
     */
    public val options: Options

    /**
     * Starts a new search through the publication content, with the given [query].
     *
     * If an option is nil when calling search(), its value is assumed to be the default one.
     */
    public suspend fun search(query: String, options: Options? = null): SearchIterator
}

/**
 * Indicates whether the content of this publication can be searched.
 */
@ExperimentalReadiumApi
public val Publication.isSearchable: Boolean
    get() = findService(SearchService::class) != null

/**
 * Default value for the search options of this publication.
 */
@ExperimentalReadiumApi
public val Publication.searchOptions: SearchService.Options get() =
    findService(SearchService::class)?.options ?: SearchService.Options()

/**
 * Starts a new search through the publication content, with the given [query].
 *
 * If an option is nil when calling [search], its value is assumed to be the default one for the
 * search service.
 *
 * Returns null if the publication is not searchable.
 */
@ExperimentalReadiumApi
public suspend fun Publication.search(query: String, options: SearchService.Options? = null): SearchIterator? =
    findService(SearchService::class)?.search(query, options)

/** Factory to build a [SearchService] */
@ExperimentalReadiumApi
public var Publication.ServicesBuilder.searchServiceFactory: ServiceFactory?
    get() = get(SearchService::class)
    set(value) = set(SearchService::class, value)

/**
 * Iterates through search results.
 */
@ExperimentalReadiumApi
public interface SearchIterator : Closeable {

    /**
     * Number of matches for this search, if known.
     *
     * Depending on the search algorithm, it may not be possible to know the result count until
     * reaching the end of the publication.
     *
     * The count might be updated after each call to [next].
     */
    public val resultCount: Int? get() = null

    /**
     * Retrieves the next page of results.
     *
     * @return Null when reaching the end of the publication, or an error in case of failure.
     */
    public suspend fun next(): SearchTry<LocatorCollection?>

    /**
     * Closes any resources allocated for the search query, such as a cursor.
     * To be called when the user dismisses the search.
     */
    override fun close() {}

    /**
     * Performs the given operation on each result page of this [SearchIterator].
     */
    public suspend fun forEach(action: (LocatorCollection) -> Unit): SearchTry<Unit> {
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
