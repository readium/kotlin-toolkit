/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.PopupProperties
import org.readium.navigator.media.readaloud.ReadAloudNavigatorFactory
import org.readium.navigator.web.fixedlayout.FixedWebRenditionFactory
import org.readium.navigator.web.reflowable.ReflowableWebRenditionFactory
import org.readium.r2.shared.ExperimentalReadiumApi

class SelectNavigatorViewModel(
    val items: List<SelectNavigatorItem>,
    val onItemSelected: (SelectNavigatorItem) -> Unit,
    val onMenuDismissed: () -> Unit,
) {

    fun select(item: SelectNavigatorItem) {
        onItemSelected(item)
    }

    fun cancel() {
        onMenuDismissed()
    }
}

sealed class SelectNavigatorItem(
    val name: String,
) {

    abstract val factory: Any

    data class ReflowableWeb(
        override val factory: ReflowableWebRenditionFactory,
    ) : SelectNavigatorItem("Reflowable Web Rendition")

    data class FixedWeb(
        override val factory: FixedWebRenditionFactory,
    ) : SelectNavigatorItem("Fixed Web Rendition")

    data class ReadAloud(
        override val factory: ReadAloudNavigatorFactory,
    ) : SelectNavigatorItem("Read Aloud Navigator")
}

@Composable
fun SelectNavigatorMenu(
    viewModel: SelectNavigatorViewModel,
) {
    SelectNavigatorMenu(
        popupProperties = PopupProperties(),
        items = viewModel.items,
        onItemSelected = { viewModel.select(it) },
        onDismissRequest = viewModel::cancel
    )
}

@Composable
private fun SelectNavigatorMenu(
    popupProperties: PopupProperties,
    items: List<SelectNavigatorItem>,
    onItemSelected: (SelectNavigatorItem) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = true,
        properties = popupProperties,
        onDismissRequest = onDismissRequest
    ) {
        for (item in items) {
            DropdownMenuItem(
                text = { Text(item.name) },
                onClick = { onItemSelected(item) }
            )
        }
    }
}
