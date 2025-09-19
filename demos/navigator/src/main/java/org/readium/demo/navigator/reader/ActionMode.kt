/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

@file:Suppress("ktlint:standard:filename")

package org.readium.demo.navigator.reader

import android.graphics.Color
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.demo.navigator.R
import org.readium.demo.navigator.decorations.Highlight
import org.readium.demo.navigator.decorations.HighlightsManager
import org.readium.navigator.common.SelectionController
import org.readium.navigator.common.SelectionLocation
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.shared.ExperimentalReadiumApi

class SelectionActionModeFactory(
    private val highlightsManager: HighlightsManager<*>,
) {

    fun createActionModeCallback(
        selectionController: SelectionController<*>,
        coroutineScope: CoroutineScope,
        onNoteAdded: (Long) -> Unit,
        onAnyHighlightAdded: () -> Unit,
    ): ActionMode.Callback = SelectionActionModeCallback(
        coroutineScope = coroutineScope,
        selectionController = selectionController,
        highlightsManager = highlightsManager,
        onNoteAdded = onNoteAdded,
        onAnyHighlightAdded = onAnyHighlightAdded
    )
}

private class SelectionActionModeCallback<S : SelectionLocation>(
    private val coroutineScope: CoroutineScope,
    private val selectionController: SelectionController<S>,
    private val highlightsManager: HighlightsManager<*>,
    private val onAnyHighlightAdded: () -> Unit,
    private val onNoteAdded: (Long) -> Unit,
) : BaseActionModeCallback() {

    private val defaultTint = Color.rgb(249, 239, 125)

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_action_mode, menu)
        menu.findItem(R.id.highlight).isVisible = true
        menu.findItem(R.id.underline).isVisible = true
        menu.findItem(R.id.note).isVisible = true
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        coroutineScope.launch {
            val selection = selectionController.currentSelection() ?: return@launch
            val locator = selection.location.toLocator()

            when (item.itemId) {
                R.id.highlight -> {
                    highlightsManager.addHighlight(
                        locator = locator,
                        style = Highlight.Style.HIGHLIGHT,
                        tint = defaultTint
                    )
                }
                R.id.underline -> {
                    highlightsManager.addHighlight(
                        locator = locator,
                        style = Highlight.Style.UNDERLINE,
                        tint = defaultTint
                    )
                }
                R.id.note -> {
                    val highlightId = highlightsManager.addHighlight(
                        locator = locator,
                        style = Highlight.Style.HIGHLIGHT,
                        tint = defaultTint
                    )
                    onNoteAdded(highlightId)
                }
                else -> throw IllegalStateException()
            }

            onAnyHighlightAdded()
        }
        mode.finish()
        return true
    }
}
