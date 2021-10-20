package org.readium.r2.testapp.utils.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class ComposeBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    this@ComposeBottomSheetDialogFragment.Content()
                }
            }
        }

    @Composable
    abstract fun Content()
}