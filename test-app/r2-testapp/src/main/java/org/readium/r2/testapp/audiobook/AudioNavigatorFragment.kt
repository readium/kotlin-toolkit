/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.audiobook

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_audiobook.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.anko.support.v4.indeterminateProgressDialog
import org.readium.r2.navigator.Navigator
import org.readium.r2.testapp.utils.createFragmentFactory
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R

class AudioNavigatorFragment(
    val publication: Publication,
    private var initialLocator: Locator? = null,
    internal val listener: Listener? = null
) : Fragment(R.layout.fragment_audiobook), Navigator {

    interface Listener : Navigator.Listener

    private lateinit var activity: AudiobookActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getParcelable<Locator>("locator")?.let {
            initialLocator = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity() as AudiobookActivity

        // Setting cover
        viewLifecycleOwner.lifecycleScope.launch {
            publication.cover()?.let {
                imageView.setImageBitmap(it)
            }
        }

        activity.mediaPlayer.progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_audiobook))
        initialLocator?.let { go(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.mediaPlayer.progress!!.dismiss()
    }

    override val currentLocator: StateFlow<Locator>
        get() = activity.currentLocator

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean =
        activity.go(locator, animated, completion)

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        activity.go(link, animated, completion)

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean =
        activity.goForward(animated, completion)

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean =
        activity.goBackward(animated, completion)

    companion object {

        fun createFactory(publication: Publication, initialLocator: Locator? = null, listener: Listener? = null): FragmentFactory =
            createFragmentFactory { AudioNavigatorFragment(publication, initialLocator, listener) }
    }
}