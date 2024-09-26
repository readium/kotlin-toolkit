/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import org.readium.navigator.demo.util.Fullscreenable
import org.readium.navigator.demo.util.Theme
import org.readium.r2.shared.util.toAbsoluteUrl

class DemoActivity : ComponentActivity() {

    private val viewModel: DemoViewModel by viewModels()

    private val sharedStoragePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val url = requireNotNull(it.toAbsoluteUrl())
                viewModel.open(url)
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

                    when (val stateNow = state.value) {
                        DemoViewModel.State.BookSelection -> {
                            LaunchedEffect(stateNow) {
                                sharedStoragePickerLauncher.launch(arrayOf("*/*"))
                            }
                        }

                        is DemoViewModel.State.Error -> {
                            LaunchedEffect(stateNow.error) {
                                snackbarHostState.showSnackbar(stateNow.error.message)
                                viewModel.acknowledgeError()
                            }
                        }

                        DemoViewModel.State.Loading -> {
                            // Display and do nothing
                        }

                        is DemoViewModel.State.Reader -> {
                            Reader(
                                state = stateNow,
                                fullScreenState = fullscreenState
                            )
                        }
                    }
                }
            }
        }
    }
}
