/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.demo.preferences.PreferencesManager
import org.readium.navigator.demo.preferences.UserPreferencesViewModel
import org.readium.navigator.web.PrepaginatedWebNavigatorFactory
import org.readium.navigator.web.PrepaginatedWebNavigatorState
import org.readium.navigator.web.preferences.NavigatorPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber

class DemoViewModel(
    application: Application
) : AndroidViewModel(application) {

    sealed interface State {

        data object BookSelection :
            State

        data object Loading :
            State

        data class Error(
            val error: org.readium.r2.shared.util.Error
        ) : State

        data class Reader(
            val navigatorState: PrepaginatedWebNavigatorState,
            val preferencesViewModel: UserPreferencesViewModel<NavigatorPreferences>
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

    private val stateMutable: MutableStateFlow<State> =
        MutableStateFlow(State.BookSelection)

    val state: StateFlow<State> = stateMutable.asStateFlow()

    fun open(url: AbsoluteUrl) {
        stateMutable.value = State.Loading

        viewModelScope.launch {
            val asset = assetRetriever.retrieve(url)
                .getOrElse {
                    stateMutable.value = State.Error(it)
                    return@launch
                }

            val publication = publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse {
                    asset.close()
                    stateMutable.value = State.Error(it)
                    return@launch
                }

            val navigatorFactory = PrepaginatedWebNavigatorFactory(getApplication(), publication)
                ?: run {
                    publication.close()
                    val error = DebugError("Publication not supported")
                    stateMutable.value = State.Error(error)
                    return@launch
                }

            val initialPreferences = NavigatorPreferences()

            val preferencesViewModel =
                UserPreferencesViewModel(
                    viewModelScope = viewModelScope,
                    preferencesManager = PreferencesManager(initialPreferences),
                    createPreferencesEditor = navigatorFactory::createPreferencesEditor
                )

            val navigatorState = navigatorFactory.createNavigator(
                initialPreferences = initialPreferences
            ).getOrElse {
                throw IllegalStateException()
            }

            preferencesViewModel.preferences
                .onEach { navigatorState.preferences.value = it }
                .launchIn(viewModelScope)

            stateMutable.value = State.Reader(navigatorState, preferencesViewModel)
        }
    }

    fun acknowledgeError() {
        stateMutable.value = State.BookSelection
    }
}
