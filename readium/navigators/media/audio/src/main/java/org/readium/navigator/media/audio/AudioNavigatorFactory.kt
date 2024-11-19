/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.audio

import android.os.Build
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

@ExperimentalReadiumApi
@OptIn(ExperimentalTime::class, DelicateReadiumApi::class)
public class AudioNavigatorFactory<
    S : Configurable.Settings,
    P : Configurable.Preferences<P>,
    E : PreferencesEditor<P>,
    > private constructor(
    private val publication: Publication,
    private val audioEngineProvider: AudioEngineProvider<S, P, E>,
) {

    public companion object {

        public operator fun <
            S : Configurable.Settings,
            P : Configurable.Preferences<P>,
            E : PreferencesEditor<P>,
            > invoke(
            publication: Publication,
            audioEngineProvider: AudioEngineProvider<S, P, E>,
        ): AudioNavigatorFactory<S, P, E>? {
            if (!publication.conformsTo(Publication.Profile.AUDIOBOOK)) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            if (publication.readingOrder.any { it.duration == 0.0 }) {
                return null
            }

            return AudioNavigatorFactory(
                publication,
                audioEngineProvider
            )
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public class UnsupportedPublication(
            cause: org.readium.r2.shared.util.Error? = null,
        ) : Error("Publication is not supported.", cause)

        public class EngineInitialization(
            cause: org.readium.r2.shared.util.Error? = null,
        ) : Error("Failed to initialize audio engine.", cause)
    }

    public suspend fun createNavigator(
        initialLocator: Locator? = null,
        initialPreferences: P? = null,
        readingOrder: List<Link> = publication.readingOrder,
    ): Try<AudioNavigator<S, P>, Error> {
        fun duration(link: Link, publication: Publication): Duration? {
            var duration: Duration? = link.duration?.seconds
                .takeUnless { it == Duration.ZERO }

            if (duration == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val resource = requireNotNull(publication.get(link))
                val metadataRetriever = MetadataRetriever(resource)
                duration = metadataRetriever.duration()
                metadataRetriever.close()
            }

            return duration
        }

        val items = readingOrder.map {
            AudioNavigator.ReadingOrder.Item(
                href = it.url(),
                duration = duration(it, publication)
            )
        }
        val totalDuration = publication.metadata.duration?.seconds
            ?: items.mapNotNull { it.duration }
                .takeIf { it.size == items.size }
                ?.sum()

        val actualReadingOrder = AudioNavigator.ReadingOrder(totalDuration, items)

        val actualInitialLocator =
            initialLocator?.let { publication.normalizeLocator(it) }
                ?: publication.locatorFromLink(publication.readingOrder[0])!!

        val audioEngine =
            audioEngineProvider.createEngine(
                publication,
                actualInitialLocator,
                initialPreferences ?: audioEngineProvider.createEmptyPreferences()
            ).getOrElse {
                return Try.failure(Error.EngineInitialization(it))
            }

        val audioNavigator = AudioNavigator(
            publication = publication,
            audioEngine = audioEngine,
            readingOrder = actualReadingOrder
        )

        return Try.success(audioNavigator)
    }

    public fun createAudioPreferencesEditor(
        currentPreferences: P,
    ): E = audioEngineProvider.createPreferenceEditor(publication, currentPreferences)
}
