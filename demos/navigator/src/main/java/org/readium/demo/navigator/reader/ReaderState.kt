/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import org.readium.demo.navigator.decorations.HighlightsManager
import org.readium.demo.navigator.preferences.ReadAloudPreferencesEditor
import org.readium.navigator.common.ExportableLocation
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.SelectionController
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.media.readaloud.ReadAloudNavigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl

sealed interface ReaderState {

    fun close()
}

data class VisualReaderState<L : ExportableLocation, G : GoLocation, S : SelectionLocation, C>(
    val url: AbsoluteUrl,
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val renditionState: RenditionState<C>,
    val preferencesEditor: PreferencesEditor<*, *>,
    val highlightsManager: HighlightsManager<*>,
    val onControllerAvailable: (C) -> Unit,
    val actionModeFactory: SelectionActionModeFactory,
) : ReaderState where C : NavigationController<L, G>, C : SelectionController<S> {

    override fun close() {
        coroutineScope.cancel()
        publication.close()
    }
}

data class ReadAloudReaderState(
    val url: AbsoluteUrl,
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val navigator: ReadAloudNavigator,
    val preferencesEditor: StateFlow<ReadAloudPreferencesEditor>,
) : ReaderState {

    override fun close() {
        navigator.release()
        coroutineScope.cancel()
        publication.close()
    }
}
