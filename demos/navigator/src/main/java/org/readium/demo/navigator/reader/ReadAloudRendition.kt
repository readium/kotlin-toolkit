/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)

package org.readium.demo.navigator.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.readium.demo.navigator.preferences.UserPreferences
import org.readium.navigator.media.readaloud.ReadAloudTextHighlightLocation
import org.readium.r2.shared.ExperimentalReadiumApi

@Composable
fun ReadAloudRendition(
    readerState: ReadAloudReaderState,
) {
    val showPreferences = remember { mutableStateOf(false) }
    val preferencesSheetState = rememberModalBottomSheetState()

    if (showPreferences.value) {
        ModalBottomSheet(
            sheetState = preferencesSheetState,
            onDismissRequest = {
                showPreferences.value = false
            }
        ) {
            val preferencesEditor = readerState.preferencesEditor.collectAsState()

            UserPreferences(
                editor = preferencesEditor.value,
                title = "Preferences"
            )
        }
    }

    val showOutline = rememberSaveable { mutableStateOf(false) }

    if (showOutline.value) {
        Outline(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxSize(),
            publication = readerState.publication,
            onBackActivated = {
                showOutline.value = false
            },
            onTocItemActivated = {
                readerState.navigator.goTo(it)
                showOutline.value = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            RenditionTopBar(
                modifier = Modifier.zIndex(10f),
                visible = true,
                onPreferencesActivated = { showPreferences.value = !showPreferences.value },
                onOutlineActivated = { showOutline.value = !showOutline.value }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.Top)
            ) {
                val playbackState = readerState.navigator.playback.collectAsState()

                Text("Playback State: ${playbackState.value.state}")

                Text("Play When Ready: ${playbackState.value.playWhenReady}")

                when (val highlightLocation = playbackState.value.nodeHighlightLocation) {
                    is ReadAloudTextHighlightLocation -> {
                        Text("Node Highlight Href: ${highlightLocation.href}")

                        Text("Node Highlight Css Selector ${highlightLocation.cssSelector?.value}")

                        Text("Node Highlight Text ${highlightLocation.textQuote?.text}")
                    }
                    null -> {}
                }
            }

            Toolbar(readerState)
        }
    }
}

@Composable
private fun Toolbar(
    readerState: ReadAloudReaderState,
) {
    val playbackState = readerState.navigator.playback.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        IconButton(
            onClick = { readerState.navigator.skipToPrevious(force = true) }
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Skip to previous"
            )
        }

        if (playbackState.value.playWhenReady) {
            IconButton(
                onClick = {
                    readerState.navigator.pause()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause"
                )
            }
        } else {
            IconButton(
                onClick = {
                    readerState.navigator.play()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play"
                )
            }
        }

        IconButton(
            onClick = { readerState.navigator.skipToNext(force = true) }
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip to next"
            )
        }

        IconButton(
            onClick = { readerState.navigator.escape(force = true) }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = "Escape"
            )
        }
    }
}
