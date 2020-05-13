/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.extensions.urlToHref
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.net.URL

class PdfNavigatorViewModel(
    private val publication: Publication,
    initialLocator: Locator? = null
) : ViewModel() {

    data class GoToLocationEvent(val href: String, val url: URL, val page: Int, val animated: Boolean)

    val goToLocation = MutableLiveData<GoToLocationEvent?>(null)

    val currentLocator: LiveData<Locator?> get() = _currentLocator
    private val _currentLocator = MutableLiveData<Locator?>(null)

    private var currentPageCount: Int? = null

    init {
        if (initialLocator != null) {
            goTo(initialLocator)
        } else {
            goTo(publication.readingOrder.first())
        }
    }

    fun goTo(locator: Locator, animated: Boolean = false, completion: () -> Unit = {}): Boolean {
        return goToHref(locator.href, locator.locations.page ?: 0, animated, completion)
    }

    fun goTo(link: Link, animated: Boolean = false, completion: () -> Unit = {}): Boolean {
        return goToHref(link.href, 0, animated, completion)
    }

    fun goToHref(href: String, page: Int, animated: Boolean = false, completion: () -> Unit = {}): Boolean {
        val url = publication.urlToHref(href) ?: return false
        goToLocation.value = GoToLocationEvent(
            href = href,
            url = url,
            page = page,
            animated = animated
        )
        // FIXME: call in onLocationChanged
        completion()
        return true
    }

    fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val currentLocator = currentLocator.value ?: return false
        val page = currentLocator.locations.page ?: 0
        val pageCount = currentPageCount ?: return false
        if (page >= (pageCount - 1)) {
            return false
        }
        return goToHref(currentLocator.href, page + 1, animated, completion)
    }

    fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val currentLocator = currentLocator.value ?: return false
        val page = currentLocator.locations.page ?: 0
        if (page <= 0) {
            return false
        }
        return goToHref(currentLocator.href, page - 1, animated, completion)
    }

    /** Called by the PDF view when the visible page changed. */
    fun onPageChanged(href: String, page: Int, pageCount: Int) {
        val link = publication.linkWithHref(href)
        _currentLocator.value = Locator(
            href = href,
            type = link?.type ?: MediaType.PDF.toString(),
            title = link?.title,
            locations = Locator.Locations(
                fragments = listOf("page=${page + 1}"),
                progression = if (pageCount > 0) page / pageCount.toDouble() else 0.0
            )
        )
    }

}