/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.web.pager.PageScrollState
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.r2.shared.util.Url

@Stable
internal class ReflowableResourceState(
    val index: Int,
    val href: Url,
    var progression: Double,
) : PageScrollState {

    override val scrollController: MutableState<WebViewScrollController?> = mutableStateOf(null)
}
