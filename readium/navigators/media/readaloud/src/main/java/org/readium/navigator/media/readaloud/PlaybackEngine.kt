/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import kotlin.time.Duration
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.TimeInterval
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public interface AudioEngineProvider<out E : Error> {

    public fun createEngineFactory(
        publication: Publication,
    ): AudioEngineFactory<E>
}

@ExperimentalReadiumApi
public interface AudioEngineFactory<out E : Error> {

    public fun createPlaybackEngine(
        chunks: List<AudioChunk>,
        listener: PlaybackEngine.Listener<AudioEngineProgress, E>,
    ): PlaybackEngine
}

@ExperimentalReadiumApi
public data class AudioChunk(
    val href: Url,
    val interval: TimeInterval?,
)

@ExperimentalReadiumApi
public interface TtsEngineProvider<out V : TtsVoice, out E : Error> {

    public suspend fun createEngineFactory(): TtsEngineFactory<V, E>
}

@ExperimentalReadiumApi
public interface TtsEngineFactory<out V : TtsVoice, out E : Error> {

    /**
     * Sets of voices available with this [TtsEngineFactory].
     */
    public val voices: Set<V>

    /**
     * Creates a [PlaybackEngine] to read [utterances] using the given [voiceId].
     *
     * Throws if the given [voiceId] matches no voice in [voices].
     */
    public fun createPlaybackEngine(
        voiceId: TtsVoice.Id,
        utterances: List<String>,
        listener: PlaybackEngine.Listener<TtsEngineProgress, E>,
    ): PlaybackEngine
}

internal class NullTtsEngineFactory<V : TtsVoice, E : Error>() : TtsEngineFactory<V, E> {

    override val voices: Set<V> = emptySet()

    override fun createPlaybackEngine(
        voiceId: TtsVoice.Id,
        utterances: List<String>,
        listener: PlaybackEngine.Listener<TtsEngineProgress, E>,
    ): PlaybackEngine {
        throw IllegalArgumentException("Unknown voice.")
    }
}

/**
 * Engine reading aloud a list of items.
 */
@ExperimentalReadiumApi
public interface PlaybackEngine {

    /**
     * State of the playback.
     */
    public enum class PlaybackState {
        /**
         * The playback is ongoing.
         */
        Playing,

        /**
         * The playback has been momentarily interrupted because of a lack of ready data.
         */
        Starved,
    }

    /**
     * Marker interface for playback progress information.
     */
    public interface Progress

    /**
     * Listener for a [PlaybackEngine]
     */
    public interface Listener<in P : Progress, in E : Error> {

        /**
         * Called after [start] was invoked. [initialState] tells you if the engine has enough data
         * to start playing right now or must still wait for data.
         */
        public fun onStartRequested(initialState: PlaybackState)

        /**
         * Called when the playback state changes.
         */
        public fun onPlaybackStateChanged(state: PlaybackState)

        /**
         * Called when the last playback request completed.
         */
        public fun onPlaybackCompleted()

        /**
         * Called when an error occurred during playback.
         */
        public fun onPlaybackError(error: E)

        /**
         * Called regularly to report progress when this information is available.
         */
        public fun onPlaybackProgressed(progress: P)
    }

    /**
     * Sets the playback pitch.
     */
    public var pitch: Double

    /**
     * Sets the playback speed.
     */
    public var speed: Double

    /**
     * Sets the index of the item to play on the next call to [start].
     *
     * The behavior is undefined if this property is set during playback.
     */
    public var itemToPlay: Int

    /**
     * Starts playing the [itemToPlay]-th item.
     */
    public fun start()

    /**
     * Stops ongoing playback.
     */
    public fun stop()

    /**
     * Pauses playback.
     */
    public fun pause()

    /**
     * Resumes playback where it was paused if possible, or starts again otherwise.
     */
    public fun resume()

    /**
     * Free all used resources.
     */
    public fun release()
}

@ExperimentalReadiumApi
public interface TtsVoice {

    @kotlinx.serialization.Serializable
    @JvmInline
    public value class Id(public val value: String)

    public val id: Id

    /**
     * The languages supported by the voice.
     */
    public val languages: Set<Language>
}

@ExperimentalReadiumApi
@JvmInline
public value class TtsEngineProgress(public val value: IntRange) : PlaybackEngine.Progress

@ExperimentalReadiumApi
@JvmInline
public value class AudioEngineProgress(public val value: Duration) : PlaybackEngine.Progress
