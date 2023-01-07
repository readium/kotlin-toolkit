/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.media3.api.MediaMetadataFactory
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class PlayerNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>> private constructor(
    override val publication: Publication,
    private val playerNavigator: PlayerNavigatorInternal<S, P>,
) : MediaNavigator<PlayerNavigator.Playback>, Navigator, Configurable<S, P> {

    companion object {

        suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>> invoke(
            publication: Publication,
            mediaEngineProvider: MediaEngineProvider<S, P, *>,
            metadataFactory: MediaMetadataFactory,
            initialPreferences: P = mediaEngineProvider.createEmptyPreferences(),
            initialLocator: Locator = publication.startLocator,
        ): PlayerNavigator<S, P> {
            TODO("Not yet implemented")
            /*val player = mediaEngineProvider.createPlayer(publication)

            val playlist = publication.readingOrder.indices.map { index ->
                val metadata = metadataFactory.resourceMetadata(index)
                MediaItem.Builder()
                    .setMediaMetadata(metadata)
                    .build()
            }

            val publicationMetadata = metadataFactory.publicationMetadata()

            val settingsResolver = ExoPlayerSettingsResolver(publication.metadata)

            return PlayerNavigator(

            )*/
        }

        private val Publication.startLocator: Locator
            get() = locatorFromLink(readingOrder.first())!!
    }

    data class Playback(
        override val state: MediaNavigator.State,
        override val locator: Locator,
        override val buffer: MediaNavigator.Buffer
    ) : MediaNavigator.Playback, MediaNavigator.BufferProvider

    private val coroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Configurable

    override val settings: StateFlow<S> =
        TODO("Not yet implemented")

    override fun submitPreferences(preferences: P) {
        TODO("Not yet implemented")
    }

    // MediaNavigator

    override val playback: StateFlow<Playback>
        get() = TODO("Not yet implemented")

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun asPlayer(): Player {
        return playerNavigator.asPlayer()
    }

    // Navigator

    override val currentLocator: StateFlow<Locator> =
        playback.mapStateIn(coroutineScope) { it.locator }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link)
            ?: return false
        go(locator)
        return true
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        playerNavigator.goForward()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        playerNavigator.goBackward()
        return true
    }

    override fun close() {
    }
}
