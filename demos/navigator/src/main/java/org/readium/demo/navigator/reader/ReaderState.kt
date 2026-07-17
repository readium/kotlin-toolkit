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
import org.readium.demo.navigator.preferences.PreferencesViewModel
import org.readium.navigator.common.ExportableLocation
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.PreferencesController
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.SelectionController
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.common.Settings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl

data class ReaderState<L : ExportableLocation, G : GoLocation, Sel : SelectionLocation, P : Preferences<P>, Set : Settings, C>(
    val url: AbsoluteUrl,
    val coroutineScope: CoroutineScope,
    val publication: Publication,
    val renditionState: RenditionState<C>,
    val highlightsManager: HighlightsManager<*, G>,
    val onControllerAvailable: (C) -> Unit,
    val createPreferencesViewModel: (C) -> PreferencesViewModel<P, Set>,
    val actionModeFactory: SelectionActionModeFactory,
) where C : NavigationController<L, G>, C : SelectionController<Sel>, C : PreferencesController<P, Set> {

    fun close() {
        coroutineScope.cancel()
        publication.close()
    }
}
