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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.SingleJob
import org.readium.r2.shared.util.Url
import timber.log.Timber

@ExperimentalReadiumApi
public class PdfiumDocumentFragment internal constructor(
    private val publication: Publication,
    private val href: Url,
    private val initialPageIndex: Int,
    initialSettings: PdfiumSettings,
    private val listener: Listener?,
    private val inputListener: InputListener
) : PdfDocumentFragment<PdfiumDocumentFragment.Listener, PdfiumSettings>() {

    public interface Listener : PdfDocumentFragment.Listener {
        /**
         * Called when a PDF resource failed to be loaded, for example because of an
         * [OutOfMemoryError].
         */
        public fun onResourceLoadFailed(href: Url, error: Resource.Exception) {}

        /** Called when configuring [PDFView]. */
        public fun onConfigurePdfView(configurator: PDFView.Configurator) {}
    }

    private lateinit var pdfView: PDFView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        PDFView(inflater.context, null)
            .also { pdfView = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resetJob = SingleJob(viewLifecycleOwner.lifecycleScope)
        reset(pageIndex = initialPageIndex)
    }

    private lateinit var resetJob: SingleJob

    private fun reset(pageIndex: Int = _pageIndex.value) {
        if (view == null) return
        val context = context?.applicationContext ?: return

        resetJob.launch {
            try {
                val document = PdfiumDocumentFactory(context)
                    // PDFium crashes when reusing the same PdfDocument, so we must not cache it.
//                    .cachedIn(publication)
                    .open(publication.get(href), null)

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
                    .apply { listener?.onConfigurePdfView(this) }
                    .defaultPage(page)
                    .onRender { _, _, _ ->
                        if (settings.fit == Fit.WIDTH) {
                            pdfView.fitToWidth()
                            // Using `fitToWidth` often breaks the use of `defaultPage`, so we
                            // need to jump manually to the target page.
                            pdfView.jumpTo(page, false)
                        }
                    }
                    .onPageChange { index, _ ->
                        _pageIndex.value = convertPageIndexFromView(index)
                    }
                    .onTap { event ->
                        inputListener.onTap(TapEvent(PointF(event.x, event.y)))
                    }
                    .load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val error = Resource.Exception.wrap(e)
                Timber.e(error)
                listener?.onResourceLoadFailed(href, error)
            }
        }
    }

    private var pageCount = 0

    private val _pageIndex = MutableStateFlow(initialPageIndex)
    override val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    override fun goToPageIndex(index: Int, animated: Boolean): Boolean {
        if (!isValidPageIndex(index)) {
            return false
        }
        pdfView.jumpTo(convertPageIndexToView(index), animated)
        return true
    }

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
        settings.scrollAxis == Axis.HORIZONTAL && settings.readingProgression == ReadingProgression.RTL

    private var settings: PdfiumSettings = initialSettings

    override fun applySettings(settings: PdfiumSettings) {
        if (this.settings == settings) {
            return
        }

        this.settings = settings
        reset()
    }
}
