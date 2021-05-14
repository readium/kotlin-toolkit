/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.navigator.databinding.ViewpagerFragmentCbzBinding
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import kotlin.coroutines.CoroutineContext


class R2CbzPageFragment(private val publication: Publication, private val onTapListener: (Float, Float) -> Unit)
    : androidx.fragment.app.Fragment(), CoroutineScope  {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val link: Link
        get() = requireArguments().getParcelable("link")!!

    private lateinit var containerView: View
    private lateinit var photoView: PhotoView

    private var _binding: ViewpagerFragmentCbzBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = ViewpagerFragmentCbzBinding.inflate(inflater, container, false)
        containerView = binding.root
        photoView = binding.imageView
        photoView.setOnViewTapListener { _, x, y -> onTapListener(x, y) }

        setupPadding()

       launch {
           publication.get(link)
               .read()
               .getOrNull()
               ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
               ?.let { photoView.setImageBitmap(it) }
       }

       return containerView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPadding() {
        updatePadding()

        // Update padding when the window insets change, for example when the navigation and status
        // bars are toggled.
        ViewCompat.setOnApplyWindowInsetsListener(containerView) { _, insets ->
            updatePadding()
            insets
        }
    }

    private fun updatePadding() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            val window = activity?.window ?: return@launchWhenResumed
            var top = 0
            var bottom = 0

            // Add additional padding to take into account the display cutout, if needed.
            if (
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P &&
                window.attributes.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            ) {
                // Request the display cutout insets from the decor view because the ones given by
                // setOnApplyWindowInsetsListener are not always correct for preloaded views.
                window.decorView.rootWindowInsets?.displayCutout?.let { displayCutoutInsets ->
                    top += displayCutoutInsets.safeInsetTop
                    bottom += displayCutoutInsets.safeInsetBottom
                }
            }

            photoView.setPadding(0, top, 0, bottom)
        }
    }

}


