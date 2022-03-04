package org.readium.r2.shared.publication.services.iterator

import android.icu.text.BreakIterator
import android.os.Build
import androidx.annotation.RequiresApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.StringSearchService.Algorithm
import org.readium.r2.shared.publication.services.search.StringSearchService.IcuAlgorithm
import org.readium.r2.shared.util.Try
import java.util.*

data class TextContentOptions(
    val tokenizer: TextContentTokenizer,
)

data class TextContent(
    val text: String,
    val locator: Locator
) : Content

interface TextContentTokenizer {
    suspend fun tokenize(content: String): ContentIteratorTry<List<Locator.Text>>
}

enum class TextContentUnit {
    Character, Word, Sentence, Paragraph
}

/**
 * Implementation of a [TextContentTokenizer] using ICU components to perform the actual
 * tokenization while taking into account languages specificities.
 */
@RequiresApi(Build.VERSION_CODES.N)
class IcuUnitTextContentTokenizer(
    locale: Locale,
    private val unit: TextContentUnit,
    private val contextLength: Int = 200,
) : TextContentTokenizer {
    private val breakIterator: ContentIteratorTry<BreakIterator> by lazy {
        when (unit) {
            TextContentUnit.Character ->
                Try.success(BreakIterator.getCharacterInstance(locale))
            TextContentUnit.Word ->
                Try.success(BreakIterator.getWordInstance(locale))
            TextContentUnit.Sentence ->
                Try.success(BreakIterator.getSentenceInstance(locale))
            TextContentUnit.Paragraph ->
                Try.failure(ContentIteratorException.UnsupportedOption("IcuUnitTextContentTokenizer does not handle TextContentUnit.Paragraph"))
        }
    }

    override suspend fun tokenize(content: String): ContentIteratorTry<List<Locator.Text>> =
        breakIterator.map { iter ->
            iter.setText(content)
            var start: Int = iter.first()
            var end: Int = iter.next()
            val tokens = mutableListOf<Locator.Text>()
            while (end != BreakIterator.DONE) {
                content.extractText(start = start, end = end, contextLength = contextLength, unit = unit)
                    ?.let { tokens.add(it) }

                start = end
                end = iter.next()
            }

            tokens
        }
}

/**
 * A naive [TextContentTokenizer] relying on java.text.BreakIterator to split the content.
 * Use [IcuUnitTextContentTokenizer] for better results.
 */
class NaiveUnitTextContentTokenizer(
    private val unit: TextContentUnit,
    private val contextLength: Int = 200,
) : TextContentTokenizer {
    private val breakIterator: ContentIteratorTry<java.text.BreakIterator> by lazy {
        when (unit) {
            TextContentUnit.Character ->
                Try.success(java.text.BreakIterator.getCharacterInstance())
            TextContentUnit.Word ->
                Try.success(java.text.BreakIterator.getWordInstance())
            TextContentUnit.Sentence ->
                Try.success(java.text.BreakIterator.getSentenceInstance())
            TextContentUnit.Paragraph ->
                Try.failure(ContentIteratorException.UnsupportedOption("NaiveUnitTextContentTokenizer does not handle TextContentUnit.Paragraph"))
        }
    }

    override suspend fun tokenize(content: String): ContentIteratorTry<List<Locator.Text>> =
        breakIterator.map { iter ->
            iter.setText(content)
            var start: Int = iter.first()
            var end: Int = iter.next()
            val tokens = mutableListOf<Locator.Text>()
            while (end != java.text.BreakIterator.DONE) {
                content.extractText(start = start, end = end, contextLength = contextLength, unit = unit)
                    ?.let { tokens.add(it) }

                start = end
                end = iter.next()
            }

            tokens
        }
}

private fun String.extractText(start: Int, end: Int, contextLength: Int, unit: TextContentUnit): Locator.Text? {
    val before = substring((start - contextLength).coerceAtLeast(0), start)
    var highlight = substring(start, end)
    var after = substring(end, (end + contextLength).coerceAtMost(length))

    if (
        unit != TextContentUnit.Character &&
        (
            highlight.isBlank() ||
            highlight.find { it.isLetterOrDigit() } == null
        )
    ) {
        return null
    }

    if (unit == TextContentUnit.Sentence) {
        val origHighlight = highlight
        highlight = origHighlight.trimEnd()
        val whitespaceSuffix = origHighlight.removePrefix(highlight)
        after = whitespaceSuffix + after
    }

    return Locator.Text(
        highlight = highlight,
        before = before,
        after = after
    )
}
