/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.demo.navigator.decorations.FixedWebHighlightsManager
import org.readium.demo.navigator.decorations.HighlightsManager
import org.readium.demo.navigator.decorations.ReflowableWebHighlightsManager
import org.readium.demo.navigator.decorations.pageNumberDecorations
import org.readium.demo.navigator.persistence.LocatorRepository
import org.readium.demo.navigator.preferences.PreferencesManager
import org.readium.navigator.common.DecorationController
import org.readium.navigator.common.DecorationLocation
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.common.Settings
import org.readium.navigator.common.SettingsController
import org.readium.navigator.web.fixedlayout.FixedWebGoLocation
import org.readium.navigator.web.fixedlayout.FixedWebLocation
import org.readium.navigator.web.fixedlayout.FixedWebRenditionController
import org.readium.navigator.web.fixedlayout.FixedWebRenditionFactory
import org.readium.navigator.web.fixedlayout.FixedWebSelectionLocation
import org.readium.navigator.web.fixedlayout.preferences.FixedWebPreferences
import org.readium.navigator.web.reflowable.ReflowableWebGoLocation
import org.readium.navigator.web.reflowable.ReflowableWebLocation
import org.readium.navigator.web.reflowable.ReflowableWebRenditionController
import org.readium.navigator.web.reflowable.ReflowableWebRenditionFactory
import org.readium.navigator.web.reflowable.ReflowableWebSelectionLocation
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferences
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

    suspend fun open(url: AbsoluteUrl): Try<ReaderState<*, *, *, *>, Error> {
        val asset = assetRetriever.retrieve(url)
            .getOrElse { return Try.failure(it) }

        val publication = publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                return Try.failure(it)
            }

        val initialLocator = LocatorRepository.getLocator(url)

        val readerState = (
            createFixedWebReader(url, publication, initialLocator)
                ?: createReflowableWebReader(url, publication, initialLocator)
            )
            .or { Try.failure(DebugError("Publication not supported")) }
            .getOrElse { error ->
                publication.close()
                return Try.failure(error)
            }

        return Try.success(readerState)
    }

    private fun <S, F> Try<S, F>?.or(onNull: () -> Try<S, F>): Try<S, F> =
        this ?: onNull()

    private suspend fun createReflowableWebReader(
        url: AbsoluteUrl,
        publication: Publication,
        initialLocator: Locator?,
    ): Try<ReaderState<ReflowableWebLocation, ReflowableWebGoLocation, ReflowableWebSelectionLocation, ReflowableWebRenditionController>, Error>? {
        val navigatorFactory = ReflowableWebRenditionFactory(
            application = application,
            publication = publication,
            configuration = reflowableConfig
        ) ?: return null

        val initialLocation = initialLocator?.let { ReflowableWebGoLocation(it) }

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

        val highlightsManager = ReflowableWebHighlightsManager()

        val onControllerAvailable: (ReflowableWebRenditionController) -> Unit = { controller ->
            applySettings(coroutineScope, controller, preferencesEditor)
            applyHighlightDecorations(coroutineScope, controller, highlightsManager)

            publication.pageNumberDecorations
                .takeIf { it.isNotEmpty() }
                ?.let { controller.decorations + ("pageNumbers" to it) }
        }

        val actionModeFactory = SelectionActionModeFactory(highlightsManager)

        val readerState = ReaderState(
            url = url,
            coroutineScope = coroutineScope,
            publication = publication,
            renditionState = renditionState,
            preferencesEditor = preferencesEditor,
            onControllerAvailable = onControllerAvailable,
            actionModeFactory = actionModeFactory,
            highlightsManager = highlightsManager
        )

        return Try.success(readerState)
    }

    private suspend fun createFixedWebReader(
        url: AbsoluteUrl,
        publication: Publication,
        initialLocator: Locator?,
    ): Try<ReaderState<FixedWebLocation, FixedWebGoLocation, FixedWebSelectionLocation, FixedWebRenditionController>, Error>? {
        val navigatorFactory = FixedWebRenditionFactory(
            application = application,
            publication = publication,
            configuration = fixedConfig
        ) ?: return null

        val initialLocation = initialLocator?.let { FixedWebGoLocation(it) }

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

        val highlightsManager = FixedWebHighlightsManager()

        val onControllerAvailable: (FixedWebRenditionController) -> Unit = { controller ->
            applySettings(coroutineScope, controller, preferencesEditor)
            applyHighlightDecorations(coroutineScope, controller, highlightsManager)
        }

        val actionModeFactory = SelectionActionModeFactory(highlightsManager)

        val readerState = ReaderState(
            url = url,
            coroutineScope = coroutineScope,
            publication = publication,
            renditionState = renditionState,
            preferencesEditor = preferencesEditor,
            onControllerAvailable = onControllerAvailable,
            highlightsManager = highlightsManager,
            actionModeFactory = actionModeFactory
        )

        return Try.success(readerState)
    }

    private fun <S : Settings> applySettings(
        coroutineScope: CoroutineScope,
        settingsController: SettingsController<S>,
        preferencesEditor: PreferencesEditor<*, S>,
    ) {
        snapshotFlow { preferencesEditor.settings }
            .onEach { settingsController.settings = it }
            .launchIn(coroutineScope)
    }

    private fun <L : DecorationLocation> applyHighlightDecorations(
        coroutineScope: CoroutineScope,
        decorationController: DecorationController<L>,
        highlightsManager: HighlightsManager<L>,
    ) {
        highlightsManager.decorations
            .onEach {
                decorationController.decorations += ("highlights" to it)
            }.launchIn(coroutineScope)
    }
}
