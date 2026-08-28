/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.exoplayer.audio

import android.app.Application
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.readium.navigator.media.audio.AudioEngineProvider
import org.readium.navigator.media.common.DefaultMediaMetadataProvider
import org.readium.navigator.media.common.MediaMetadataProvider
import org.readium.r2.navigator.extensions.time
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.Try

/**
 * Main component to use the audio navigator with the ExoPlayer adapter.
 *
 * Provide [ExoPlayerDefaults] to customize the default values that will be used by
 * the navigator for some preferences.
 *
 * Pass an [ExoPlayerCacheProvider] to enable caching of remote resources.
 */
@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
public class ExoPlayerEngineProvider(
    private val application: Application,
    private val metadataProvider: MediaMetadataProvider = DefaultMediaMetadataProvider(),
    private val cacheProvider: ExoPlayerCacheProvider? = null,
    private val defaults: ExoPlayerDefaults = ExoPlayerDefaults(),
    private val configuration: ExoPlayerEngine.Configuration = ExoPlayerEngine.Configuration(),
) : AudioEngineProvider<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerPreferencesEditor> {

    override suspend fun createEngine(
        publication: Publication,
        initialLocator: Locator,
        initialPreferences: ExoPlayerPreferences,
    ): Try<ExoPlayerEngine, Nothing> {
        val metadataFactory = metadataProvider.createMetadataFactory(publication)
        val settingsResolver = ExoPlayerSettingsResolver(defaults)
        val dataSourceFactory = cacheProvider
            ?.createCacheDataSourceFactory(publication)
            ?: ExoPlayerDataSource.Factory(publication)
        val initialIndex = publication.readingOrder.indexOfFirstWithHref(initialLocator.href) ?: 0
        val initialPosition = initialLocator.locations.time ?: Duration.ZERO
        val playlist = ExoPlayerEngine.Playlist(
            mediaMetadata = metadataFactory.publicationMetadata(),
            duration = publication.metadata.duration?.seconds,
            items = publication.readingOrder.mapIndexed { index, link ->
                ExoPlayerEngine.Playlist.Item(
                    url = link.url(),
                    mediaMetadata = metadataFactory.resourceMetadata(index),
                    duration = link.duration?.seconds
                )
            }
        )

        val engine = ExoPlayerEngine(
            application = application,
            settingsResolver = settingsResolver,
            playlist = playlist,
            dataSourceFactory = dataSourceFactory,
            configuration = configuration,
            initialIndex = initialIndex,
            initialPosition = initialPosition,
            initialPreferences = initialPreferences
        )

        return Try.success(engine)
    }

    override fun createPreferenceEditor(
        publication: Publication,
        initialPreferences: ExoPlayerPreferences,
    ): ExoPlayerPreferencesEditor =
        ExoPlayerPreferencesEditor(
            initialPreferences,
            publication.metadata,
            defaults
        )

    override fun createEmptyPreferences(): ExoPlayerPreferences =
        ExoPlayerPreferences()
}
