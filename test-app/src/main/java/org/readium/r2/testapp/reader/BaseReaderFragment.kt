/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.preferences.MainPreferencesBottomSheetDialogFragment
import org.readium.r2.testapp.utils.UserError

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalReadiumApi::class)
abstract class BaseReaderFragment : Fragment() {

    val model: ReaderViewModel by activityViewModels()
    protected val publication: Publication get() = model.publication

    protected abstract val navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.fragmentChannel.receive(this) { event ->
            fun toast(id: Int) {
                Toast.makeText(requireContext(), getString(id), Toast.LENGTH_SHORT).show()
            }

            when (event) {
                is ReaderViewModel.FragmentFeedback.BookmarkFailed -> toast(
                    R.string.bookmark_exists
                )
                is ReaderViewModel.FragmentFeedback.BookmarkSuccessfullyAdded -> toast(
                    R.string.bookmark_added
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_reader, menu)

                    menu.findItem(R.id.settings).isVisible =
                        navigator is Configurable<*, *>

                    menu.findItem(R.id.drm).isVisible =
                        model.publication.lcpLicense != null
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.toc -> {
                            model.activityChannel.send(
                                ReaderViewModel.ActivityCommand.OpenOutlineRequested
                            )
                        }
                        R.id.info -> {
                            PublicationMetadataDialogFragment()
                                .show(childFragmentManager, "Info")
                        }
                        R.id.bookmark -> {
                            model.insertBookmark(navigator.currentLocator.value)
                        }
                        R.id.settings -> {
                            MainPreferencesBottomSheetDialogFragment()
                                .show(childFragmentManager, "Settings")
                        }
                        R.id.drm -> {
                            model.activityChannel.send(
                                ReaderViewModel.ActivityCommand.OpenDrmManagementRequested
                            )
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner
        )
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    open fun go(locator: Locator, animated: Boolean) {
        navigator.go(locator, animated)
    }

    protected fun showError(error: UserError) {
        val activity = activity ?: return
        error.show(activity)
    }
}
