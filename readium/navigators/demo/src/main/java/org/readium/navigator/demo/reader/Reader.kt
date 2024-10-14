/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.readium.navigator.common.NavigatorState
import org.readium.navigator.demo.preferences.UserPreferences
import org.readium.navigator.demo.preferences.UserPreferencesViewModel
import org.readium.navigator.pdf.PdfNavigator
import org.readium.navigator.pdf.PdfNavigatorState
import org.readium.navigator.web.FixedWebNavigator
import org.readium.navigator.web.FixedWebNavigatorState
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

data class ReaderState(
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val navigatorState: NavigatorState,
    val preferencesViewModel: UserPreferencesViewModel<*, *>
) {

    fun close() {
        coroutineScope.cancel()
        publication.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun Reader(
    state: ReaderState,
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

        val onTap = remember(fullScreenState) {
            { _: TapEvent -> fullScreenState.value = !fullScreenState.value }
        }

        when (state.navigatorState) {
            is FixedWebNavigatorState -> {
                FixedWebNavigator(
                    modifier = Modifier.fillMaxSize(),
                    state = state.navigatorState,
                    onTap = onTap
                )
            }
            is PdfNavigatorState<*, *> -> {
                PdfNavigator(
                    modifier = Modifier.fillMaxSize(),
                    state = state.navigatorState,
                    onTap = onTap
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
