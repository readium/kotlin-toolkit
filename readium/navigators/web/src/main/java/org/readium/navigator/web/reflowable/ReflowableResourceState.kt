/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.r2.shared.util.Url

@Stable
internal class ReflowableResourceState(
    val index: Int,
    val href: Url,
    initialProgression: Progression,
) {

    var progression: Progression = initialProgression

    val scrollController: MutableState<WebViewScrollController?> = mutableStateOf(null)

    var scrollableState: WebViewScrollable2DState? = null
}

internal sealed interface Progression {

    val ratio: Double
}

internal data class RatioProgression(override val ratio: Double) : Progression

internal data object EndProgression : Progression {

    override val ratio: Double = 1.0
}
