package org.readium.r2.shared.publication.services.iterator

import android.icu.text.BreakIterator
import android.os.Build
import androidx.annotation.RequiresApi
import org.readium.r2.shared.publication.Locator
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
                val before = content.substring((start - contextLength).coerceAtLeast(0), start)
                var highlight = content.substring(start, end)
                var after =
                    content.substring(end, (end + contextLength).coerceAtMost(content.length))

                start = end
                end = iter.next()

                if (
                    unit != TextContentUnit.Character &&
                    (
                            highlight.isBlank() ||
                                    highlight.find { it.isLetterOrDigit() } == null
                            )
                ) {
                    continue
                }

                if (unit == TextContentUnit.Sentence) {
                    val origHighlight = highlight
                    highlight = origHighlight.trimEnd()
                    val whitespaceSuffix = origHighlight.removePrefix(highlight)
                    after = whitespaceSuffix + after
                }

                tokens.add(
                    Locator.Text(
                        highlight = highlight,
                        before = before,
                        after = after
                    )
                )
            }

            tokens
        }
}
