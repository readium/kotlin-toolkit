/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.preferences

import android.app.Dialog
import android.os.Bundle
import androidx.compose.runtime.Composable
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.readium.r2.testapp.utils.compose.ComposeBottomSheetDialogFragment

class UserPreferencesBottomSheetDialogFragment(
    private val model: UserPreferencesViewModel<*, *>,
    private val title: String
) : ComposeBottomSheetDialogFragment(
    isScrollable = true
) {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            // Reduce the dim to see the impact of the settings on the page.
            window?.setDimAmount(0.1f)

            behavior.apply {
                peekHeight = 1000
                maxHeight = 1000
            }
        }

    @Composable
    override fun Content() {
        UserPreferences(model, title)
    }
}
