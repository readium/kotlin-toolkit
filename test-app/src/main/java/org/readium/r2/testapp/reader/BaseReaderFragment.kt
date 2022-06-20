/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
abstract class BaseReaderFragment : Fragment() {

    protected abstract val model: ReaderViewModel
    protected abstract val navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)

        model.fragmentChannel.receive(this) { event ->
            fun toast(id: Int) {
                Toast.makeText(requireContext(), getString(id), Toast.LENGTH_SHORT).show()
            }

            when (event) {
                is ReaderViewModel.FeedbackEvent.BookmarkFailed -> toast(R.string.bookmark_exists)
                is ReaderViewModel.FeedbackEvent.BookmarkSuccessfullyAdded -> (R.string.bookmark_added)
                is ReaderViewModel.FeedbackEvent.GoTo -> go(event.locator, animated = event.animated)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        model.ttsPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_reader, menu)
        menu.findItem(R.id.drm).isVisible = model.publication.lcpLicense != null
        menu.findItem(R.id.tts).isVisible = model.canUseTts
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toc -> {
                model.activityChannel.send(ReaderViewModel.Event.OpenOutlineRequested)
            }
            R.id.bookmark -> {
                model.insertBookmark(navigator.currentLocator.value)
            }
            R.id.drm -> {
                model.activityChannel.send(ReaderViewModel.Event.OpenDrmManagementRequested)
            }
            R.id.tts -> {
                model.ttsPlay(navigator)
            }
            else -> return false
        }

        return true
    }

    open fun go(locator: Locator, animated: Boolean) {
        navigator.go(locator, animated)
    }
}