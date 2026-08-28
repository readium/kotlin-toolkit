/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.navigator.media.common.Media3Adapter
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.common.TextAwareMediaNavigator
import org.readium.navigator.media.tts.session.TtsSessionAdapter
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.combineStateIn
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/**
 * A navigator to read aloud a [Publication] with a TTS engine.
 */
@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class TtsNavigator<
    S : TtsEngine.Settings,
    P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error,
    V : TtsEngine.Voice,
    > internal constructor(
    coroutineScope: CoroutineScope,
    private val publication: Publication,
    private val player: TtsPlayer<S, P, E, V>,
    private val sessionAdapter: TtsSessionAdapter<E>,
) :
    MediaNavigator<TtsNavigator.Location, TtsNavigator.Playback, TtsNavigator.ReadingOrder>,
    TextAwareMediaNavigator<TtsNavigator.Location, TtsNavigator.Playback, TtsNavigator.ReadingOrder>,
    Media3Adapter,
    Configurable<S, P> {

    public interface Listener {

        public fun onStopRequested()
    }

    public data class Location(
        override val href: Url,
        override val utterance: String,
        override val range: IntRange?,
        override val textBefore: String?,
        override val textAfter: String?,
        override val utteranceLocator: Locator,
        override val tokenLocator: Locator?,
    ) : TextAwareMediaNavigator.Location

    public data class Playback(
        override val state: MediaNavigator.State,
        override val playWhenReady: Boolean,
        override val index: Int,
        override val utterance: String,
        override val range: IntRange?,
    ) : TextAwareMediaNavigator.Playback

    public data class ReadingOrder(
        override val items: List<Item>,
    ) : TextAwareMediaNavigator.ReadingOrder {

        public data class Item(
            val href: Url,
        ) : TextAwareMediaNavigator.ReadingOrder.Item
    }

    public sealed interface State {

        public data object Ready : State, MediaNavigator.State.Ready

        public data object Ended : State, MediaNavigator.State.Ended

        public data class Failure(val error: Error) : State, MediaNavigator.State.Failure
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public data class EngineError<E : TtsEngine.Error>(override val cause: E) :
            Error("An error occurred in the TTS engine.", cause)

        public data class ContentError(override val cause: org.readium.r2.shared.util.Error) :
            Error("An error occurred while trying to read publication content.", cause)
    }

    public val voices: Set<V> get() =
        player.voices

    override val readingOrder: ReadingOrder =
        ReadingOrder(
            items = publication.readingOrder
                .map { ReadingOrder.Item(it.url()) }
        )

    override val playback: StateFlow<Playback> =
        player.playback.combineStateIn(coroutineScope, player.utterance) { playback, utterance ->
            navigatorPlayback(playback, utterance)
        }

    override val location: StateFlow<Location> =
        player.utterance.mapStateIn(coroutineScope) { playerUtterance ->
            playerUtterance.toPosition()
        }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    public fun go(locator: Locator) {
        player.go(publication.normalizeLocator(locator))
    }

    override fun skipToPreviousUtterance() {
        player.previousUtterance()
    }

    override fun skipToNextUtterance() {
        player.nextUtterance()
    }

    override fun hasPreviousUtterance(): Boolean {
        return player.hasPreviousUtterance()
    }

    override fun hasNextUtterance(): Boolean {
        return player.hasNextUtterance()
    }

    override fun asMedia3Player(): Player =
        sessionAdapter

    override fun close() {
        player.close()
        sessionAdapter.release()
    }

    override val currentLocator: StateFlow<Locator> =
        location.mapStateIn(coroutineScope) { it.tokenLocator ?: it.utteranceLocator }

    override fun go(locator: Locator, animated: Boolean): Boolean {
        player.go(publication.normalizeLocator(locator))
        return true
    }

    override fun go(link: Link, animated: Boolean): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated)
    }

    override val settings: StateFlow<S> =
        player.settings

    override fun submitPreferences(preferences: P) {
        player.submitPreferences(preferences)
    }

    private fun navigatorPlayback(playback: TtsPlayer.Playback, utterance: TtsPlayer.Utterance) =
        Playback(
            state = playback.state.toState() as MediaNavigator.State,
            playWhenReady = playback.playWhenReady,
            index = utterance.position.resourceIndex,
            utterance = utterance.text,
            range = utterance.range
        )

    private fun TtsPlayer.State.toState(): State =
        when (this) {
            TtsPlayer.State.Ready -> State.Ready
            TtsPlayer.State.Ended -> State.Ended
            is TtsPlayer.State.Failure -> this.toError()
        }

    private fun TtsPlayer.State.Failure.toError(): State.Failure =
        when (this) {
            is TtsPlayer.State.Failure.Content -> State.Failure(Error.ContentError(error))
            is TtsPlayer.State.Failure.Engine<*> -> State.Failure(Error.EngineError(error))
        }

    private fun TtsPlayer.Utterance.toPosition(): Location {
        val currentLink = publication.readingOrder[position.resourceIndex]
        val url = currentLink.url()

        val utteranceLocator = publication
            .locatorFromLink(currentLink)!!
            .copy(
                locations = position.locations,
                text = position.text
            )

        val tokenLocator = range
            ?.let { utteranceLocator.copy(text = utteranceLocator.text.substring(it)) }

        return Location(
            href = url,
            textBefore = position.text.before,
            textAfter = position.text.after,
            utterance = text,
            range = range,
            utteranceLocator = utteranceLocator,
            tokenLocator = tokenLocator
        )
    }
}
