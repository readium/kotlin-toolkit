/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import org.readium.navigator.demo.reader.Reader
import org.readium.navigator.demo.util.Fullscreenable
import org.readium.navigator.demo.util.Theme
import org.readium.r2.shared.util.toAbsoluteUrl

class DemoActivity : FragmentActivity() {

    private val viewModel: DemoViewModel by viewModels()

    private val sharedStoragePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                finish()
            } else {
                val url = requireNotNull(uri.toAbsoluteUrl())
                viewModel.onBookSelected(url)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme {
                val fullscreenState = remember { mutableStateOf(false) }

                Fullscreenable(
                    fullscreenState = fullscreenState,
                    insetsController = WindowCompat.getInsetsController(window, window.decorView)
                ) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val state = viewModel.state.collectAsState()

                    LaunchedEffect(state.value) {
                        fullscreenState.value = when (state.value) {
                            DemoViewModel.State.BookSelection -> true
                            is DemoViewModel.State.Error -> false
                            DemoViewModel.State.Loading -> true
                            is DemoViewModel.State.Reader -> true
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (val stateNow = state.value) {
                            DemoViewModel.State.BookSelection -> {
                                Placeholder()
                                LaunchedEffect(stateNow) {
                                    sharedStoragePickerLauncher.launch(arrayOf("*/*"))
                                }
                            }

                            is DemoViewModel.State.Error -> {
                                Placeholder()
                                LaunchedEffect(stateNow.error) {
                                    snackbarHostState.showSnackbar(
                                        message = stateNow.error.message,
                                        duration = SnackbarDuration.Short
                                    )
                                    viewModel.onErrorDisplayed()
                                }
                            }

                            DemoViewModel.State.Loading -> {
                                Placeholder()
                                // Display and do nothing
                            }

                            is DemoViewModel.State.Reader -> {
                                BackHandler {
                                    viewModel.onBookClosed()
                                }

                                Reader(
                                    readerState = stateNow.readerState,
                                    fullScreenState = fullscreenState
                                )
                            }
                        }

                        SnackbarHost(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .safeDrawingPadding(),
                            hostState = snackbarHostState
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
}
