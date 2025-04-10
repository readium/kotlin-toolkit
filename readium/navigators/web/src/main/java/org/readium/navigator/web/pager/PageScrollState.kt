/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.runtime.MutableState
import org.readium.navigator.web.webview.WebViewScrollController

internal interface PageScrollState {

    val scrollController: MutableState<WebViewScrollController?>
}
