/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:Suppress("DEPRECATION")

package org.readium.r2.testapp.audiobook

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAudiobookBinding
import org.readium.r2.testapp.utils.createFragmentFactory

class AudioNavigatorFragment(
    val publication: Publication,
    private var initialLocator: Locator? = null,
    internal val listener: Listener? = null
) : Fragment(), Navigator {

    interface Listener : Navigator.Listener

    private lateinit var activity: AudiobookActivity

    private var _binding: FragmentAudiobookBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getParcelable<Locator>("locator")?.let {
            initialLocator = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudiobookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity() as AudiobookActivity

        // Setting cover
        viewLifecycleOwner.lifecycleScope.launch {
            publication.cover()?.let {
                binding.imageView.setImageBitmap(it)
            }
        }

        // TODO Replace or add a ProgressBar in the navigator to use here, then remove the deprecation annotation for this file
        activity.mediaPlayer.progress = ProgressDialog.show(requireContext(), null, getString(R.string.progress_wait_while_preparing_audiobook), true)
        initialLocator?.let { go(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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