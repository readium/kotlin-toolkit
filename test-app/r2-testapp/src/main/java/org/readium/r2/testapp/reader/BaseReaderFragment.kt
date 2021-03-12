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
import androidx.fragment.app.Fragment
import org.jetbrains.anko.support.v4.toast
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
abstract class BaseReaderFragment : Fragment(R.layout.fragment_reader) {

    protected abstract var model: ReaderViewModel
    protected abstract var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onStop() {
        model.persistence.savedLocation = navigator.currentLocator.value
        super.onStop()
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
                val added = model.persistence.addBookmark(navigator.currentLocator.value)
                toast(if (added) "Bookmark added" else "Bookmark already exists")
                true
            }
            R.id.drm -> {
                model.channel.send(ReaderViewModel.Event.OpenDrmManagementRequested)
                true
            }
            else -> false
        }
    }

    fun go(locator: Locator, animated: Boolean) =
        navigator.go(locator, animated)
}