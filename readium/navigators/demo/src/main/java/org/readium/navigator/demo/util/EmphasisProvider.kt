/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

val LocalContentEmphasis = compositionLocalOf { Emphasis.Medium }

enum class Emphasis {
    Disabled,
    Medium,
    High
}

@Composable
fun EmphasisProvider(emphasis: ProvidedValue<Emphasis>, content: @Composable () -> Unit) {
    val contentColor = when (emphasis.value) {
        Emphasis.Disabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        Emphasis.Medium -> MaterialTheme.colorScheme.onSurfaceVariant
        Emphasis.High -> MaterialTheme.colorScheme.onSurface
    }
    val fontWeight = when (emphasis.value) {
        Emphasis.High -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        content = {
            ProvideTextStyle(value = TextStyle(fontWeight = fontWeight)) {
                content()
            }
        }
    )
}
