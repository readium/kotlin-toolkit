@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package org.readium.navigator.media.tts

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.content.DefaultContentService
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.StringResource
import org.readium.r2.shared.util.tokenizer.DefaultTextContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextTokenizer
import org.readium.r2.shared.util.tokenizer.TextUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class TtsUtteranceIteratorTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class TestSettings(
        override val language: Language? = null,
        override val overrideContentLanguage: Boolean = false,
    ) : TtsEngine.Settings

    private data class TestPreferences(override val language: Language? = null) : TtsEngine.Preferences<TestPreferences> {
        override fun plus(other: TestPreferences): TestPreferences = other
    }

    private object TestError : TtsEngine.Error {
        override val message: String = "Test failure"
        override val cause: org.readium.r2.shared.util.Error? = null
    }

    private class TestVoice : TtsEngine.Voice {
        override val language: Language = Language("en")
    }

    private class TestEngine : TtsEngine<TestSettings, TestPreferences, TestError, TestVoice> {
        override val settings = MutableStateFlow(TestSettings())
        override val voices: Set<TestVoice> = emptySet()
        val spoken = mutableListOf<String>()
        val speechRequests = MutableStateFlow(0)

        override fun submitPreferences(preferences: TestPreferences) {}
        override fun speak(requestId: TtsEngine.RequestId, text: String, language: Language?) {
            spoken.add(text.trim())
            speechRequests.value = spoken.size
        }
        override fun stop() {}
        override fun setListener(listener: TtsEngine.Listener<TestError>?) {}
        override fun close() {}
    }
    private val href = Url("article.xhtml")!!
    private val selector = "body > article:nth-of-type(1) > p:nth-of-type(3)"
    private val text = "First sentence. Second sentence. Third sentence."
    private val html = """
        <html lang="en"><body><article>
          <p>Earlier.</p><p>Other.</p><p>$text</p>
        </article></body></html>
    """.trimIndent()

    private fun publication(html: String = this.html): Publication = publication(mapOf(href to html))

    private fun publication(resources: Map<Url, String>, onContent: () -> Unit = {}): Publication {
        val links = resources.keys.map { Link(href = it, mediaType = MediaType.XHTML) }
        val contentFactory = DefaultContentService.createFactory(listOf(HtmlResourceContentIterator.Factory()))
        val container = object : Container<Resource> {
            override val entries = resources.keys
            override fun get(url: Url): Resource? = resources[url]?.let(::StringResource)
            override fun close() {}
        }
        return Publication(
            manifest = Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Article")),
                readingOrder = links,
            ),
            container = container,
            servicesBuilder = Publication.ServicesBuilder(
                content = { context ->
                    val delegate = contentFactory(context)
                    object : ContentService {
                        override fun content(start: Locator?): Content {
                            onContent()
                            return delegate.content(start)
                        }
                    }
                },
            ),
        )
    }

    private fun locator(highlight: String? = null, selector: String = this.selector): Locator =
        Locator(
            href = href,
            mediaType = MediaType.XHTML,
            locations = Locator.Locations(otherLocations = mapOf("cssSelector" to selector)),
            text = Locator.Text(highlight = highlight),
        )

    private fun tapLocator(
        highlight: String?,
        offset: Int?,
        pointSelector: String = selector,
        textNodeIndex: Int = 0,
    ): Locator =
        locator(highlight).copy(
            locations = Locator.Locations(
                otherLocations = mapOf(
                    "cssSelector" to selector,
                    "domRange" to mapOf(
                        "start" to buildMap {
                            put("cssSelector", pointSelector)
                            put("textNodeIndex", textNodeIndex)
                            offset?.let { put("charOffset", it) }
                        }
                    ),
                ),
            ),
        )

    private fun iterator(publication: Publication, locator: Locator) = TtsUtteranceIterator(
        publication = publication,
        tokenizerFactory = { DefaultTextContentTokenizer(TextUnit.Sentence, it) },
        initialLocator = locator,
    )

    @Test
    fun `GIVEN the first sentence of a paragraph WHEN seeking THEN it starts there`() = runTest {
        val publication = publication()
        val iterator = iterator(publication, tapLocator("First sentence.", 0))

        assertEquals("First sentence.", iterator.next()?.utterance?.trim())
        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN a sentence after br WHEN seeking THEN it starts in the second fragment`() = runTest {
        val publication = publication(html.replace(text, "First sentence.<br>Second sentence. Third sentence."))
        val iterator = iterator(publication, tapLocator("Second sentence.", 0, textNodeIndex = 2))

        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("First sentence.", iterator.previous()?.utterance?.trim())
        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("Third sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN an omitted offset after an inline element WHEN seeking THEN it uses the direct child text node`() = runTest {
        val publication = publication(html.replace(text, "First sentence. <em>Other. </em>Second sentence. Third sentence."))
        val iterator = iterator(publication, tapLocator(null, null, textNodeIndex = 2))

        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("Third sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN a DOM range without a top level CSS selector WHEN seeking THEN it reaches the target paragraph`() = runTest {
        val publication = publication(
            html.replace("<p>Other.</p>", "<p>Second sentence.</p>")
                .replace(text, "First sentence. <span id=\"sentence\">Second sentence.</span> Third sentence.")
        )
        val tap = tapLocator("Second sentence.", 0, pointSelector = "#sentence")
        val locator = tap.copy(
            locations = Locator.Locations(
                otherLocations = mapOf(
                    "domRange" to tap.locations.otherLocations.getValue("domRange"),
                )
            )
        )

        val iterator = iterator(publication, locator)
        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("First sentence.", iterator.previous()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN an invalid DOM point WHEN seeking THEN the original article position is used`() = runTest {
        val lastHref = Url("last.xhtml")!!
        val publication = publication(
            mapOf(
                href to html.replace(text, "First sentence.<br>Second sentence."),
                lastHref to "<html><body><p>Next chapter.</p></body></html>",
            )
        )
        val tap = tapLocator("Absent.", offset = 100, textNodeIndex = 2)
        val locator = tap.copy(
            locations = Locator.Locations(
                otherLocations = mapOf(
                    "cssSelector" to "body > article",
                    "domRange" to tap.locations.otherLocations.getValue("domRange"),
                )
            )
        )
        val iterator = iterator(publication, locator)

        assertEquals("Other.", iterator.next()?.utterance?.trim())
        assertEquals("First sentence.", iterator.next()?.utterance?.trim())
        assertEquals(0, iterator.next()?.resourceIndex)
        publication.close()
    }

    @Test
    fun `GIVEN punctuation-only language segments WHEN seeking THEN the range selects the emitted utterance`() = runTest {
        val publication = publication(html.replace(text, "Start. <span lang=\"sv\">!!!</span><span lang=\"en\">Target. End.</span>"))
        val iterator = TtsUtteranceIterator(
            publication = publication,
            tokenizerFactory = { TextTokenizer { source -> listOf(0 until source.length) } },
            initialLocator = tapLocator("Target. End.", 0, "$selector > span:nth-of-type(2)"),
        )

        assertEquals("Target. End.", iterator.next()?.utterance?.trim())
        assertEquals("Start.", iterator.previous()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN a text highlight without a DOM range WHEN seeking THEN it remains at the paragraph start`() = runTest {
        val publication = publication(html.replace(text, "First sentence. Second  sentence. Third sentence."))
        val iterator = iterator(publication, locator("Second  sentence. "))

        assertEquals("First sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN identical sentences WHEN tapping the second THEN seek picks the second occurrence`() = runTest {
        val publication = publication(html.replace(text, "Again. Again. End."))
        val iterator = iterator(publication, tapLocator("Again.", offset = 7))

        val utterance = iterator.next()
        assertEquals("Again.", utterance?.utterance?.trim())
        assertEquals("Again. ", utterance?.text?.before)
        publication.close()
    }

    @Test
    fun `GIVEN a DOM offset inside the sentence WHEN seeking THEN it selects that utterance`() = runTest {
        val publication = publication()
        val iterator = iterator(publication, tapLocator(highlight = null, offset = 23))

        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("Third sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN leading whitespace and inline formatting WHEN seeking THEN it starts at the containing sentence`() = runTest {
        val publication = publication(html.replace(text, "First sentence.  <em>  Second</em> sentence. Third sentence."))
        val iterator = iterator(publication, tapLocator("Second sentence.", 2, "$selector > em"))

        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("Third sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN an inline space with no adjacent whitespace WHEN seeking THEN the DOM point selects the emitted sentence`() = runTest {
        val tail = "sentence. Second sentence."
        val publication = publication(html.replace(text, "First<span> </span>$tail"))
        val iterator = iterator(publication, tapLocator(null, tail.indexOf("Second"), textNodeIndex = 2))

        assertEquals("Second sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN nbsp and supplementary characters around a DOM point WHEN seeking THEN UTF16 offsets select the correct sentence`() = runTest {
        val rendered = "First\u00a0&copy; 😀. 😀 Second sentence. Last sentence."
        val publication = publication(html.replace(text, "First&nbsp;&amp;copy; 😀. 😀 Second sentence. Last sentence."))
        val iterator = iterator(publication, tapLocator(null, rendered.indexOf("Second sentence.") + 8))

        assertEquals("😀 Second sentence.", iterator.next()?.utterance?.trim())
        assertEquals("Last sentence.", iterator.next()?.utterance?.trim())
        publication.close()
    }

    @Test
    fun `GIVEN the first sentence of a paragraph WHEN seeking THEN previous and next cross its boundary`() = runTest(dispatcher) {
        val publication = publication(html.replace(text, "Again. End.").replace("<p>Other.</p>", "<p>Previous.</p>"))
        val player = TtsPlayer(TestEngine(), iterator(publication, tapLocator("Again.", 0)), TestPreferences())!!

        assertEquals("Again.", player.utterance.value.text.trim())
        assertTrue(player.hasPreviousUtterance())
        player.previousUtterance()
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "Previous." } }
        }
        player.nextUtterance()
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "Again." } }
        }
        player.nextUtterance()
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "End." } }
        }

        player.close()
        publication.close()
    }

    @Test
    fun `GIVEN the last sentence of a paragraph WHEN seeking THEN previous and next restore its position`() = runTest(dispatcher) {
        val publication = publication()
        val player = TtsPlayer(TestEngine(), iterator(publication, tapLocator("Third sentence.", 33)), TestPreferences())!!

        assertEquals("Third sentence.", player.utterance.value.text.trim())
        player.previousUtterance()
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "Second sentence." } }
        }
        player.nextUtterance()
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "Third sentence." } }
        }
        assertEquals("Third sentence.", player.utterance.value.text.trim())
        player.close()
        publication.close()
    }

    @Test
    fun `GIVEN a paused player WHEN seeking THEN its published position changes before playing`() = runTest(dispatcher) {
        val publication = publication()
        val player = TtsPlayer(TestEngine(), iterator(publication, locator()), TestPreferences())!!

        player.go(tapLocator("Third sentence.", 33))
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "Third sentence." } }
        }

        assertEquals("Third sentence.", player.utterance.value.text.trim())
        player.close()
        publication.close()
    }

    @Test
    fun `GIVEN a playing player WHEN seeking THEN it speaks the target and skips earlier sentences`() = runTest(dispatcher) {
        val publication = publication()
        val engine = TestEngine()
        val player = TtsPlayer(engine, iterator(publication, locator()), TestPreferences())!!
        player.play()
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { engine.speechRequests.first { it == 1 } }
        }

        player.go(tapLocator("Third sentence.", 33))
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { player.utterance.first { it.text.trim() == "Third sentence." } }
        }
        withContext(Dispatchers.IO) {
            withTimeout(10.seconds) { engine.speechRequests.first { it == 2 } }
        }

        assertEquals(listOf("First sentence.", "Third sentence."), engine.spoken)
        assertEquals("Third sentence.", player.utterance.value.text.trim())
        player.close()
        publication.close()
    }

    @Test
    fun `GIVEN cancellation during sentence selection WHEN seeking THEN it does not restart fallback`() = runTest {
        var contentRequests = 0
        val publication = publication(mapOf(href to html)) { contentRequests++ }
        val seekJob = Job()
        var tokenizations = 0
        val iterator = TtsUtteranceIterator(
            publication = publication,
            tokenizerFactory = {
                TextTokenizer {
                    tokenizations++
                    seekJob.cancel()
                    emptyList()
                }
            },
            initialLocator = tapLocator(null, 23),
        )

        try {
            withContext(seekJob) { iterator.next() }
            fail("Seek should be cancelled")
        } catch (_: CancellationException) {
            assertEquals(1, tokenizations)
            assertEquals(1, contentRequests)
        } finally {
            publication.close()
        }
    }
}
