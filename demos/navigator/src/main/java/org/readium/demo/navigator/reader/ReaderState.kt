/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.readium.demo.navigator.decorations.HighlightsManager
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.LocatorAdapter
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.SelectionLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl

data class ReaderState<L : Location, G : GoLocation, S : SelectionLocation, N : NavigationController<L, G>>(
    val url: AbsoluteUrl,
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val renditionState: RenditionState<N>,
    val preferencesEditor: PreferencesEditor<*, *>,
    val locatorAdapter: LocatorAdapter<L, G, S>,
    val onControllerAvailable: (N) -> Unit,
    val highlightsManager: HighlightsManager? = null,
) {

    fun close() {
        coroutineScope.cancel()
        publication.close()
    }
}
