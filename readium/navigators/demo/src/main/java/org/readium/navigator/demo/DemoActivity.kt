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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.readium.navigator.demo.preferences.UserPreferences
import org.readium.navigator.web.PrepaginatedWebNavigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.toAbsoluteUrl

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
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

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val preferencesSheetState = rememberModalBottomSheetState()
                var showPreferences by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopBar(
                            onPreferencesActivated = {
                                showPreferences = !showPreferences
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->

                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        propagateMinConstraints = true
                    ) {
                        val state = viewModel.state.collectAsState()

                        when (val stateNow = state.value) {
                            DemoViewModel.State.BookSelection -> {
                                sharedStoragePickerLauncher.launch(arrayOf("*/*"))
                            }
                            is DemoViewModel.State.Error -> {
                                LaunchedEffect(stateNow.error) {
                                    snackbarHostState.showSnackbar(stateNow.error.message)
                                    viewModel.acknowledgeError()
                                }
                            }
                            DemoViewModel.State.Loading -> {
                                // Display nothing
                            }
                            is DemoViewModel.State.Reader -> {
                                if (showPreferences) {
                                    ModalBottomSheet(
                                        sheetState = preferencesSheetState,
                                        onDismissRequest = { showPreferences = false }
                                    ) {
                                        UserPreferences(
                                            model = stateNow.preferencesViewModel,
                                            title = "Preferences"
                                        )
                                    }
                                }

                                PrepaginatedWebNavigator(
                                    modifier = Modifier.fillMaxSize(),
                                    state = stateNow.navigatorState
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        onPreferencesActivated: () -> Unit
    ) {
        CenterAlignedTopAppBar(
            title = {},
            actions = {
                IconButton(
                    onClick = onPreferencesActivated
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Preferences"
                    )
                }
            }
        )
    }
}
