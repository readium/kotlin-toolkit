/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator

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
import org.readium.demo.navigator.reader.ReadAloudReaderState
import org.readium.demo.navigator.reader.ReadAloudRendition
import org.readium.demo.navigator.reader.SelectNavigatorMenu
import org.readium.demo.navigator.reader.VisualReaderState
import org.readium.demo.navigator.reader.VisualRendition
import org.readium.demo.navigator.util.Fullscreenable

@Composable
fun Scaffold(
    fullscreenState: State<Boolean>,
    insetsController: WindowInsetsControllerCompat,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
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
    fullscreenState: MutableState<Boolean>,
) {
    val viewmodelState = viewmodel.state.collectAsState().value

    LaunchedEffect(viewmodelState) {
        fullscreenState.value = when (viewmodelState) {
            DemoViewModel.State.BookSelection -> true
            is DemoViewModel.State.Error -> false
            DemoViewModel.State.Loading -> true
            is DemoViewModel.State.Reader -> when (viewmodelState.readerState) {
                is VisualReaderState<*, *, *, *> -> true
                is ReadAloudReaderState -> false
            }
            is DemoViewModel.State.NavigatorSelection -> true
        }
    }

    when (viewmodelState) {
        DemoViewModel.State.BookSelection -> {
            Placeholder()
            LaunchedEffect(viewmodelState) {
                launchBookSelection.invoke()
            }
        }

        is DemoViewModel.State.NavigatorSelection -> {
            SelectNavigatorMenu(viewmodelState.viewModel)
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

            when (viewmodelState.readerState) {
                is ReadAloudReaderState -> {
                    ReadAloudRendition(
                        readerState = viewmodelState.readerState
                    )
                }
                is VisualReaderState<*, *, *, *> -> {
                    VisualRendition(
                        readerState = viewmodelState.readerState,
                        fullScreenState = fullscreenState
                    )
                }
            }
        }
    }
}

// This is useful for setting a background color.
@Composable
private fun Placeholder() {
    Surface(modifier = Modifier.fillMaxSize()) {}
}
