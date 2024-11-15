/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import org.readium.navigator.demo.reader.Reader
import org.readium.navigator.demo.util.Fullscreenable

@Composable
fun Scaffold(
    fullscreenState: State<Boolean>,
    insetsController: WindowInsetsControllerCompat,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Fullscreenable(
        fullscreenState = fullscreenState,
        insetsController = insetsController
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            content.invoke()

            SnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding(),
                hostState = snackbarHostState
            )
        }
    }
}

@Composable
fun MainContent(
    viewmodel: DemoViewModel,
    launchBookSelection: () -> Unit,
    snackbarHostState: SnackbarHostState,
    fullscreenState: MutableState<Boolean>
) {
    val viewmodelState = viewmodel.state.collectAsState().value

    LaunchedEffect(viewmodelState) {
        fullscreenState.value = when (viewmodelState) {
            DemoViewModel.State.BookSelection -> true
            is DemoViewModel.State.Error -> false
            DemoViewModel.State.Loading -> true
            is DemoViewModel.State.Reader -> true
        }
    }

    when (viewmodelState) {
        DemoViewModel.State.BookSelection -> {
            Placeholder()
            LaunchedEffect(viewmodelState) {
                launchBookSelection.invoke()
            }
        }

        is DemoViewModel.State.Error -> {
            Placeholder()
            LaunchedEffect(viewmodelState.error) {
                snackbarHostState.showSnackbar(
                    message = viewmodelState.error.message,
                    duration = SnackbarDuration.Short
                )
                viewmodel.onErrorDisplayed()
            }
        }

        DemoViewModel.State.Loading -> {
            Placeholder()
            // Display and do nothing
        }

        is DemoViewModel.State.Reader -> {
            BackHandler {
                viewmodel.onBookClosed()
            }

            Reader(
                readerState = viewmodelState.readerState,
                fullScreenState = fullscreenState
            )
        }
    }
}

// This is useful for setting a background color.
@Composable
private fun Placeholder() {
    Surface(modifier = Modifier.fillMaxSize()) {}
}
