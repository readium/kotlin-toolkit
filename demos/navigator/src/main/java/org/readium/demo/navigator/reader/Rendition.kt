/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.demo.navigator.R
import org.readium.demo.navigator.decorations.DecorationStylePageNumber
import org.readium.demo.navigator.decorations.EditAnnotationDialog
import org.readium.demo.navigator.decorations.EditAnnotationViewModel
import org.readium.demo.navigator.decorations.EditHighlightPopup
import org.readium.demo.navigator.decorations.EditHighlightViewModel
import org.readium.demo.navigator.persistence.LocatorRepository
import org.readium.demo.navigator.preferences.UserPreferences
import org.readium.demo.navigator.util.launchWebBrowser
import org.readium.navigator.common.DecorationController
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.Location
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.SelectionController
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.fixedlayout.FixedWebRendition
import org.readium.navigator.web.fixedlayout.FixedWebRenditionState
import org.readium.navigator.web.reflowable.ReflowableWebRendition
import org.readium.navigator.web.reflowable.ReflowableWebRenditionState
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.util.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <L : Location, G : GoLocation, S : SelectionLocation, N : NavigationController<L, G>> Reader(
    readerState: ReaderState<L, G, S, N>,
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
                    controllerNow.location
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
                    navigationHistory.value += location
                    true
                },
                onExternalLinkActivated = { url, _ -> launchWebBrowser(context, url.toUri()) }
            )

        LaunchedEffect(controllerNow) {
            (controllerNow as? DecorationController)?.let { decoController ->
                readerState.highlightsManager!!.decorations
                    .onEach {
                        decoController.decorations["highlights"] = it
                    }.launchIn(coroutineScope)

                decoController.applyPageNumberDecorations(readerState.publication)
            }
        }

        val showAnnotationDialog: MutableState<EditAnnotationViewModel?> =
            remember { mutableStateOf(null) }

        val showEditHighlightPopup: MutableState<EditHighlightViewModel?> =
            remember { mutableStateOf(null) }

        showAnnotationDialog.value?.let { viewModel ->
            EditAnnotationDialog(
                viewModel = viewModel,
                onDismissRequest = { showAnnotationDialog.value = null }
            )
        }

        showEditHighlightPopup.value?.let { viewModel ->
            EditHighlightPopup(
                viewModel = viewModel,
                onDismissRequest = {
                    showEditHighlightPopup.value = null
                },
                onEditNoteRequest = {
                    showAnnotationDialog.value =
                        EditAnnotationViewModel(viewModel.id, viewModel.highlightsManager)
                    showEditHighlightPopup.value = null
                }
            )
        }

        val selectionActionMode = remember(controllerNow) {
            (controllerNow as? SelectionController<S>)
                ?.let {
                    SelectionActionModeCallback(
                        coroutineScope = coroutineScope,
                        selectionController = it,
                        highlightsManager = readerState.highlightsManager!!,
                        onNoteAdded = { id ->
                            showAnnotationDialog.value =
                                EditAnnotationViewModel(id, readerState.highlightsManager)
                        },
                        onAnyHighlightAdded = {
                            it.clearSelection()
                        },
                        locatorAdapter = readerState.locatorAdapter
                    )
                }
        }

        val decorationsListener = remember {
            object : DecorationListener {

                override fun onDecorationActivated(event: DecorationListener.OnActivatedEvent) {
                    if (event.group != "highlights") {
                        return
                    }

                    showEditHighlightPopup.value =
                        EditHighlightViewModel(
                            id = event.decoration.id.split('-').first().toLong(),
                            contentRect = event.rect!!,
                            highlightsManager = readerState.highlightsManager!!
                        )
                }
            }
        }

        when (readerState.renditionState) {
            is FixedWebRenditionState -> {
                FixedWebRendition(
                    modifier = Modifier.fillMaxSize(),
                    state = readerState.renditionState,
                    inputListener = inputListener,
                    hyperlinkListener = hyperlinkListener,
                    decorationListener = decorationsListener,
                    textSelectionActionModeCallback = selectionActionMode
                )
            }
            is ReflowableWebRenditionState -> {
                ReflowableWebRendition(
                    modifier = Modifier.fillMaxSize(),
                    state = readerState.renditionState,
                    inputListener = inputListener,
                    hyperlinkListener = hyperlinkListener,
                    decorationListener = decorationsListener,
                    textSelectionActionModeCallback = selectionActionMode
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

/**
 * Will display margin labels next to page numbers in an EPUB publication with a `page-list`
 * navigation document.
 *
 * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
 */
private fun DecorationController.applyPageNumberDecorations(
    publication: Publication,
) {
    val decorations = publication.pageList
        .mapIndexedNotNull { index, link ->
            val label = link.title ?: return@mapIndexedNotNull null
            val locator = publication.locatorFromLink(link) ?: return@mapIndexedNotNull null

            Decoration(
                id = "page-$index",
                locator = locator,
                style = DecorationStylePageNumber(label = label)
            )
        }

    this.decorations["pageNumbers"] = decorations.toImmutableList()
}
