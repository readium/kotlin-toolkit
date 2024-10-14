/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo.reader

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.adapter.pdfium.navigator.PdfiumSettings
import org.readium.navigator.demo.preferences.PreferencesManager
import org.readium.navigator.demo.preferences.UserPreferencesViewModel
import org.readium.navigator.pdf.PdfNavigatorFactory
import org.readium.navigator.web.FixedWebNavigatorFactory
import org.readium.navigator.web.preferences.FixedWebPreferences
import org.readium.navigator.web.preferences.FixedWebSettings
import org.readium.r2.shared.ExperimentalReadiumApi
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

    private val pdfiumDocumentFactory =
        PdfiumDocumentFactory(application)

    private val publicationParser =
        DefaultPublicationParser(application, httpClient, assetRetriever, pdfiumDocumentFactory)

    private val publicationOpener =
        PublicationOpener(publicationParser)

    private val pdfEngineProvider =
        PdfiumEngineProvider()

    suspend fun open(url: AbsoluteUrl): Try<ReaderState, Error> {
        val asset = assetRetriever.retrieve(url)
            .getOrElse { return Try.failure(it) }

        val publication = publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                return Try.failure(it)
            }

        val readerState = when {
            publication.conformsTo(Publication.Profile.EPUB) ->
                createFixedWebReader(publication)
            publication.conformsTo(Publication.Profile.PDF) ->
                createPdfReader(publication)
            else ->
                Try.failure(DebugError("Publication not supported"))
        }.getOrElse { error ->
            publication.close()
            return Try.failure(error)
        }

        return Try.success(readerState)
    }

    private suspend fun createFixedWebReader(
        publication: Publication
    ): Try<ReaderState, Error> {
        val navigatorFactory = FixedWebNavigatorFactory(application, publication)
            ?: return Try.failure(DebugError("Publication not supported"))

        val initialPreferences = FixedWebPreferences()

        val navigatorState = navigatorFactory.createNavigator(
            initialPreferences = initialPreferences
        ).getOrElse {
            return Try.failure(it)
        }

        val coroutineScope = MainScope()

        val preferencesViewModel =
            UserPreferencesViewModel<FixedWebSettings, FixedWebPreferences>(
                viewModelScope = coroutineScope,
                preferencesManager = PreferencesManager(initialPreferences),
                createPreferencesEditor = navigatorFactory::createPreferencesEditor
            )

        preferencesViewModel.preferences
            .onEach {
                navigatorState.preferences.value = it
            }.launchIn(coroutineScope)

        val readerState = ReaderState(
            coroutineScope = coroutineScope,
            publication = publication,
            navigatorState = navigatorState,
            preferencesViewModel = preferencesViewModel
        )

        return Try.success(readerState)
    }

    private fun createPdfReader(
        publication: Publication
    ): Try<ReaderState, Error> {
        val navigatorFactory = PdfNavigatorFactory(publication, pdfEngineProvider)

        val initialPreferences = PdfiumPreferences()

        val navigatorState = navigatorFactory.createNavigator(
            initialPreferences = initialPreferences
        ).getOrElse {
            throw IllegalStateException()
        }

        val coroutineScope = MainScope()

        val preferencesViewModel =
            UserPreferencesViewModel<PdfiumSettings, PdfiumPreferences>(
                viewModelScope = coroutineScope,
                preferencesManager = PreferencesManager(initialPreferences),
                createPreferencesEditor = navigatorFactory::createPreferencesEditor
            )

        preferencesViewModel.preferences
            .onEach { navigatorState.preferences.value = it }
            .launchIn(coroutineScope)

        val readerState = ReaderState(
            coroutineScope = coroutineScope,
            publication = publication,
            navigatorState = navigatorState,
            preferencesViewModel = preferencesViewModel
        )

        return Try.success(readerState)
    }
}
