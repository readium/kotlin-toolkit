/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationAudioRef
import org.readium.r2.shared.guided.GuidedNavigationText
import org.readium.r2.shared.guided.GuidedNavigationTextRef
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.TemporalFragmentParser
import org.readium.r2.shared.util.TimeInterval
import org.readium.r2.shared.util.Url

internal sealed interface ReadAloudSegment {

    val nodes: List<ReadAloudNode>

    val player: PlaybackEngine

    val emptyNodes: Set<ReadAloudNode>
}

internal data class AudioSegment(
    override val player: PlaybackEngine,
    val items: List<AudioChunk>,
    val textRefs: List<Url>,
    override val nodes: List<ReadAloudNode>,
    override val emptyNodes: Set<ReadAloudNode>,
) : ReadAloudSegment

internal data class TtsSegment(
    override val player: PlaybackEngine,
    val items: List<GuidedNavigationText>,
    override val nodes: List<ReadAloudNode>,
    override val emptyNodes: Set<ReadAloudNode>,
) : ReadAloudSegment

internal class ReadAloudSegmentFactory(
    private val audioEngineFactory: (List<AudioChunk>) -> PlaybackEngine?,
    private val ttsEngineFactory: (Language?, List<String>) -> PlaybackEngine?,
) {

    fun createSegmentFromNode(node: ReadAloudNode): ReadAloudSegment? =
        createAudioSegmentFromNode(node)
            ?: createTtsSegmentFromNode(node)

    private fun createAudioSegmentFromNode(
        firstNode: ReadAloudNode,
    ): AudioSegment? {
        var nextNode: ReadAloudNode? = firstNode
        val audioChunks = mutableListOf<AudioChunk>()
        val textRefs = mutableListOf<Url>()
        val nodes = mutableListOf<ReadAloudNode>()
        val emptyNodes = mutableSetOf<ReadAloudNode>()

        while (nextNode != null && nextNode.content !is TextContent) {
            val audioContent = (nextNode.content as? AudioContent)

            if (audioContent != null) {
                audioChunks.add(audioContent.toAudioItem())
                nodes.add(nextNode)
                textRefs.add(audioContent.textRef)
            } else {
                emptyNodes.add(nextNode)
            }

            nextNode = nextNode.next()
        }

        if (audioChunks.isEmpty()) {
            return null
        }

        val audioEngine = audioEngineFactory(audioChunks)
            ?: return null

        return AudioSegment(
            player = audioEngine,
            items = audioChunks,
            textRefs = textRefs,
            nodes = nodes,
            emptyNodes = emptyNodes
        )
    }

    private fun createTtsSegmentFromNode(
        firstNode: ReadAloudNode,
    ): TtsSegment? {
        var nextNode: ReadAloudNode? = firstNode
        val segmentLanguage = firstNode.text?.language
        val textItems = mutableListOf<GuidedNavigationText>()
        val nodes = mutableListOf<ReadAloudNode>()
        val emptyNodes = mutableSetOf<ReadAloudNode>()

        while (
            nextNode != null &&
            nextNode.content !is AudioContent &&
            nextNode.language == segmentLanguage
        ) {
            val textContent = (nextNode.content as? TextContent)

            if (textContent != null) {
                textItems.add(textContent.text)
                nodes.add(nextNode)
            } else {
                emptyNodes.add(nextNode)
            }

            nextNode = nextNode.next()
        }

        if (textItems.isEmpty()) {
            return null
        }

        val utterances = textItems.map { it.plain!! }

        val ttsPlayer = ttsEngineFactory(segmentLanguage, utterances)
            ?: return null

        return TtsSegment(
            player = ttsPlayer,
            items = textItems,
            nodes = nodes,
            emptyNodes = emptyNodes
        )
    }

    private val ReadAloudNode.language: Language? get() = when (content) {
        is TextContent -> text?.language
        else -> null
    }

    private val ReadAloudNode.content: NodeContent? get() {
        refs
            .firstNotNullOfOrNull { it as? GuidedNavigationAudioRef }
            ?.let { audioRef ->

                val textRef = refs.firstNotNullOfOrNull { it as? GuidedNavigationTextRef }

                // Ignore audio nodes without textref. Might be changed later.
                textRef?.let {
                    return AudioContent(
                        href = audioRef.url.removeFragment(),
                        interval = audioRef.url.timeInterval,
                        textRef = it.url
                    )
                }
            }

        text
            ?.let {
                return TextContent(it)
            }

        return null
    }
}

private sealed interface NodeContent

private data class AudioContent(
    val href: Url,
    val interval: TimeInterval?,
    val textRef: Url,
) : NodeContent

private data class TextContent(
    val text: GuidedNavigationText,
) : NodeContent

private fun AudioContent.toAudioItem(): AudioChunk {
    return AudioChunk(
        href = href,
        interval = interval
    )
}

private val Url.timeInterval get() = fragment
    ?.let { TemporalFragmentParser.parse(it) }
