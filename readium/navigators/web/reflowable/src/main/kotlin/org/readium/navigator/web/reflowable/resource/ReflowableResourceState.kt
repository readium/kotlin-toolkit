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

    /** This progression will be used on resource initialization if there is no pending go. */
    var currentProgression: Progression? =
        (initialLocation as? ReflowableResourceLocation.Progression)?.value ?: Progression(0.0)!!

    fun go(location: ReflowableResourceLocation, continuation: Continuation<Unit>?) {
        pendingGoMutable = PendingGo(location, continuation)
    }

    fun cancelPendingLocation(
        location: ReflowableResourceLocation,
    ) {
        pendingGoMutable?.let { pendingGoMutable ->
            if (pendingGoMutable.location == location) {
                this.pendingGoMutable = null
                pendingGoMutable.continuation?.resume(Unit)
            }
        }
    }

    fun acknowledgePendingLocation(
        location: ReflowableResourceLocation,
        orientation: Orientation,
        direction: LayoutDirection,
    ) {
        val pendingGoNow = pendingGoMutable
        if (location != pendingGoNow?.location) {
            return
        }
        pendingGoMutable = if (updateProgression(orientation, direction)) {
            null
        } else {
            pendingGoNow.copy(continuation = null)
        }

        // Resume the call even in case of failed progression update because it's not
        // clear when the progression will be updated again.
        pendingGoNow.continuation?.resume(Unit)
    }

    fun updateProgression(
        orientation: Orientation,
        direction: LayoutDirection,
    ): Boolean {
        val scrollController = scrollController.value ?: return false

        // Do not trust computed progressions to be between 0 and 1.
        // Sometimes scrollX is far higher than maxScrollX.

        val startProgression = scrollController.startProgression(
            orientation = orientation,
            direction = direction
        )?.let { Progression(it) }
            ?: return false

        val endProgression = scrollController.endProgression(
            orientation = orientation,
            direction = direction
        )?.let { Progression(it) } ?: return false

        Timber.d("updateProgression $startProgression $endProgression")

        lastComputedProgressionRange = startProgression..endProgression
        currentProgression = startProgression

        return true
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

    data class CssSelector(
        val value: org.readium.navigator.common.CssSelector,
    ) : ReflowableResourceLocation

    data class TextAnchor(
        val value: org.readium.navigator.common.TextAnchor,
    ) : ReflowableResourceLocation
}

internal data class PendingGo(
    val location: ReflowableResourceLocation,
    val continuation: Continuation<Unit>? = null,
)
