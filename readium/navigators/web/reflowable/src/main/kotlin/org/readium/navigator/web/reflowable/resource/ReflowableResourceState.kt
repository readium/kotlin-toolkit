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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import org.readium.navigator.common.Progression
import org.readium.navigator.web.internals.pager.PageScrollState
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url
import timber.log.Timber

@Stable
internal class ReflowableResourceState(
    val index: Int,
    val href: Url,
    initialLocation: ReflowableResourceLocation,
) : PageScrollState {

    private var pendingGoMutable by mutableStateOf<PendingGo?>(PendingGo(initialLocation))

    private var lastComputedProgressionRange: ClosedRange<Progression>? = null

    val pendingLocation: ReflowableResourceLocation? get() =
        pendingGoMutable?.location

    val progressionRange: ClosedRange<Progression>? get() =
        lastComputedProgressionRange

    fun go(location: ReflowableResourceLocation, continuation: Continuation<Unit>?) {
        pendingGoMutable = PendingGo(location, continuation)
    }

    fun acknowledgePendingLocation(location: ReflowableResourceLocation) {
        val pendingGoNow = pendingGoMutable
        if (location != pendingGoNow?.location) {
            return
        }
        pendingGoMutable = null
        pendingGoNow.continuation?.resume(Unit)
    }

    fun updateProgression(
        orientation: Orientation,
        direction: LayoutDirection,
    ) {
        val scrollController = scrollController.value ?: return

        // Do not trust computed progressions to be between 0 and 1.
        // Sometimes scrollX is far higher than maxScrollX.

        val startProgression = scrollController.startProgression(
            orientation = orientation,
            direction = direction
        )?.let { Progression(it) }
            ?: return

        val endProgression = scrollController.endProgression(
            orientation = orientation,
            direction = direction
        )?.let { Progression(it) } ?: return

        Timber.d("updateProgression $startProgression $endProgression")

        lastComputedProgressionRange = startProgression..endProgression
    }

    override val scrollController: MutableState<WebViewScrollController?> =
        mutableStateOf(null)
}

internal sealed interface ReflowableResourceLocation {

    data class Progression(
        val value: org.readium.navigator.common.Progression,
    ) : ReflowableResourceLocation

    data class HtmlId(
        val value: org.readium.navigator.common.HtmlId,
    ) : ReflowableResourceLocation
}

internal data class PendingGo(
    val location: ReflowableResourceLocation,
    val continuation: Continuation<Unit>? = null,
)
