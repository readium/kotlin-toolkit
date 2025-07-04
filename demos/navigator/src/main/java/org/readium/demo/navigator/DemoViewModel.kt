/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.demo.navigator.reader.ReaderOpener
import org.readium.demo.navigator.reader.ReaderState
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toDebugDescription
import timber.log.Timber

class DemoViewModel(
    application: Application,
) : AndroidViewModel(application) {

    sealed interface State {

        data object BookSelection :
            State

        data object Loading :
            State

        data class Error(
            val error: org.readium.r2.shared.util.Error,
        ) : State

        data class Reader(
            val readerState: ReaderState<*, *, *, *>,
        ) : State
    }

    init {
        Timber.plant(Timber.DebugTree())
    }

    private val readerOpener =
        ReaderOpener(application)

    private val stateMutable: MutableStateFlow<State> =
        MutableStateFlow(State.BookSelection)

    val state: StateFlow<State> = stateMutable.asStateFlow()

    fun onBookSelected(url: AbsoluteUrl) {
        stateMutable.value = State.Loading

        viewModelScope.launch {
            readerOpener.open(url)
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
