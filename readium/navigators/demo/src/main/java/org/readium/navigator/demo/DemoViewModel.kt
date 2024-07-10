@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.web.NavigatorFactory
import org.readium.navigator.web.NavigatorState
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
            val state: NavigatorState
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

            val navigatorFactory = NavigatorFactory(getApplication(), publication)
                ?: run {
                    publication.close()
                    val error = DebugError("Publication not supported")
                    stateMutable.value = State.Error(error)
                    return@launch
                }

            val navigatorState = navigatorFactory.createNavigator()
                .getOrElse {
                    throw IllegalStateException()
                }

            stateMutable.value = State.Reader(navigatorState)
        }
    }

    fun acknowledgeError() {
        stateMutable.value = State.BookSelection
    }
}
