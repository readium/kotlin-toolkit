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
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.use
import timber.log.Timber
import java.util.*

/**
 * Navigator for PDF publications.
 *
 * To use this [Fragment], create a factory with `PdfNavigatorFragment.createFactory()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PdfNavigatorFragment internal constructor(
    override val publication: Publication,
    private val initialLocator: Locator? = null,
    private val listener: Listener? = null
) : Fragment(), VisualNavigator {

    interface Listener : VisualNavigator.Listener {

        /** Called when configuring [PDFView]. */
        fun onConfigurePdfView(configurator: PDFView.Configurator) {}

        /**
         * Called when a PDF resource failed to be loaded, for example because of an [OutOfMemoryError].
         */
        fun onResourceLoadFailed(link: Link, error: Resource.Exception) {}

    }

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }
    }

    lateinit var pdfView: PDFView

    private lateinit var positionsByReadingOrder: List<List<Locator>>
    private var positionCount: Int = 1

    private var currentHref: String? = null

    private val currentResourcePositions: List<Locator> get() {
        val href = currentHref ?: return emptyList()
        val index = publication.readingOrder.indexOfFirstWithHref(href) ?: return emptyList()
        return positionsByReadingOrder[index]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        pdfView = PDFView(context, null)

        positionsByReadingOrder = runBlocking { publication.positionsByReadingOrder() }
        positionCount = positionsByReadingOrder.fold(0) { c, locators -> c + locators.size }
        require(positionCount > 0)

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
        outState.putParcelable(KEY_LOCATOR, _currentLocator.value)
    }

    private fun goToHref(href: String, page: Int, animated: Boolean = false, completion: () -> Unit = {}): Boolean {
        val link = publication.linkWithHref(href) ?: return false

        if (currentHref == href) {
            pdfView.jumpTo(page, animated)
            completion()

        } else {
            lifecycleScope.launch {
                try {
                    pdfView
                        .run {
                            publication.get(link).use { resource ->
                                val file = resource.file
                                if (file != null) fromFile(file)
                                else fromBytes(resource.read().getOrThrow())
                            }
                        }
                        .apply {
                            if (isPagesOrderReversed) {
                                // AndroidPdfViewer doesn't support RTL. A workaround is to provide
                                // the explicit page list in the right order.
                                pages(*((positionCount - 1) downTo 0).toList().toIntArray())
                            }
                        }
                        .swipeHorizontal(readingProgression.isHorizontal ?: false)
                        .spacing(10)
                        // Customization of [PDFView] is done before setting the listeners,
                        // to avoid overriding them in reading apps, which would break the
                        // navigator.
                        .apply { listener?.onConfigurePdfView(this) }
                        .defaultPage(page)
                        .onRender { _, _, _ ->
                            pdfView.fitToWidth()
                            // Using `fitToWidth` often breaks the use of `defaultPage`, so we
                            // need to jump manually to the target page.
                            pdfView.jumpTo(page, false)

                            completion()
                        }
                        .onPageChange { index, _ -> onPageChanged(pageIndexToNumber(index)) }
                        .onTap { event -> onTap(event) }
                        .load()

                    currentHref = href

                } catch (e: Exception) {
                    val error = Resource.Exception.wrap(e)
                    if (error != Resource.Exception.Cancelled) {
                        listener?.onResourceLoadFailed(link, error)
                    }

                    Timber.e(e)
                    completion()
                }
            }
        }

        return true
    }
    
    private fun pageNumberToIndex(page: Int): Int {
        var index = (page - 1).coerceAtLeast(0)
        if (isPagesOrderReversed) {
            index = (positionCount - 1) - index
        }
        return index
    }

    private fun pageIndexToNumber(index: Int): Int {
        var page = index + 1
        if (isPagesOrderReversed) {
            page = (positionCount + 1) - page
        }
        return page
    }


    // Navigator

    override val currentLocator: StateFlow<Locator> get() = _currentLocator
    private val _currentLocator = MutableStateFlow(initialLocator
        ?: requireNotNull(publication.locatorFromLink(publication.readingOrder.first()))
    )

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        listener?.onJumpToLocator(locator)
        // FIXME: `position` is relative to the full publication, which would cause an issue for a publication containing several PDFs resources. Only publications with a single PDF resource are supported at the moment, so we're fine.
        val pageNumber = locator.locations.page ?: locator.locations.position ?: 1
        return goToHref(locator.href, pageNumberToIndex(pageNumber), animated, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pageIndexToNumber(pdfView.currentPage)
        if (page >= positionCount) return false

        pdfView.jumpTo(pageNumberToIndex(page + 1), animated)
        completion()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pageIndexToNumber(pdfView.currentPage)
        if (page <= 1) return false

        pdfView.jumpTo(pageNumberToIndex(page - 1), animated)
        completion()
        return true
    }


    // VisualNavigator

    override val readingProgression: ReadingProgression =
        publication.metadata.readingProgression.takeIf { it != ReadingProgression.AUTO }
            ?: ReadingProgression.TTB

    /**
     * Indicates whether the order of the [PDFView] pages is reversed to take into account
     * right-to-left and bottom-to-top reading progressions.
     */
    private val isPagesOrderReversed: Boolean =
        readingProgression == ReadingProgression.RTL || readingProgression == ReadingProgression.BTT


    // [PDFView] Listeners

    private fun onPageChanged(page: Int) {
        currentResourcePositions.getOrNull(page - 1)?.let {
            _currentLocator.value = it
        }
    }

    private fun onTap(e: MotionEvent?): Boolean {
        e ?: return false
        val listener = (listener as? VisualNavigator.Listener) ?: return false
        return listener.onTap(PointF(e.x, e.y))
    }

    companion object {
        private const val KEY_LOCATOR = "locator"

        /**
         * Creates a factory for a [PdfNavigatorFragment].
         *
         * @param publication PDF publication to render in the navigator.
         * @param initialLocator The first location which should be visible when rendering the PDF.
         *        Can be used to restore the last reading location.
         * @param listener Optional listener to implement to observe events, such as user taps.
         */
        fun createFactory(publication: Publication, initialLocator: Locator? = null, listener: Listener? = null): FragmentFactory =
            createFragmentFactory { PdfNavigatorFragment(publication, initialLocator, listener) }

    }

}
