/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.*
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentReaderBinding
import org.readium.r2.testapp.domain.model.Highlight
import timber.log.Timber

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalDecorator::class)
abstract class BaseReaderFragment : Fragment() {

    protected abstract val model: ReaderViewModel
    protected abstract val navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)

        model.fragmentChannel.receive(this) { event ->
            val message =
                when (event) {
                    is ReaderViewModel.FeedbackEvent.BookmarkFailed -> R.string.bookmark_exists
                    is ReaderViewModel.FeedbackEvent.BookmarkSuccessfullyAdded -> R.string.bookmark_added
                }
            Toast.makeText(requireContext(), getString(message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_reader, menu)
        menu.findItem(R.id.drm).isVisible = model.publication.lcpLicense != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toc -> {
                model.channel.send(ReaderViewModel.Event.OpenOutlineRequested)
                true
            }
            R.id.bookmark -> {
                model.insertBookmark(navigator.currentLocator.value)
                true
            }
            R.id.drm -> {
                model.channel.send(ReaderViewModel.Event.OpenDrmManagementRequested)
                true
            }
            else -> false
        }
    }

    open fun go(locator: Locator, animated: Boolean) {
        navigator.go(locator, animated)
    }
}