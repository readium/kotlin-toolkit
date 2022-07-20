/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.settings

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.compose.ComposeBottomSheetDialogFragment

class UserSettingsBottomSheetDialogFragment : ComposeBottomSheetDialogFragment() {

    private val model: ReaderViewModel by activityViewModels()

    @Composable
    override fun Content() {
        UserSettings(model)
    }
}
