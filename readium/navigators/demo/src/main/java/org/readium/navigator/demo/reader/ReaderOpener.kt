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
import org.readium.navigator.web.ReflowableWebNavigatorFactory
import org.readium.navigator.web.ReflowableWebRenditionController
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.location.ReflowableWebLocation
import org.readium.navigator.web.preferences.FixedWebPreferences
import org.readium.navigator.web.preferences.ReflowableWebPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
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
    private val application: Application,
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
                when (publication.metadata.presentation.layout) {
                    EpubLayout.FIXED ->
                        createFixedWebReader(url, publication, initialLocator)
                    EpubLayout.REFLOWABLE, null ->
                        createReflowableWebReader(url, publication, initialLocator)
                }

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

    private suspend fun createReflowableWebReader(
        url: AbsoluteUrl,
        publication: Publication,
        initialLocator: Locator?,
    ): Try<ReaderState<ReflowableWebLocation, ReflowableWebRenditionController>, Error> {
        val navigatorFactory = ReflowableWebNavigatorFactory(application, publication)
            ?: return Try.failure(DebugError("Publication not supported"))

        val locatorAdapter = navigatorFactory.createLocatorAdapter()

        val initialLocation = with(locatorAdapter) { initialLocator?.toGoLocation() }

        val coroutineScope = MainScope()

        val initialPreferences = ReflowableWebPreferences()

        val preferencesManager = PreferencesManager(initialPreferences)

        val preferencesEditor = navigatorFactory.createPreferencesEditor(initialPreferences)

        snapshotFlow { preferencesEditor.preferences }
            .onEach { preferencesManager.setPreferences(it) }
            .launchIn(coroutineScope)

        val renditionState = navigatorFactory.createRenditionState(
            initialSettings = preferencesEditor.settings,
            initialLocation = initialLocation
        ).getOrElse {
            return Try.failure(it)
        }

        val onControllerAvailable: (ReflowableWebRenditionController) -> Unit = { controller ->
            snapshotFlow { preferencesEditor.settings }
                .onEach { controller.settings.value = it }
                .launchIn(coroutineScope)
        }

        val readerState = ReaderState(
            url = url,
            coroutineScope = coroutineScope,
            publication = publication,
            renditionState = renditionState,
            preferencesEditor = preferencesEditor,
            locatorAdapter = locatorAdapter,
            onControllerAvailable = onControllerAvailable
        )

        return Try.success(readerState)
    }

    private suspend fun createFixedWebReader(
        url: AbsoluteUrl,
        publication: Publication,
        initialLocator: Locator?,
    ): Try<ReaderState<FixedWebLocation, FixedWebRenditionController>, Error> {
        val navigatorFactory = FixedWebNavigatorFactory(application, publication)
            ?: return Try.failure(DebugError("Publication not supported"))

        val locatorAdapter = navigatorFactory.createLocatorAdapter()

        val initialLocation = with(locatorAdapter) { initialLocator?.toGoLocation() }

        val coroutineScope = MainScope()

        val initialPreferences = FixedWebPreferences()

        val preferencesManager = PreferencesManager(initialPreferences)

        val preferencesEditor = navigatorFactory.createPreferencesEditor(initialPreferences)

        snapshotFlow { preferencesEditor.preferences }
            .onEach { preferencesManager.setPreferences(it) }
            .launchIn(coroutineScope)

        val renditionState = navigatorFactory.createRenditionState(
            initialSettings = preferencesEditor.settings,
            initialLocation = initialLocation
        ).getOrElse {
            return Try.failure(it)
        }

        val onControllerAvailable: (FixedWebRenditionController) -> Unit = { controller ->
            snapshotFlow { preferencesEditor.settings }
                .onEach { controller.settings.value = it }
                .launchIn(coroutineScope)
        }

        val readerState = ReaderState(
            url = url,
            coroutineScope = coroutineScope,
            publication = publication,
            renditionState = renditionState,
            preferencesEditor = preferencesEditor,
            locatorAdapter = locatorAdapter,
            onControllerAvailable = onControllerAvailable
        )

        return Try.success(readerState)
    }
}
