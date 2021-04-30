/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.clearPadding
import org.readium.r2.testapp.utils.hideSystemUi
import org.readium.r2.testapp.utils.padSystemUi
import org.readium.r2.testapp.utils.showSystemUi

/*
 * Adds fullscreen support to the BaseReaderFragment
 */
abstract class VisualReaderFragment : BaseReaderFragment() {

    private lateinit var navigatorFragment: Fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigatorFragment = navigator as Fragment

        childFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }
        view.findViewById<FrameLayout>(R.id.fragment_reader_container).setOnApplyWindowInsetsListener { container, insets ->
            updateSystemUiPadding(container, insets)
            insets
        }
    }

    fun updateSystemUiVisibility() {
        if (navigatorFragment.isHidden)
            requireActivity().showSystemUi()
        else
            requireActivity().hideSystemUi()

        requireView().requestApplyInsets()
    }

    private fun updateSystemUiPadding(container: View, insets: WindowInsets) {
        if (navigatorFragment.isHidden) {
            container.padSystemUi(insets, requireActivity())
        } else {
            container.clearPadding()
        }
    }
}