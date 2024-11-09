/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo.reader

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.demo.persistence.LocatorRepository
import org.readium.navigator.demo.preferences.PreferencesManager
import org.readium.navigator.web.FixedWebNavigatorFactory
import org.readium.navigator.web.FixedWebRenditionController
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.preferences.FixedWebPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class ReaderOpener(
    private val application: Application
) {

    private val httpClient =
        DefaultHttpClient()

    private val assetRetriever =
        AssetRetriever(application.contentResolver, httpClient)

    private val publicationParser =
        DefaultPublicationParser(application, httpClient, assetRetriever, null)

    private val publicationOpener =
        PublicationOpener(publicationParser)

    suspend fun open(url: AbsoluteUrl): Try<ReaderState<*, *>, Error> {
        val asset = assetRetriever.retrieve(url)
            .getOrElse { return Try.failure(it) }

        val publication = publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                return Try.failure(it)
            }

        val initialLocator = LocatorRepository.getLocator(url)

        val readerState = when {
            publication.conformsTo(Publication.Profile.EPUB) ->
                createFixedWebReader(url, publication, initialLocator)

            /* publication.conformsTo(Publication.Profile.PDF) ->
                createPdfReader(url, publication, initialLocator) */

            else ->
                Try.failure(DebugError("Publication not supported"))
        }.getOrElse { error ->
            publication.close()
            return Try.failure(error)
        }

        return Try.success(readerState)
    }

    private suspend fun createFixedWebReader(
        url: AbsoluteUrl,
        publication: Publication,
        initialLocator: Locator?
    ): Try<ReaderState<FixedWebLocation, FixedWebRenditionController>, Error> {
        val navigatorFactory = FixedWebNavigatorFactory(application, publication)
            ?: return Try.failure(DebugError("Publication not supported"))

        val locatorAdapter = navigatorFactory.createLocatorAdapter()

        val initialLocation = with(locatorAdapter) { initialLocator?.toGoLocation() }

        val coroutineScope = MainScope()

        val initialPreferences = FixedWebPreferences()

        val preferencesManager = PreferencesManager(initialPreferences)

        val settingsEditor = navigatorFactory.createPreferencesEditor(initialPreferences)

        snapshotFlow { settingsEditor.preferences }
            .onEach { preferencesManager.setPreferences(it) }
            .launchIn(coroutineScope)

        val navigatorState = navigatorFactory.createRenditionState(
            initialSettings = settingsEditor.settings,
            initialLocation = initialLocation
        ).getOrElse {
            return Try.failure(it)
        }

        val onNavigatorCreated: (FixedWebRenditionController) -> Unit = { navigator ->
            snapshotFlow { settingsEditor.settings }
                .onEach { navigator.settings.value = it }
                .launchIn(coroutineScope)
        }

        val readerState = ReaderState(
            url = url,
            coroutineScope = coroutineScope,
            publication = publication,
            renditionState = navigatorState,
            preferencesEditor = settingsEditor,
            locatorAdapter = locatorAdapter,
            onNavigatorCreated = onNavigatorCreated
        )

        return Try.success(readerState)
    }
}
