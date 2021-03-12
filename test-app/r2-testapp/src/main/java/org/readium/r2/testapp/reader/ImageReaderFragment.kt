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
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.toggleSystemUi

class ImageReaderFragment : VisualReaderFragment(), ImageNavigatorFragment.Listener {

    override lateinit var model: ReaderViewModel
    override lateinit var navigator: Navigator
    private lateinit var publication: Publication
    private lateinit var persistence: BookData

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            model = it
            publication = it.publication
            persistence = it.persistence
        }

        childFragmentManager.fragmentFactory =
            ImageNavigatorFragment.createFactory(publication, persistence.savedLocation, this)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.fragment_reader_container, ImageNavigatorFragment::class.java, Bundle(), NAVIGATOR_FRAGMENT_TAG)
            }
        }
        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG)!! as Navigator
        return view
    }

    override fun onTap(point: PointF): Boolean {
        val viewWidth = requireView().width
        val leftRange = 0.0..(0.2 * viewWidth)

        when {
            leftRange.contains(point.x) -> navigator.goBackward(animated = true)
            leftRange.contains(viewWidth - point.x) -> navigator.goForward(animated = true)
            else -> requireActivity().toggleSystemUi()
        }

        return true
    }

    companion object {

        const val NAVIGATOR_FRAGMENT_TAG = "navigator"
    }
}