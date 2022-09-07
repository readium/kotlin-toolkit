/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.compose

import androidx.compose.runtime.Composable
import com.google.android.material.composethemeadapter.MdcTheme

/**
 * Setup the Compose app-wide theme.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MdcTheme(
        setDefaultFontFamily = true,
        content = content
    )
}
