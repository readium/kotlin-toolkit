/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.util.EdgeTapNavigation
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentReaderBinding
import org.readium.r2.testapp.utils.*

/*
 * Adds fullscreen support to the BaseReaderFragment
 */
abstract class VisualReaderFragment : BaseReaderFragment(), VisualNavigator.Listener {

    private lateinit var navigatorFragment: Fragment

    private var _binding: FragmentReaderBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigatorFragment = navigator as Fragment

        childFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }
        binding.fragmentReaderContainer.setOnApplyWindowInsetsListener { container, insets ->
            updateSystemUiPadding(container, insets)
            insets
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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

    // VisualNavigator.Listener

    override fun onTap(point: PointF): Boolean {
        val navigated = edgeTapNavigation.onTap(point, requireView())
        if (!navigated) {
            requireActivity().toggleSystemUi()
        }
        return true
    }

    private val edgeTapNavigation by lazy {
        EdgeTapNavigation(
            navigator = navigator as VisualNavigator
        )
    }
}