/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.shared.views

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Sets the emphasis (alpha) of a group of [Composable] views.
 */
@Composable
fun Group(lowEmphasis: Boolean = false, enabled: Boolean = true, content: @Composable () -> Unit) {
    val alpha = when {
        !enabled -> ContentAlpha.disabled
        lowEmphasis -> ContentAlpha.medium
        else -> ContentAlpha.high
    }

    CompositionLocalProvider(LocalContentAlpha provides alpha, content = content)
}
