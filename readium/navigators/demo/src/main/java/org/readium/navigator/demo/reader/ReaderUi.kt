/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo.reader

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.Location
import org.readium.navigator.common.LocatorAdapter
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.NullHyperlinkListener
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.demo.persistence.LocatorRepository
import org.readium.navigator.demo.preferences.UserPreferences
import org.readium.navigator.demo.util.launchWebBrowser
import org.readium.navigator.web.FixedWebRendition
import org.readium.navigator.web.FixedWebRenditionState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

data class ReaderState<L : Location, N : NavigationController<L, *>>(
    val url: AbsoluteUrl,
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val renditionState: RenditionState<N>,
    val preferencesEditor: PreferencesEditor<*, *>,
    val locatorAdapter: LocatorAdapter<L, *>,
    val onNavigatorCreated: (N) -> Unit
) {

    fun close() {
        coroutineScope.cancel()
        publication.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <L : Location, N : NavigationController<L, *>> Reader(
    readerState: ReaderState<L, N>,
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
                editor = readerState.preferencesEditor,
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

        val navigationHistory: MutableState<List<L>> = remember { mutableStateOf(emptyList()) }

        val controllerNow = readerState.renditionState.controller

        if (controllerNow != null) {
            LaunchedEffect(controllerNow) {
                readerState.onNavigatorCreated(controllerNow)
            }

            LaunchedEffect(controllerNow) {
                snapshotFlow {
                    controllerNow.location.value
                }.onEach {
                    val locator = with(readerState.locatorAdapter) { it.toLocator() }
                    LocatorRepository.saveLocator(readerState.url, locator)
                }.launchIn(readerState.coroutineScope)
            }

            val coroutineScope = rememberCoroutineScope()

            BackHandler(enabled = navigationHistory.value.isNotEmpty()) {
                val previousItem = navigationHistory.value.last()
                navigationHistory.value -= previousItem
                coroutineScope.launch { controllerNow.goTo(previousItem) }
            }
        }

        val fallbackInputListener = remember {
            object : InputListener {
                override fun onTap(event: TapEvent, context: TapContext) {
                    fullScreenState.value = !fullScreenState.value
                }
            }
        }

        val inputListener =
            if (controllerNow == null) {
                fallbackInputListener
            } else {
                (controllerNow as? OverflowController)?.let {
                    defaultInputListener(
                        controller = it,
                        fallbackListener = fallbackInputListener
                    )
                } ?: fallbackInputListener
            }

        val hyperlinkListener =
            if (controllerNow == null) {
                NullHyperlinkListener()
            } else {
                val context = LocalContext.current
                val onFollowingLink = { navigationHistory.value += controllerNow.location.value }

                defaultHyperlinkListener(
                    controller = controllerNow,
                    shouldFollowReadingOrderLink = { _, _ -> onFollowingLink(); true },
                    onExternalLinkActivated = { url, _ -> launchWebBrowser(context, url.toUri()) }
                )
            }

        when (readerState.renditionState) {
            is FixedWebRenditionState -> {
                FixedWebRendition(
                    modifier = Modifier.fillMaxSize(),
                    state = readerState.renditionState,
                    inputListener = inputListener,
                    hyperlinkListener = hyperlinkListener
                )
            }
            /* is PdfNavigatorState<*, *> -> {
                PdfNavigator(
                    modifier = Modifier.fillMaxSize(),
                    state = state.navigatorState,
                    inputListener = inputListener
                )
            } */
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
