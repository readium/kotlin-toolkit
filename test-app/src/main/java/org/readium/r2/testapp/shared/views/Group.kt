/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.shared.views

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Sets the emphasis (alpha) of a group of [Composable] views.
 */
@Composable
fun Group(lowEmphasis: Boolean = false, enabled: Boolean = true, content: @Composable () -> Unit) {
    val contentColor = when {
        !enabled -> 0.38f
        else -> 1.0f
    }
    val fontWeight = when {
        !lowEmphasis -> FontWeight.Normal
        else -> FontWeight.Bold
    }

    CompositionLocalProvider(value = LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = contentColor), content = {
        ProvideTextStyle(value = TextStyle(fontWeight = fontWeight)) {
            content()
        }
    })
}
