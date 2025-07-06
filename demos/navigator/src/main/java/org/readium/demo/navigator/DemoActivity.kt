/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import org.readium.demo.navigator.theme.Theme
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
                val snackbarHostState = remember { SnackbarHostState() }
                val insetsController = remember {
                    WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    )
                }

                Scaffold(
                    fullscreenState = fullscreenState,
                    insetsController = insetsController,
                    snackbarHostState = snackbarHostState
                ) {
                    MainContent(
                        viewmodel = viewModel,
                        launchBookSelection = { sharedStoragePickerLauncher.launch(arrayOf("*/*")) },
                        snackbarHostState = snackbarHostState,
                        fullscreenState = fullscreenState
                    )
                }
            }
        }
    }
}
