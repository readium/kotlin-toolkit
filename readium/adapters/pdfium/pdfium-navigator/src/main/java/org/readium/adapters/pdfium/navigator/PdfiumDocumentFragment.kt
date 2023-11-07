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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

@ExperimentalReadiumApi
class PdfiumDocumentFragment internal constructor(
    private val publication: Publication,
    private val link: Link,
    private val initialPageIndex: Int,
    settings: PdfiumSettings,
    private val appListener: Listener?,
    private val navigatorListener: PdfDocumentFragment.Listener?
) : PdfDocumentFragment<PdfiumSettings>() {

    // Dummy constructor to address https://github.com/readium/kotlin-toolkit/issues/395
    constructor() : this(
        publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    identifier = "readium:dummy",
                    localizedTitle = LocalizedString("")
                )
            )
        ),
        link = Link(href = "publication.pdf", type = "application/pdf"),
        initialPageIndex = 0,
        settings = PdfiumSettings(
            fit = Fit.WIDTH,
            pageSpacing = 0.0,
            readingProgression = ReadingProgression.LTR,
            scrollAxis = Axis.VERTICAL
        ),
        appListener = null,
        navigatorListener = null
    )

    interface Listener {
        /** Called when configuring [PDFView]. */
        fun onConfigurePdfView(configurator: PDFView.Configurator) {}
    }

    override var settings: PdfiumSettings = settings
        set(value) {
            if (field == value) return

            val page = pageIndex
            field = value
            reloadDocumentAtPage(page)
        }

    private lateinit var pdfView: PDFView

    private var isReloading: Boolean = false
    private var hasToReload: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        PDFView(inflater.context, null)
            .also { pdfView = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadDocumentAtPage(pageIndex)
    }

    private fun reloadDocumentAtPage(pageIndex: Int) {
        if (isReloading) {
            hasToReload = pageIndex
            return
        }

        isReloading = true

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
                    .swipeHorizontal(settings.scrollAxis == Axis.HORIZONTAL)
                    .spacing(settings.pageSpacing.roundToInt())
                    // Customization of [PDFView] is done before setting the listeners,
                    // to avoid overriding them in reading apps, which would break the
                    // navigator.
                    .apply { appListener?.onConfigurePdfView(this) }
                    .defaultPage(page)
                    .onRender { _, _, _ ->
                        if (settings.fit == Fit.WIDTH) {
                            pdfView.fitToWidth()
                            // Using `fitToWidth` often breaks the use of `defaultPage`, so we
                            // need to jump manually to the target page.
                            pdfView.jumpTo(page, false)
                        }
                    }
                    .onLoad {
                        val hasToReloadNow = hasToReload
                        if (hasToReloadNow != null) {
                            reloadDocumentAtPage(pageIndex)
                        } else {
                            isReloading = false
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
     * right-to-left reading progressions.
     */
    private val isPagesOrderReversed: Boolean get() =
        settings.scrollAxis == Axis.HORIZONTAL &&
            settings.readingProgression == ReadingProgression.RTL
}
