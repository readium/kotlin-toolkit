/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.settings

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.compose.ComposeBottomSheetDialogFragment

@OptIn(ExperimentalPresentation::class)
class UserSettingsBottomSheetDialogFragment : ComposeBottomSheetDialogFragment() {

    private val model: ReaderViewModel by activityViewModels()

    @Composable
    override fun Content() {
        UserSettingsView(presentation = requireNotNull(model.presentation))
    }
}
