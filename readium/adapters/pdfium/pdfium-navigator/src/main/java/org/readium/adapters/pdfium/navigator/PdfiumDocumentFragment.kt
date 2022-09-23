/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.launch
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.pdf.PdfDocumentFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
@PdfSupport
class PdfiumDocumentFragment private constructor(
    private val publication: Publication,
    private val link: Link,
    private val initialPageIndex: Int,
    settings: Settings,
    private val appListener: Listener?,
    private val navigatorListener: PdfDocumentFragment.Listener?
) : PdfDocumentFragment() {

    interface Listener {
        /** Called when configuring [PDFView]. */
        fun onConfigurePdfView(configurator: PDFView.Configurator) {}
    }

    companion object {
        fun createFactory(listener: Listener? = null): PdfDocumentFragmentFactory =
            { input ->
                PdfiumDocumentFragment(
                    publication = input.publication,
                    link = input.link,
                    initialPageIndex = input.initialPageIndex,
                    settings = input.settings,
                    appListener = listener,
                    navigatorListener = input.listener
                )
            }
    }

    override var settings: Settings = settings
        set(value) {
            if (field == value) return

            val page = pageIndex
            field = value
            reloadDocumentAtPage(page)
        }

    private lateinit var pdfView: PDFView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        PDFView(inflater.context, null)
            .also { pdfView = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadDocumentAtPage(pageIndex)
    }

    private fun reloadDocumentAtPage(pageIndex: Int) {
        val context = context?.applicationContext ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val document = PdfiumDocumentFactory(context)
                    // PDFium crashes when reusing the same PdfDocument, so we must not cache it.
//                    .cachedIn(publication)
                    .open(publication.get(link), null)

                pageCount = document.pageCount
                val page = convertPageIndexToView(pageIndex)

                pdfView.recycle()
                pdfView
                    .fromSource { _, _, _ -> document.document }
                    .apply {
                        if (isPagesOrderReversed) {
                            // AndroidPdfViewer doesn't support RTL. A workaround is to provide
                            // the explicit page list in the right order.
                            pages(*((pageCount - 1) downTo 0).toList().toIntArray())
                        }
                    }
                    .swipeHorizontal(settings.readingProgression.isHorizontal ?: false)
                    .spacing(10)
                    // Customization of [PDFView] is done before setting the listeners,
                    // to avoid overriding them in reading apps, which would break the
                    // navigator.
                    .apply { appListener?.onConfigurePdfView(this) }
                    .defaultPage(page)
                    .onRender { _, _, _ ->
                        if (settings.fit == Presentation.Fit.WIDTH) {
                            pdfView.fitToWidth()
                            // Using `fitToWidth` often breaks the use of `defaultPage`, so we
                            // need to jump manually to the target page.
                            pdfView.jumpTo(page, false)
                        }
                    }
                    .onPageChange { index, _ ->
                        navigatorListener?.onPageChanged(convertPageIndexFromView(index))
                    }
                    .onTap { event ->
                        navigatorListener?.onTap(PointF(event.x, event.y))
                            ?: false
                    }
                    .load()

            } catch (e: Exception) {
                val error = Resource.Exception.wrap(e)
                Timber.e(error)
                navigatorListener?.onResourceLoadFailed(link, error)
            }
        }
    }

    override val pageIndex: Int get() = viewPageIndex ?: initialPageIndex

    private val viewPageIndex: Int? get() =
        if (pdfView.isRecycled) null
        else convertPageIndexFromView(pdfView.currentPage)

    override fun goToPageIndex(index: Int, animated: Boolean): Boolean {
        if (!isValidPageIndex(index)) {
            return false
        }
        pdfView.jumpTo(convertPageIndexToView(index), animated)
        return true
    }

    private var pageCount = 0

    private fun isValidPageIndex(pageIndex: Int): Boolean {
        val validRange = 0 until pageCount
        return validRange.contains(pageIndex)
    }

    private fun convertPageIndexToView(page: Int): Int {
        var index = (page - 1).coerceAtLeast(0)
        if (isPagesOrderReversed) {
            index = (pageCount - 1) - index
        }
        return index
    }

    private fun convertPageIndexFromView(index: Int): Int {
        var page = index + 1
        if (isPagesOrderReversed) {
            page = (pageCount + 1) - page
        }
        return page
    }

    /**
     * Indicates whether the order of the [PDFView] pages is reversed to take into account
     * right-to-left and bottom-to-top reading progressions.
     */
    private val isPagesOrderReversed: Boolean get() =
        settings.readingProgression == ReadingProgression.RTL ||
            settings.readingProgression == ReadingProgression.BTT
}
