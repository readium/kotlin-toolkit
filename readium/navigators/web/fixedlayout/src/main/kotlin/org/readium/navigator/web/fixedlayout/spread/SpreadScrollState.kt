/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.spread

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.web.internals.pager.PageScrollState
import org.readium.navigator.web.internals.webview.WebViewScrollController

@Stable
internal class SpreadScrollState : PageScrollState {

    override val scrollController: MutableState<WebViewScrollController?> = mutableStateOf(null)
}
