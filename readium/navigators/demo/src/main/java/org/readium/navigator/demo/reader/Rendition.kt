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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.Location
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.demo.R
import org.readium.navigator.demo.persistence.LocatorRepository
import org.readium.navigator.demo.preferences.UserPreferences
import org.readium.navigator.demo.util.launchWebBrowser
import org.readium.navigator.web.FixedWebRendition
import org.readium.navigator.web.FixedWebRenditionState
import org.readium.navigator.web.ReflowableWebRendition
import org.readium.navigator.web.ReflowableWebRenditionState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <L : Location, G : GoLocation, N : NavigationController<L, G>> Reader(
    readerState: ReaderState<L, G, N>,
    fullScreenState: MutableState<Boolean>,
) {
    val coroutineScope = rememberCoroutineScope()

    val showPreferences = remember { mutableStateOf(false) }
    val preferencesSheetState = rememberModalBottomSheetState()

    if (showPreferences.value) {
        ModalBottomSheet(
            sheetState = preferencesSheetState,
            onDismissRequest = {
                showPreferences.value = false
            }
        ) {
            UserPreferences(
                editor = readerState.preferencesEditor,
                title = "Preferences"
            )
        }
    }

    val showOutline = rememberSaveable { mutableStateOf(false) }

    if (showOutline.value) {
        Outline<G>(
            modifier = Modifier.fillMaxSize(),
            publication = readerState.publication,
            locatorAdapter = readerState.locatorAdapter,
            onBackActivated = {
                showOutline.value = false
                fullScreenState.value = true
            },
            onTocItemActivated = {
                val controllerNow = readerState.renditionState.controller
                    ?: return@Outline

                coroutineScope.launch {
                    controllerNow.goTo(it)
                }

                fullScreenState.value = true
                showOutline.value = false
            }
        )
    }

    Box {
        TopBar(
            modifier = Modifier.zIndex(10f),
            visible = !fullScreenState.value,
            onPreferencesActivated = { showPreferences.value = !showPreferences.value },
            onOutlineActivated = { showOutline.value = !showOutline.value }
        )

        val navigationHistory: MutableState<List<L>> = remember { mutableStateOf(emptyList()) }

        val controllerNow = readerState.renditionState.controller

        if (controllerNow != null) {
            LaunchedEffect(controllerNow) {
                readerState.onControllerAvailable(controllerNow)
            }

            LaunchedEffect(controllerNow) {
                snapshotFlow {
                    controllerNow.location.value
                }.onEach {
                    val locator = with(readerState.locatorAdapter) { it.toLocator() }
                    LocatorRepository.saveLocator(readerState.url, locator)
                }.launchIn(readerState.coroutineScope)
            }

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
            (controllerNow as? OverflowController)?.let {
                defaultInputListener(
                    controller = it,
                    fallbackListener = fallbackInputListener
                )
            } ?: fallbackInputListener

        val context = LocalContext.current

        val hyperlinkListener =
            defaultHyperlinkListener(
                controller = controllerNow,
                shouldFollowReadingOrderLink = { _, _ ->
                    navigationHistory.value += location.value
                    true
                },
                onExternalLinkActivated = { url, _ -> launchWebBrowser(context, url.toUri()) }
            )

        when (readerState.renditionState) {
            is FixedWebRenditionState -> {
                FixedWebRendition(
                    modifier = Modifier.fillMaxSize(),
                    state = readerState.renditionState,
                    inputListener = inputListener,
                    hyperlinkListener = hyperlinkListener
                )
            }
            is ReflowableWebRenditionState -> {
                ReflowableWebRendition(
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
    onPreferencesActivated: () -> Unit,
    onOutlineActivated: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TopAppBar(
            title = { },
            actions = {
                IconButton(
                    onClick = onPreferencesActivated
                ) {
                    Icon(
                        painterResource(R.drawable.ic_preferences_24),
                        contentDescription = "Preferences",
                    )
                }
                IconButton(
                    onClick = onOutlineActivated
                ) {
                    Icon(
                        painterResource(R.drawable.ic_outline_24),
                        contentDescription = "Outline"
                    )
                }
            }
        )
    }
}
