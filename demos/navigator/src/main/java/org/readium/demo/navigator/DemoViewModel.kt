/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.adapter.exoplayer.readaloud.ExoPlayerEngineProvider
import org.readium.demo.navigator.reader.ReaderOpener
import org.readium.demo.navigator.reader.ReaderState
import org.readium.demo.navigator.reader.SelectNavigatorItem
import org.readium.demo.navigator.reader.SelectNavigatorViewModel
import org.readium.demo.navigator.reader.fixedConfig
import org.readium.demo.navigator.reader.reflowableConfig
import org.readium.navigator.media.readaloud.ReadAloudNavigatorFactory
import org.readium.navigator.media.readaloud.SystemTtsEngineProvider
import org.readium.navigator.web.fixedlayout.FixedWebRenditionFactory
import org.readium.navigator.web.reflowable.ReflowableWebRenditionFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toDebugDescription
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber

class DemoViewModel(
    application: Application,
) : AndroidViewModel(application) {

    sealed interface State {

        data object BookSelection :
            State

        data class NavigatorSelection(
            val viewModel: SelectNavigatorViewModel,
        ) : State

        data object Loading :
            State

        data class Error(
            val error: org.readium.r2.shared.util.Error,
        ) : State

        data class Reader(
            val readerState: ReaderState,
        ) : State
    }

    init {
        Timber.plant(Timber.DebugTree())
    }

    private val httpClient =
        DefaultHttpClient()

    private val assetRetriever =
        AssetRetriever(application.contentResolver, httpClient)

    private val publicationParser =
        DefaultPublicationParser(application, httpClient, assetRetriever, null)

    private val publicationOpener =
        PublicationOpener(publicationParser)

    private val readerOpener =
        ReaderOpener(application)

    private val audioEngineProvider =
        ExoPlayerEngineProvider(application)

    private val stateMutable: MutableStateFlow<State> =
        MutableStateFlow(State.BookSelection)

    val state: StateFlow<State> = stateMutable.asStateFlow()

    fun onBookSelected(url: AbsoluteUrl) {
        stateMutable.value = State.Loading

        viewModelScope.launch {
            val asset = assetRetriever.retrieve(url)
                .getOrElse {
                    Timber.d(it.toDebugDescription())
                    stateMutable.value = State.Error(it)
                    return@launch
                }

            val publication = publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse {
                    asset.close()
                    Timber.d(it.toDebugDescription())
                    stateMutable.value = State.Error(it)
                    return@launch
                }

            val reflowableFactory =
                ReflowableWebRenditionFactory(
                    application = application,
                    publication = publication,
                    configuration = reflowableConfig
                )?.let { SelectNavigatorItem.ReflowableWeb(it) }

            val fixedFactory =
                FixedWebRenditionFactory(
                    application = application,
                    publication = publication,
                    configuration = fixedConfig
                )?.let { SelectNavigatorItem.FixedWeb(it) }

            val ttsEngineProvider =
                SystemTtsEngineProvider(application)

            val readAloudFactory = ReadAloudNavigatorFactory.invoke(
                application = application,
                publication = publication,
                audioEngineProvider = audioEngineProvider,
                ttsEngineProvider = ttsEngineProvider
            )?.let { SelectNavigatorItem.ReadAloud(it) }

            val factories = listOfNotNull(
                reflowableFactory,
                fixedFactory,
                readAloudFactory
            )

            when (factories.size) {
                0 -> {
                    val error = DebugError("Publication not supported")
                    Timber.d(error.toDebugDescription())
                    stateMutable.value = State.Error(error)
                }
                1 -> {
                    onNavigatorSelected(url, publication, factories.first())
                }
                else -> {
                    val selectionViewModel = SelectNavigatorViewModel(
                        items = factories,
                        onItemSelected = { onNavigatorSelected(url, publication, it) },
                        onMenuDismissed = { stateMutable.value = State.BookSelection }
                    )

                    stateMutable.value =
                        State.NavigatorSelection(selectionViewModel)
                }
            }
        }
    }

    fun onNavigatorSelected(
        url: AbsoluteUrl,
        publication: Publication,
        navigatorItem: SelectNavigatorItem,
    ) {
        stateMutable.value = State.Loading

        viewModelScope.launch {
            readerOpener.open(url, publication, navigatorItem)
                .onFailure {
                    Timber.d(it.toDebugDescription())
                    stateMutable.value = State.Error(it)
                }
                .onSuccess { stateMutable.value = State.Reader(it) }
        }
    }

    fun onBookClosed() {
        val stateNow = state.value
        check(stateNow is State.Reader)
        stateMutable.value = State.BookSelection
        stateNow.readerState.close()
    }

    fun onErrorDisplayed() {
        stateMutable.value = State.BookSelection
    }
}
