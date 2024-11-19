/*
 * Copyight 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.icu.text.BreakIterator
import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import android.icu.text.StringSearch
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.StringCharacterIterator
import java.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.publication.services.search.SearchService.Options
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.content.DefaultResourceContentExtractorFactory
import org.readium.r2.shared.util.resource.content.ResourceContentExtractor
import timber.log.Timber

/**
 * Base implementation of [SearchService] iterating through the content of Publication's
 * resources.
 *
 * To stay media-type-agnostic, [StringSearchService] relies on [ResourceContentExtractor]
 * implementations to retrieve the pure text content from markups (e.g. HTML) or binary (e.g. PDF)
 * resources.
 *
 * The actual search is implemented by the provided [searchAlgorithm].
 */
@ExperimentalReadiumApi
public class StringSearchService(
    private val manifest: Manifest,
    private val container: Container<Resource>,
    private val services: PublicationServicesHolder,
    private val language: String?,
    private val snippetLength: Int,
    private val searchAlgorithm: Algorithm,
    private val extractorFactory: ResourceContentExtractor.Factory,
) : SearchService {

    public companion object {
        public fun createDefaultFactory(
            snippetLength: Int = 200,
            searchAlgorithm: Algorithm? = null,
            extractorFactory: ResourceContentExtractor.Factory = DefaultResourceContentExtractorFactory(),
        ): (Publication.Service.Context) -> StringSearchService =
            { context ->
                StringSearchService(
                    manifest = context.manifest,
                    container = context.container,
                    services = context.services,
                    language = context.manifest.metadata.languages.firstOrNull(),
                    snippetLength = snippetLength,
                    searchAlgorithm = searchAlgorithm
                        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) IcuAlgorithm() else NaiveAlgorithm(),
                    extractorFactory = extractorFactory
                )
            }
    }

    private val locale: Locale = language?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()

    override val options: Options = searchAlgorithm.options
        .copy(language = locale.toLanguageTag())

    override suspend fun search(query: String, options: Options?): SearchIterator =
        Iterator(
            manifest = manifest,
            container = container,
            query = query,
            options = options ?: Options(),
            locale = options?.language?.let { Locale.forLanguageTag(it) } ?: locale
        )

    private inner class Iterator(
        val manifest: Manifest,
        val container: Container<Resource>,
        val query: String,
        val options: Options,
        val locale: Locale,
    ) : SearchIterator {

        override var resultCount: Int = 0
            private set

        /**
         * Index of the last reading order resource searched in.
         */
        private var index = -1

        override suspend fun next(): SearchTry<LocatorCollection?> {
            try {
                if (index >= manifest.readingOrder.count() - 1) {
                    return Try.success(null)
                }

                index += 1

                val link = manifest.readingOrder[index]
                val mediaType = link.mediaType ?: return next()
                val text =
                    container[link.url()]
                        ?.let { extractorFactory.createExtractor(it, mediaType)?.extractText(it) }
                        ?.getOrElse { return Try.failure(SearchError.Reading(it)) }

                if (text == null) {
                    Timber.w("Cannot extract text from resource: ${link.href}")
                    return next()
                }

                val locators = findLocators(index, link, text)
                resultCount += locators.count()

                // If no occurrences were found in the current resource, skip to the next one
                // automatically.
                if (locators.isEmpty()) {
                    return next()
                }

                return Try.success(LocatorCollection(locators = locators))
            } catch (
                e: CancellationException,
            ) {
                throw e
            } catch (e: Exception) {
                return Try.failure(SearchError.Engine(ThrowableError(e)))
            }
        }

        private suspend fun findLocators(resourceIndex: Int, link: Link, text: String): List<Locator> {
            if (text == "") {
                return emptyList()
            }

            val resourceTitle = manifest.tableOfContents.titleMatching(link.url())
            var resourceLocator = manifest.locatorFromLink(link) ?: return emptyList()
            resourceLocator = resourceLocator.copy(title = resourceTitle ?: resourceLocator.title)
            val locators = mutableListOf<Locator>()

            withContext(Dispatchers.IO) {
                for (range in searchAlgorithm.findRanges(
                    query = query,
                    options = options,
                    text = text,
                    locale = locale
                )) {
                    locators.add(createLocator(resourceIndex, resourceLocator, text, range))
                }
            }

            return locators
        }

        private suspend fun createLocator(
            resourceIndex: Int,
            resourceLocator: Locator,
            text: String,
            range: IntRange,
        ): Locator {
            val progression = range.first.toDouble() / text.length.toDouble()

            var totalProgression: Double? = null
            val positions = positions()
            val resourceStartTotalProg = positions.getOrNull(resourceIndex)?.firstOrNull()?.locations?.totalProgression
            if (resourceStartTotalProg != null) {
                val resourceEndTotalProg = positions.getOrNull(resourceIndex + 1)?.firstOrNull()?.locations?.totalProgression ?: 1.0
                totalProgression = resourceStartTotalProg + progression * (resourceEndTotalProg - resourceStartTotalProg)
            }

            return resourceLocator.copy(
                locations = resourceLocator.locations.copy(
                    progression = progression,
                    totalProgression = totalProgression
                ),
                text = createSnippet(text, range)
            )
        }

        /**
         * Extracts a snippet from the given [text] at the provided highlight [range].
         *
         * Makes sure that words are not cut off at the boundaries.
         */
        private fun createSnippet(text: String, range: IntRange): Locator.Text {
            val iter = StringCharacterIterator(text)

            var before = ""
            iter.index = range.first
            var char = iter.previous()
            var count = snippetLength
            while (char != StringCharacterIterator.DONE && (count >= 0 || !char.isWhitespace())) {
                before = char + before
                count--
                char = iter.previous()
            }

            var after = ""
            iter.index = range.last
            char = iter.next()
            count = snippetLength
            while (char != StringCharacterIterator.DONE && (count >= 0 || !char.isWhitespace())) {
                after += char
                count--
                char = iter.next()
            }

            return Locator.Text(
                highlight = text.substring(range),
                before = before,
                after = after
            )
        }

        private lateinit var _positions: List<List<Locator>>
        private suspend fun positions(): List<List<Locator>> {
            if (!::_positions.isInitialized) {
                _positions = services.positionsByReadingOrder()
            }
            return _positions
        }
    }

    /** Implements the actual search algorithm in sanitized text content. */
    public interface Algorithm {

        /**
         * Default value for the search options available with this algorithm.
         * If an option does not have a value, it is not supported by the algorithm.
         */
        public val options: Options

        /**
         * Finds all the ranges of occurrences of the given [query] in the [text].
         */
        public suspend fun findRanges(query: String, options: Options, text: String, locale: Locale): List<IntRange>
    }

    /**
     * Implementation of a search [Algorithm] using ICU components to perform the actual search
     * while taking into account languages specificities.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public class IcuAlgorithm : Algorithm {

        override val options: Options = Options(
            caseSensitive = false,
            diacriticSensitive = false,
            wholeWord = false
        )

        override suspend fun findRanges(
            query: String,
            options: Options,
            text: String,
            locale: Locale,
        ): List<IntRange> {
            val ranges = mutableListOf<IntRange>()
            val iter = createStringSearch(query, options, text, locale)
            var start = iter.first()
            while (start != android.icu.text.SearchIterator.DONE) {
                ranges.add(start until (start + iter.matchLength))
                start = iter.next()
            }
            return ranges
        }

        private fun createStringSearch(
            query: String,
            options: Options,
            text: String,
            locale: Locale,
        ): StringSearch {
            val caseSensitive = options.caseSensitive ?: false
            var diacriticSensitive = options.diacriticSensitive ?: false
            val wholeWord = options.wholeWord ?: false

            // Because of an issue (see FIXME below), we can't have case sensitivity without also
            // enabling diacritic sensitivity.
            diacriticSensitive = diacriticSensitive || caseSensitive

            // http://userguide.icu-project.org/collation/customization
            // ignore diacritics and case = primary strength
            // ignore diacritics = primary strength + caseLevel on
            // ignore case = secondary strength
            val collator = Collator.getInstance(locale) as RuleBasedCollator
            if (!diacriticSensitive) {
                collator.strength = Collator.PRIMARY
                // if (caseSensitive) {
                // FIXME: This doesn't seem to work despite the documentation indicating:
                // > To ignore accents but take cases into account, set strength to primary and case level to on.
                // > http://userguide.icu-project.org/collation/customization
                // collator.isCaseLevel = true
                // }
            } else if (!caseSensitive) {
                collator.strength = Collator.SECONDARY
            }

            val breakIterator: BreakIterator? =
                if (wholeWord) {
                    BreakIterator.getWordInstance()
                } else {
                    null
                }

            return StringSearch(query, StringCharacterIterator(text), collator, breakIterator)
        }
    }

    /**
     * A naive search [Algorithm] performing exact matches on strings.
     *
     * There are no safe ways to perform case insensitive search using [String.indexOf] with
     * all languages, so this [Algorithm] does not have any options. Use [IcuAlgorithm] for
     * better results.
     */
    public class NaiveAlgorithm : Algorithm {

        override val options: Options get() = Options()

        override suspend fun findRanges(
            query: String,
            options: Options,
            text: String,
            locale: Locale,
        ): List<IntRange> {
            val ranges = mutableListOf<IntRange>()
            var index: Int = text.indexOf(query)
            while (index >= 0) {
                ranges.add(index until (index + query.length))
                index = text.indexOf(query, index + 1)
            }
            return ranges
        }
    }
}

private fun List<Link>.titleMatching(href: Url): String? {
    for (link in this) {
        link.titleMatching(href)?.let { return it }
    }
    return null
}

private fun Link.titleMatching(targetHref: Url): String? {
    if (url().removeFragment().isEquivalent(targetHref)) {
        return title
    }
    return children.titleMatching(targetHref)
}
