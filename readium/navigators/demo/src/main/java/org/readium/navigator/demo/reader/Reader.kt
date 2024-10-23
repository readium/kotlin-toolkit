/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.Location
import org.readium.navigator.common.LocatorAdapter
import org.readium.navigator.common.Navigator
import org.readium.navigator.common.Overflowable
import org.readium.navigator.common.ReadingOrder
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.demo.persistence.LocatorRepository
import org.readium.navigator.demo.preferences.UserPreferences
import org.readium.navigator.demo.preferences.UserPreferencesViewModel
import org.readium.navigator.demo.util.launchWebBrowser
import org.readium.navigator.pdf.PdfNavigator
import org.readium.navigator.pdf.PdfNavigatorState
import org.readium.navigator.web.FixedWebNavigator
import org.readium.navigator.web.FixedWebNavigatorState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

data class ReaderState<R : ReadingOrder, L : Location>(
    val url: AbsoluteUrl,
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val navigatorState: Navigator<R, L, *>,
    val preferencesViewModel: UserPreferencesViewModel<*, *>,
    val locatorAdapter: LocatorAdapter<L, *>
) {

    fun close() {
        coroutineScope.cancel()
        publication.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <R : ReadingOrder, L : Location> Reader(
    state: ReaderState<R, L>,
    fullScreenState: MutableState<Boolean>
) {
    val showPreferences = remember { mutableStateOf(false) }
    val preferencesSheetState = rememberModalBottomSheetState()

    if (showPreferences.value) {
        ModalBottomSheet(
            sheetState = preferencesSheetState,
            onDismissRequest = { showPreferences.value = false }
        ) {
            UserPreferences(
                model = state.preferencesViewModel,
                title = "Preferences"
            )
        }
    }

    Box {
        TopBar(
            modifier = Modifier.zIndex(1f),
            visible = !fullScreenState.value,
            onPreferencesActivated = { showPreferences.value = !showPreferences.value }
        )

        val fallbackInputListener = remember {
            object : InputListener {
                override fun onTap(event: TapEvent, context: TapContext) {
                    fullScreenState.value = !fullScreenState.value
                }
            }
        }

        val inputListener =
            if (state.navigatorState is Overflowable) {
                defaultInputListener(
                    navigatorState = state.navigatorState,
                    fallbackListener = fallbackInputListener
                )
            } else {
                fallbackInputListener
            }

        val context = LocalContext.current

        val hyperlinkListener =
            defaultHyperlinkListener(
                navigatorState = state.navigatorState,
                onExternalLinkActivated = { url, _ -> launchWebBrowser(context, url.toUri()) }
            )

        val locationFlow = remember {
            snapshotFlow {
                state.navigatorState.location.value
            }
        }

        LaunchedEffect(locationFlow) {
            locationFlow
                .onEach {
                    val locator = with(state.locatorAdapter) { it.toLocator() }
                    LocatorRepository.saveLocator(state.url, locator)
                }
                .launchIn(state.coroutineScope)
        }

        when (state.navigatorState) {
            is FixedWebNavigatorState -> {
                FixedWebNavigator(
                    modifier = Modifier.fillMaxSize(),
                    state = state.navigatorState,
                    inputListener = inputListener,
                    hyperlinkListener = hyperlinkListener
                )
            }
            is PdfNavigatorState<*, *> -> {
                PdfNavigator(
                    modifier = Modifier.fillMaxSize(),
                    state = state.navigatorState,
                    inputListener = inputListener
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier,
    visible: Boolean,
    onPreferencesActivated: () -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TopAppBar(
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
