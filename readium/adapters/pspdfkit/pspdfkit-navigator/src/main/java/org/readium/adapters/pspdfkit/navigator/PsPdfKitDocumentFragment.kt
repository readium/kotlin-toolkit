/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.LinkAnnotation
import com.pspdfkit.annotations.SoundAnnotation
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.annotations.AnnotationReplyFeatures
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.PageBinding
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.toolbar.popup.PdfTextSelectionPopupToolbar
import kotlin.math.roundToInt
import org.readium.adapters.pspdfkit.document.PsPdfKitDocument
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isProtected

@ExperimentalReadiumApi
internal class PsPdfKitDocumentFragment(
    private val publication: Publication,
    private val document: PsPdfKitDocument,
    private val initialPageIndex: Int,
    settings: PsPdfKitSettings,
    private val listener: Listener?
) : PdfDocumentFragment<PsPdfKitSettings>() {

    override var settings: PsPdfKitSettings = settings
        set(value) {
            if (field == value) return

            field = value
            reloadDocumentAtPage(pageIndex)
        }

    private lateinit var pdfFragment: PdfFragment
    private val psPdfKitListener = PsPdfKitListener()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentContainerView(inflater.context)
            .apply {
                id = org.readium.adapter.pspdfkit.navigator.R.id.readium_pspdfkit_fragment
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadDocumentAtPage(initialPageIndex)
    }

    private fun reloadDocumentAtPage(pageIndex: Int) {
        pdfFragment = createPdfFragment()

        childFragmentManager.commitNow {
            replace(org.readium.adapter.pspdfkit.navigator.R.id.readium_pspdfkit_fragment, pdfFragment, "com.pspdfkit.ui.PdfFragment")
        }
    }

    private fun createPdfFragment(): PdfFragment {
        document.document.pageBinding = settings.readingProgression.pageBinding
        val config = configForSettings(settings)

        val newFragment = if (::pdfFragment.isInitialized) {
            PdfFragment.newInstance(pdfFragment, config)
        } else {
            PdfFragment.newInstance(document.document, config)
        }

        newFragment.apply {
            setOnPreparePopupToolbarListener(psPdfKitListener)
            addDocumentListener(psPdfKitListener)
        }

        return newFragment
    }

    private fun configForSettings(settings: PsPdfKitSettings): PdfConfiguration {
        val config = PdfConfiguration.Builder()
            .animateScrollOnEdgeTaps(false)
            .annotationReplyFeatures(AnnotationReplyFeatures.READ_ONLY)
            .automaticallyGenerateLinks(true)
            .autosaveEnabled(false)
//            .backgroundColor(Color.TRANSPARENT)
            .disableAnnotationEditing()
            .disableAnnotationRotation()
            .disableAutoSelectNextFormElement()
            .disableFormEditing()
            .enableMagnifier(true)
            .excludedAnnotationTypes(emptyList())
            .fitMode(settings.fit.fitMode)
            .layoutMode(settings.spread.pageLayout)
//            .loadingProgressDrawable(null)
//            .maxZoomScale()
            .firstPageAlwaysSingle(settings.offsetFirstPage)
            .pagePadding(settings.pageSpacing.roundToInt())
            .restoreLastViewedPage(false)
            .scrollDirection(
                if (!settings.scroll) PageScrollDirection.HORIZONTAL
                else settings.scrollAxis.scrollDirection
            )
            .scrollMode(settings.scroll.scrollMode)
            .scrollOnEdgeTapEnabled(false)
            .scrollOnEdgeTapMargin(50)
            .scrollbarsEnabled(true)
            .setAnnotationInspectorEnabled(false)
            .setJavaScriptEnabled(false)
            .textSelectionEnabled(true)
            .textSelectionPopupToolbarEnabled(true)
            .themeMode(ThemeMode.DEFAULT)
            .videoPlaybackEnabled(true)
            .zoomOutBounce(true)

        if (publication.isProtected) {
            config.disableCopyPaste()
        }

        return config.build()
    }

    override var pageIndex: Int = initialPageIndex
        private set

    override fun goToPageIndex(index: Int, animated: Boolean): Boolean {
        if (!isValidPageIndex(index)) {
            return false
        }
        pageIndex = index
        pdfFragment.setPageIndex(index, animated)
        return true
    }

    private fun isValidPageIndex(pageIndex: Int): Boolean {
        val validRange = 0 until pdfFragment.pageCount
        return validRange.contains(pageIndex)
    }

    private inner class PsPdfKitListener : DocumentListener, OnPreparePopupToolbarListener {
        override fun onPageChanged(document: PdfDocument, pageIndex: Int) {
            this@PsPdfKitDocumentFragment.pageIndex = pageIndex
            listener?.onPageChanged(pageIndex)
        }

        override fun onDocumentClick(): Boolean {
            val listener = listener ?: return false

            val center = view?.run { PointF(width.toFloat() / 2, height.toFloat() / 2) }
            return center?.let { listener.onTap(it) } ?: false
        }

        override fun onPageClick(
            document: PdfDocument,
            pageIndex: Int,
            event: MotionEvent?,
            pagePosition: PointF?,
            clickedAnnotation: Annotation?
        ): Boolean {
            if (
                pagePosition == null || clickedAnnotation is LinkAnnotation ||
                clickedAnnotation is SoundAnnotation
            ) return false

            pdfFragment.viewProjection.toViewPoint(pagePosition, pageIndex)
            return listener?.onTap(pagePosition) ?: false
        }

        private val allowedTextSelectionItems = listOf(
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_share,
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_copy,
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_speak
        )

        override fun onPrepareTextSelectionPopupToolbar(toolbar: PdfTextSelectionPopupToolbar) {
            // Makes sure only the menu items in `allowedTextSelectionItems` will be visible.
            toolbar.menuItems = toolbar.menuItems
                .filter { allowedTextSelectionItems.contains(it.id) }
        }

        override fun onDocumentLoaded(document: PdfDocument) {
            super.onDocumentLoaded(document)

            pdfFragment.setPageIndex(pageIndex, false)
        }
    }

    private val Boolean.scrollMode: PageScrollMode
        get() = when (this) {
            false -> PageScrollMode.PER_PAGE
            true -> PageScrollMode.CONTINUOUS
        }

    private val Fit.fitMode: PageFitMode
        get() = when (this) {
            Fit.WIDTH -> PageFitMode.FIT_TO_WIDTH
            else -> PageFitMode.FIT_TO_SCREEN
        }

    private val Axis.scrollDirection: PageScrollDirection
        get() = when (this) {
            Axis.VERTICAL -> PageScrollDirection.VERTICAL
            Axis.HORIZONTAL -> PageScrollDirection.HORIZONTAL
        }

    private val ReadingProgression.pageBinding: PageBinding
        get() = when (this) {
            ReadingProgression.LTR -> PageBinding.LEFT_EDGE
            ReadingProgression.RTL -> PageBinding.RIGHT_EDGE
        }

    private val Spread.pageLayout: PageLayoutMode
        get() = when (this) {
            Spread.AUTO -> PageLayoutMode.AUTO
            Spread.ALWAYS -> PageLayoutMode.DOUBLE
            Spread.NEVER -> PageLayoutMode.SINGLE
        }
}
