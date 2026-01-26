/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.readaloud

import android.os.Handler
import android.os.Looper
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.TimeOffset
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.readaloud.preferences.ReadAloudSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.guided.GuidedNavigationAudioRef
import org.readium.r2.shared.guided.GuidedNavigationTextRef
import org.readium.r2.shared.util.Error as BaseError
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.TemporalFragmentParser
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType

@ExperimentalReadiumApi
public class ReadAloudNavigator private constructor(
    private val guidedNavigationTree: ReadAloudNode,
    private val resources: List<ReadAloudPublication.Item>,
    private val audioEngineFactory: AudioEngineFactory<BaseError>,
    private val ttsEngineFactory: TtsEngineFactory<TtsVoice, BaseError>,
    initialSettings: ReadAloudSettings,
    initialLocation: ReadAloudLocation?,
) {
    public companion object {

        internal suspend operator fun invoke(
            initialLocation: ReadAloudLocation?,
            initialSettings: ReadAloudSettings,
            publication: ReadAloudPublication,
            audioEngineFactory: AudioEngineFactory<BaseError>,
            ttsEngineFactory: TtsEngineFactory<TtsVoice, BaseError>,
        ): ReadAloudNavigator {
            val tree = withContext(Dispatchers.Default) {
                ReadAloudNode.fromGuidedNavigationObject(publication.guidedNavigationTree)
            }

            return ReadAloudNavigator(
                guidedNavigationTree = tree,
                resources = publication.resources,
                audioEngineFactory = audioEngineFactory,
                ttsEngineFactory = ttsEngineFactory,
                initialSettings = initialSettings,
                initialLocation = initialLocation
            )
        }
    }

    public data class Playback(
        val state: PlaybackState,
        val playWhenReady: Boolean,
        val node: ReadAloudNode,
        private val textItemMediaType: MediaType?,
    ) {
        val nodeHighlightLocation: ReadAloudHighlightLocation? get() {
            val textRef = node.refs.firstNotNullOfOrNull { it as? GuidedNavigationTextRef }
                ?: return null

            val href = textRef.url.removeFragment()
            val cssSelector = textRef.url.fragment
                ?.let { fragment -> CssSelector(fragment.addPrefix("#")) }
            return ReadAloudTextHighlightLocation(
                href = href,
                textQuote = null,
                // mediaType = textItemMediaType,
                cssSelector = cssSelector
            )
        }

        val utteranceHighlightLocation: ReadAloudHighlightLocation? get() {
            return null
        }
    }

    public sealed interface PlaybackState {

        public data object Ready : PlaybackState, MediaNavigator.State.Ready

        public data object Starved : PlaybackState, MediaNavigator.State.Buffering

        public data object Ended : PlaybackState, MediaNavigator.State.Ended

        public data class Failure(val error: Error) : PlaybackState, MediaNavigator.State.Failure
    }

    public sealed class Error(
        override val message: String,
        override val cause: BaseError?,
    ) : BaseError {

        public data class AudioEngineError(override val cause: BaseError) :
            Error("An error occurred in the audio engine.", cause)

        public data class TtsEngineError(override val cause: ReadError) :
            Error("An error occurred in the TTS engine.", cause)
    }

    private inner class PlaybackEngineListener : PlaybackEngine.Listener<PlaybackEngine.Progress, BaseError> {

        private val handler = Handler(Looper.getMainLooper())

        override fun onPlaybackStateChanged(state: PlaybackEngine.PlaybackState) {
            handler.post {
                with(stateMachine) {
                    stateMutable.value = stateMutable.value.onPlaybackEngineStateChanged(state)
                }
            }
        }

        override fun onStartRequested(initialState: PlaybackEngine.PlaybackState) {
            handler.post {
                with(stateMachine) {
                    stateMutable.value = stateMutable.value.onPlaybackEngineStateChanged(initialState)
                }
            }
        }

        override fun onPlaybackCompleted() {
            handler.post {
                with(stateMachine) {
                    stateMutable.value = stateMutable.value.onPlaybackCompleted()
                }
            }
        }

        override fun onPlaybackError(error: BaseError) {
        }

        override fun onPlaybackProgressed(progress: PlaybackEngine.Progress) {
        }
    }

    public var settings: ReadAloudSettings by Delegates.observable(initialSettings) {
            property, oldValue, newValue ->
        with(stateMachine) {
            if (newValue != oldValue) {
                stateMutable.value = stateMutable.value.onSettingsChanged(newValue)
            }
        }
    }

    public val voices: Set<TtsVoice> =
        ttsEngineFactory.voices

    private val segmentFactory = ReadAloudSegmentFactory(
        audioEngineFactory = { chunks: List<AudioChunk> ->
            audioEngineFactory.createPlaybackEngine(
                chunks = chunks,
                listener = PlaybackEngineListener()
            )
        },
        ttsEngineFactory = { language: Language?, utterances: List<String> ->
            val language =
                settings.language
                    .takeIf { settings.overrideContentLanguage }
                    ?: language
                    ?: settings.language

            val preferredVoiceWithRegion =
                settings.voices[language]
                    ?.let { voiceId -> ttsEngineFactory.voices.firstOrNull { it.id == voiceId } }

            val preferredVoiceWithoutRegion =
                language
                    .let { settings.voices[it.removeRegion()] }
                    ?.let { voiceId -> ttsEngineFactory.voices.firstOrNull { it.id == voiceId } }

            val fallbackVoiceWithRegion = ttsEngineFactory.voices
                .firstOrNull { language in it.languages }

            val fallbackVoiceWithoutRegion = ttsEngineFactory.voices
                .firstOrNull { voice -> language.removeRegion() in voice.languages.map { it.removeRegion() } }

            val voice = preferredVoiceWithRegion
                ?: preferredVoiceWithoutRegion
                ?: fallbackVoiceWithRegion
                ?: fallbackVoiceWithoutRegion
                ?: ttsEngineFactory.voices.firstOrNull()

            voice?.let { voice ->
                ttsEngineFactory.createPlaybackEngine(
                    voiceId = voice.id,
                    utterances = utterances,
                    listener = PlaybackEngineListener()
                )
            }
        }
    )

    private val dataLoader = ReadAloudDataLoader(segmentFactory, initialSettings)

    private val navigationHelper = ReadAloudNavigationHelper(initialSettings)

    private val stateMachine = ReadAloudStateMachine(dataLoader, navigationHelper)

    private val initialNode = with(navigationHelper) {
        val nodeFromLocation = initialLocation
            ?.let { guidedNavigationTree.firstMatchingLocation(it) }
            ?.firstContentNode()
        nodeFromLocation ?: guidedNavigationTree.firstContentNode() ?: guidedNavigationTree
    }

    private val stateMutable: MutableStateFlow<ReadAloudStateMachine.State> = run {
        val itemRef = dataLoader.getItemRef(initialNode)!! // there is at least one node to read
        val nodeIndex = itemRef.nodeIndex!! // the node has content

        MutableStateFlow(
            stateMachine.play(
                segment = itemRef.segment,
                itemIndex = nodeIndex,
                playWhenReady = true,
                settings = settings
            )
        )
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    public val playback: StateFlow<Playback> =
        stateMutable.mapStateIn(coroutineScope) { state ->
            val textRef = state.node.refs.firstNotNullOfOrNull { it as? GuidedNavigationTextRef }
            val textHref = textRef?.url?.removeFragment()
            val textItemMediaType = textHref?.let {
                resources.firstOrNull { item -> item.href == textHref }?.mediaType
            }
            Playback(
                playWhenReady = state.playWhenReady,
                state = state.playbackState.toState(),
                node = state.node,
                textItemMediaType = textItemMediaType
            )
        }
    private fun ReadAloudStateMachine.PlaybackState.toState(): PlaybackState =
        when (this) {
            is ReadAloudStateMachine.PlaybackState.Ready ->
                PlaybackState.Ready
            is ReadAloudStateMachine.PlaybackState.Starved ->
                PlaybackState.Starved
            is ReadAloudStateMachine.PlaybackState.Ended ->
                PlaybackState.Ended
            is ReadAloudStateMachine.PlaybackState.Failure ->
                PlaybackState.Failure(Error.AudioEngineError(error))
        }

    public val locations: StateFlow<List<ReadAloudLocation>> =
        stateMutable.runningFold(
            emptyList()
        ) { prevLocations: List<ReadAloudLocation>, state ->
            val textLocation = state.node.toReadAloudTextLocation()
            val audioLocation = state.node.toReadAloudAudioLocation()
            buildList {
                when (state.segment) {
                    is AudioSegment -> {
                        audioLocation?.let { add(it) }
                        textLocation?.let { add(it) }
                    }
                    is TtsSegment -> {
                        textLocation?.let { add(it) }
                        audioLocation?.let { add(it) }
                    }
                }

                if (none { it is ReadAloudTextLocation }) {
                    prevLocations.firstOrNull { it is ReadAloudTextLocation }
                        ?.let { add(it) }
                }

                if (none { it is ReadAloudAudioLocation }) {
                    prevLocations.firstOrNull { it is ReadAloudAudioLocation }
                        ?.let { add(it) }
                }
            }
        }.stateInFirst(
            scope = coroutineScope,
            started = SharingStarted.Eagerly
        )

    private fun ReadAloudNode.toReadAloudTextLocation(): ReadAloudTextLocation? {
        val textRef = refs.firstNotNullOfOrNull { it as? GuidedNavigationTextRef }
            ?: return null
        val href = textRef.url.removeFragment()
        val cssSelector = textRef.url.fragment
            ?.let { fragment -> CssSelector(fragment.addPrefix("#")) }
        return ReadAloudTextLocation(href = href, cssSelector = cssSelector, textAnchor = null)
    }

    private fun ReadAloudNode.toReadAloudAudioLocation(): ReadAloudAudioLocation? {
        val audioRef = refs.firstNotNullOfOrNull { it as? GuidedNavigationAudioRef }
            ?: return null

        val href = audioRef.url.removeFragment()
        val timeOffset = audioRef.url.fragment
            ?.let { TemporalFragmentParser.parse(it) }
            ?.start
            ?.let { TimeOffset(it) }
        return ReadAloudAudioLocation(href = href, timeOffset = timeOffset)
    }

    private fun <T> Flow<T>.stateInFirst(
        scope: CoroutineScope,
        started: SharingStarted,
    ): StateFlow<T> {
        val first = runBlocking { first() }
        return stateIn(scope, started, first)
    }

    public fun play() {
        with(stateMachine) {
            stateMutable.value = stateMutable.value.resume()
        }
    }

    public fun pause() {
        with(stateMachine) {
            stateMutable.value = stateMutable.value.pause()
        }
    }

    public fun go(node: ReadAloudNode) {
        with(stateMachine) {
            stateMutable.value = stateMutable.value.jump(node)
        }
    }

    public fun canEscape(): Boolean =
        with(navigationHelper) {
            stateMutable.value.node.isEscapable()
        }

    public fun canSkip(): Boolean =
        with(navigationHelper) {
            stateMutable.value.node.isSkippable()
        }

    public fun escape(force: Boolean = true) {
        with(navigationHelper) {
            stateMutable.value.node.escape(force)
                ?.let { go(it) }
        }
    }

    public fun skipToPrevious(force: Boolean = true) {
        with(navigationHelper) {
            stateMutable.value.node.skipToPrevious(force)
                ?.let { go(it) }
        }
    }

    public fun skipToNext(force: Boolean = true) {
        with(navigationHelper) {
            stateMutable.value.node.skipToNext(force)
                ?.let { go(it) }
        }
    }

    public fun goTo(location: ReadAloudLocation) {
        with(navigationHelper) {
            guidedNavigationTree.firstMatchingLocation(location)
                ?.let { go(it) }
        }
    }

    public fun goTo(url: Url) {
        val location = url.fragment?.let { TemporalFragmentParser.parse(it) }
            ?.let { timeInterval ->
                val href = url.removeFragment()
                val timeOffset = TimeOffset(timeInterval.start ?: Duration.ZERO)
                ReadAloudAudioLocation(href = href, timeOffset = timeOffset)
            } ?: ReadAloudTextLocation(
            href = url.removeFragment(),
            cssSelector = url.fragment?.let { CssSelector(it.addPrefix("#")) },
            textAnchor = null
        )

        goTo(location)
    }

    public fun release() {
        with(stateMachine) {
            stateMutable.value = stateMutable.value.release()
        }
    }
}
