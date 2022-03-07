package org.readium.r2.shared.publication.services.iterator

import org.readium.r2.shared.Search
import org.readium.r2.shared.fetcher.DefaultResourceContentExtractorFactory
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceContentExtractor
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.*
import java.util.*

typealias TextIteratorTry<SuccessT> = Try<SuccessT, TextIteratorException>

/**
 * Represents an error which might occur while iterating through a publication's content.
 */
sealed class TextIteratorException private constructor(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    object PublicationNotIterable : TextIteratorException("The content of this publication cannot be iterated through")
    class PublicationUnavailable(message: String) : TextIteratorException(message)
    class UnsupportedOption(message: String) : TextIteratorException(message)
    class UnsupportedResource(message: String, val link: Link) : TextIteratorException(message)
    class ResourceError(message: String, val link: Link, cause: Resource.Exception) : TextIteratorException(message, cause)
}

data class Text(
    val text: String,
    val locator: Locator
)

enum class TextUnit {
    Character, Word, Sentence, Paragraph
}

interface TextIteratorService : Publication.Service {
    suspend fun iterator(unit: TextUnit, start: Locator?, locale: Locale?): TextIteratorTry<TextIterator>
}

/**
 * Iterates through a publication's content.
 */
interface TextIterator : SuspendingCloseable {

    /**
     * Retrieves the next piece of content.
     *
     * @return Null when reaching the end of the publication, or an error in case of failure.
     */
    suspend fun next(): TextIteratorTry<Text?>

    /**
     * Closes any resources allocated for the search query, such as a cursor.
     * To be called when the user dismisses the search.
     */
    override suspend fun close() {}
}

suspend fun Publication.textIterator(unit: TextUnit, start: Locator? = null, locale: Locale? = null): TextIteratorTry<TextIterator> =
    findService(TextIteratorService::class)?.iterator(unit, start, locale)
        ?: Try.failure(TextIteratorException.PublicationNotIterable)

/** Factory to build a [TextIteratorService] */
var Publication.ServicesBuilder.textIteratorServiceFactory: ServiceFactory?
    get() = get(TextIteratorService::class)
    set(value) = set(TextIteratorService::class, value)

@OptIn(Search::class)
class DefaultTextIteratorService(
    private val publication: Ref<Publication>
) : TextIteratorService {
    companion object {
        fun createFactory(): (Publication.Service.Context) -> DefaultTextIteratorService = { context ->
            DefaultTextIteratorService(publication = context.publication)
        }
    }

    override suspend fun iterator(
        unit: TextUnit,
        start: Locator?,
        locale: Locale?,
    ): TextIteratorTry<TextIterator> {
        val publication = publication.ref
            ?: return Try.failure(TextIteratorException.PublicationUnavailable("The publication is null"))

        return Try.Success(DefaultTextIterator(
            publication = publication,
            start = start,
            contentExtractorFactory = DefaultResourceContentExtractorFactory(),
            tokenizer = unitTextContentTokenizer(
                unit = unit,
                locale = locale ?: publication.metadata.locale,
            ),
        ))
    }
}

@OptIn(Search::class)
internal class DefaultTextIterator(
    private val publication: Publication,
    start: Locator?,
    private val contentExtractorFactory: ResourceContentExtractor.Factory,
    private val tokenizer: Tokenizer,
) : TextIterator {

    private var nextIndex =
        start?.let { publication.readingOrder.indexOfFirstWithHref(it.href) }
            ?: 0

    private var tokens = mutableListOf<Text>()

    override suspend fun next(): TextIteratorTry<Text?> {
        tokens.removeFirstOrNull()
            ?.let { return Try.success(it) }

        return tokenizeNextResource()
            .flatMap { endReached ->
                if (endReached) {
                    Try.success(null)
                } else {
                    next()
                }
            }
    }

    private suspend fun tokenizeNextResource(): TextIteratorTry<Boolean> {
        if (nextIndex >= publication.readingOrder.count()) {
            return Try.success(true)
        }

        val link = publication.readingOrder[nextIndex]

        val locator = publication.locatorFromLink(link)
            ?: return Try.failure(TextIteratorException.UnsupportedResource(
                "Failed to create a Locator from this link", link
            ))

        val text = publication.get(link).use { resource ->
            val extractor = contentExtractorFactory.createExtractor(resource)
                ?: return Try.failure(TextIteratorException.UnsupportedResource(
                    "Cannot extract the text out of this resource", link
                ))

            extractor.extractText(resource)
                .mapFailure {
                    TextIteratorException.ResourceError("Failed to extract the text out of this resource", link, it)
                }
        }

        return text
            .flatMap { tokenizer.tokenize(it) }
            .map { tokens ->
                this.tokens = tokens
                    .map { tokenText ->
                        Text(
                            text = tokenText.highlight ?: "",
                            locator = locator.copy(text = tokenText, locations = Locator.Locations())
                        )
                    }
                    .toMutableList()

                nextIndex += 1
                false
            }
    }
}

