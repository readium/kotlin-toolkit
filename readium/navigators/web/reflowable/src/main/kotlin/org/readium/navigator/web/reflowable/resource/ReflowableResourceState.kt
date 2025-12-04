/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable.resource

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.LayoutDirection
import org.readium.navigator.common.Progression
import org.readium.navigator.web.internals.pager.PageScrollState
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@Stable
internal class ReflowableResourceState(
    val index: Int,
    val href: Url,
    initialProgression: Progression,
) : PageScrollState {

    var startProgression: Progression = initialProgression
        private set
    private var lastComputedProgressionRange: ClosedRange<Progression>? = null

    val progressionRange: ClosedRange<Progression>? get() =
        lastComputedProgressionRange

    fun updateProgression(
        startProgression: Progression,
        orientation: Orientation,
        direction: LayoutDirection,
    ) {
        this.startProgression = startProgression
        updateProgressionRange(startProgression, orientation, direction)
    }

    private fun updateProgressionRange(
        start: Progression,
        orientation: Orientation,
        direction: LayoutDirection,
    ) {
        val end = scrollController.value?.endProgression(
            orientation = orientation,
            direction = direction
        )?.let { Progression(it) }
        end?.let { lastComputedProgressionRange = start..end }
    }

    override val scrollController: MutableState<WebViewScrollController?> = mutableStateOf(null)
}
