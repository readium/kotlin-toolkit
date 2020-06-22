/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.extensions.urlToHref
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import timber.log.Timber

class PdfNavigatorFragment(
    private val publication: Publication,
    private val initialLocator: Locator? = null,
    private val listener: Navigator.Listener? = null
) : Fragment(), Navigator {

    interface Listener: Navigator.Listener {
        /** Called when configuring [PDFView]. */
        fun onConfigurePdfView(configurator: PDFView.Configurator) {}
    }

    lateinit var pdfView: PDFView

    private lateinit var positionsByReadingOrder: List<List<Locator>>

    private var currentHref: String? = null

    private val currentResourcePositions: List<Locator> get() {
        val href = currentHref ?: return emptyList()
        val index = publication.readingOrder.indexOfFirstWithHref(href) ?: return emptyList()
        return positionsByReadingOrder[index]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        pdfView = PDFView(context, null)

        positionsByReadingOrder = runBlocking { publication.positionsByReadingOrder() }

        val locator: Locator? = savedInstanceState?.getParcelable(KEY_LOCATOR) ?: initialLocator
        if (locator != null) {
            go(locator)
        } else {
            go(publication.readingOrder.first())
        }

        return pdfView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_LOCATOR, currentLocator.value)
    }

    private fun goToHref(href: String, page: Int, animated: Boolean = false, completion: () -> Unit = {}): Boolean {
        val url = publication.urlToHref(href) ?: return false

        if (currentHref == href) {
            pdfView.jumpTo(page, animated)
            completion()

        } else {
            lifecycleScope.launch {
                try {
                    // Android forbids network requests on the main thread by default, so we have to
                    // do that in the IO dispatcher.
                    withContext(Dispatchers.IO) {
                        pdfView.fromStream(url.openStream())
                            .spacing(10)
                            // Customization of [PDFView] is done before setting the listeners,
                            // to avoid overriding them in reading apps, which would break the
                            // navigator.
                            .also { (listener as? Listener)?.onConfigurePdfView(it) }
                            .defaultPage(page)
                            .onRender { _, _, _ ->
                                pdfView.fitToWidth()
                                // Using `fitToWidth` often breaks the use of `defaultPage`, so we
                                // need to jump manually to the target page.
                                pdfView.jumpTo(page, false)

                                completion()
                            }
                            .onPageChange { page, _ -> onPageChanged(page) }
                            .onTap { event -> onTap(event) }
                            .load()
                    }

                    currentHref = href

                } catch (e: Exception) {
                    Timber.e(e)
                    completion()
                }
            }
        }

        return true
    }

    // Navigator

    override val currentLocator: LiveData<Locator?> get() = _currentLocator
    private val _currentLocator = MutableLiveData<Locator?>(null)

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        val page = ((locator.locations.page ?: 1) - 1).coerceAtLeast(0)
        return goToHref (locator.href, page, animated, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        goToHref(link.href, 0, animated, completion)

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pdfView.currentPage
        val pageCount = pdfView.pageCount
        if (page >= (pageCount - 1)) return false

        pdfView.jumpTo(page + 1, animated)
        completion()
        return true
    }


    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pdfView.currentPage
        if (page <= 0) return false

        pdfView.jumpTo(page - 1, animated)
        completion()
        return true
    }

    // [PDFView] Listeners

    private fun onPageChanged(page: Int) {
        _currentLocator.value = currentResourcePositions.getOrNull(page)
    }

    private fun onTap(e: MotionEvent?): Boolean {
        e ?: return false
        val listener = (listener as? Navigator.VisualListener) ?: return false
        return listener.onTap(PointF(e.x, e.y))
    }

    companion object {
        private const val KEY_LOCATOR = "locator"
    }

}
