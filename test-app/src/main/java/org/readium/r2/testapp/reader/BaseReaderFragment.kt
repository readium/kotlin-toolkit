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
import androidx.fragment.app.activityViewModels
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.*
import org.readium.r2.navigator.presentation.PresentableNavigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.settings.UserSettingsBottomSheetDialogFragment

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalDecorator::class, ExperimentalPresentation::class)
abstract class BaseReaderFragment : Fragment() {

    protected val model: ReaderViewModel by activityViewModels()
    protected val publication: Publication get() = model.publication
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.onNavigatorCreated(navigator)
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
                model.activityChannel.send(ReaderViewModel.Event.OpenOutlineRequested)
                true
            }
            R.id.bookmark -> {
                model.insertBookmark(navigator.currentLocator.value)
                true
            }
            R.id.drm -> {
                model.activityChannel.send(ReaderViewModel.Event.OpenDrmManagementRequested)
                true
            }
            R.id.settings -> {
                onOpenSettings()
                true
            }
            else -> false
        }
    }

    protected open fun onOpenSettings() {
        if (navigator is PresentableNavigator) {
            UserSettingsBottomSheetDialogFragment().show(parentFragmentManager, "Settings")
        }
    }

    open fun go(locator: Locator, animated: Boolean) {
        navigator.go(locator, animated)
    }
}