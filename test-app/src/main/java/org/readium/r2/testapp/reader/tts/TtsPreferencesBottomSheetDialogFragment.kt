/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import androidx.fragment.app.activityViewModels
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.reader.preferences.UserPreferencesBottomSheetDialogFragment
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel

@OptIn(ExperimentalReadiumApi::class)
class TtsPreferencesBottomSheetDialogFragment : UserPreferencesBottomSheetDialogFragment(
    "TTS Settings"
) {

    private val viewModel: ReaderViewModel by activityViewModels()

    override val preferencesModel: UserPreferencesViewModel<*, *> by lazy {
        checkNotNull(viewModel.tts!!.preferencesModel)
    }
}
