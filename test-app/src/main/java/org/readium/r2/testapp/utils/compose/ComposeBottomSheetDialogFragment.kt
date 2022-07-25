/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * A bottom sheet whose content is built using Jetpack Compose.
 *
 * @param initialState BottomSheetBehavior state to use by default.
 */
abstract class ComposeBottomSheetDialogFragment(
    val initialState: Int? = null
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    this@ComposeBottomSheetDialogFragment.Content()
                }
            }
        }

    override fun onStart() {
        super.onStart()

        if (initialState != null) {
            (dialog as? BottomSheetDialog)?.behavior?.state = initialState
        }
    }

    @Composable
    abstract fun Content()
}
