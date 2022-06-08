/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.pspdfkit.annotations.*
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.annotations.AnnotationReplyFeatures
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.toolbar.popup.PdfTextSelectionPopupToolbar
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.util.pdf.cachedIn

@PdfSupport
class PsPdfKitDocumentFragment(
    private val publication: Publication,
    private val document: PsPdfKitDocument,
    private val listener: Listener?
) : PdfDocumentFragment() {

    companion object {
        fun createFactory(documentFactory: PsPdfKitDocumentFactory): PdfDocumentFragmentFactory =
            { publication, link, listener ->
                val document = documentFactory.cachedIn(publication).open(publication.get(link), null)
                PsPdfKitDocumentFragment(publication, document, listener)
            }
    }

    private lateinit var pdfFragment: PdfFragment
    private val psPdfKitListener = PsPdfKitListener()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = FragmentContainerView(inflater.context)
        view.id = R.id.readium_pspdfkit_container

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
            .firstPageAlwaysSingle(false)
            .fitMode(PageFitMode.FIT_TO_SCREEN)
            .layoutMode(PageLayoutMode.SINGLE)
//            .loadingProgressDrawable(null)
//            .maxZoomScale()
            .pagePadding(0)
            .restoreLastViewedPage(false)
            .scrollDirection(PageScrollDirection.HORIZONTAL)
            .scrollMode(PageScrollMode.CONTINUOUS)
            .scrollOnEdgeTapEnabled(false)
            .scrollOnEdgeTapMargin(50)
            .scrollbarsEnabled(true)
            .setAnnotationInspectorEnabled(false)
            .setJavaScriptEnabled(false)
            .showGapBetweenPages(true)
            .textSelectionEnabled(true)
            .textSelectionPopupToolbarEnabled(true)
            .themeMode(ThemeMode.DEFAULT)
            .videoPlaybackEnabled(true)
            .zoomOutBounce(true)

        if (publication.isProtected) {
            config.disableCopyPaste()
        }

        pdfFragment = PdfFragment.newInstance(document.document, config.build())
        childFragmentManager.commit {
            replace(R.id.readium_pspdfkit_container, pdfFragment, "com.pspdfkit.ui.PdfFragment")
        }

        pdfFragment.addDocumentListener(psPdfKitListener)
        pdfFragment.setOnPreparePopupToolbarListener(psPdfKitListener)

        return view
    }

    override val pageIndex: Int get() = pdfFragment.pageIndex

    override fun goToPageIndex(index: Int, animated: Boolean): Boolean {
        if (!isValidPageIndex(index)) {
            return false
        }
        pdfFragment.setPageIndex(index, animated)
        return true
    }

    private fun isValidPageIndex(pageIndex: Int): Boolean {
        val validRange = 0 until pdfFragment.pageCount
        return validRange.contains(pageIndex)
    }

    private inner class PsPdfKitListener : DocumentListener, OnPreparePopupToolbarListener {
        override fun onPageChanged(document: PdfDocument, pageIndex: Int) {
            listener?.onPageChanged(pageIndex)
        }

        override fun onDocumentClick(): Boolean {
            val listener = listener ?: return false

            val center = view?.run { PointF(width.toFloat() / 2, height.toFloat() / 2) }
            return center?.let { listener.onTap(it) } ?: false
        }

        override fun onPageClick(document: PdfDocument, pageIndex: Int, event: MotionEvent?, pagePosition: PointF?, clickedAnnotation: Annotation?): Boolean {
            if (
                pagePosition == null ||
                clickedAnnotation is LinkAnnotation ||
                clickedAnnotation is MediaAnnotation ||
                clickedAnnotation is ScreenAnnotation ||
                clickedAnnotation is SoundAnnotation ||
                clickedAnnotation is WidgetAnnotation
            ) return false

            pdfFragment.viewProjection.toViewPoint(pagePosition, pageIndex)
            return listener?.onTap(pagePosition) ?: false
        }

        private val allowedTextSelectionItems = listOf(
            R.id.pspdf__text_selection_toolbar_item_share,
            R.id.pspdf__text_selection_toolbar_item_copy,
            R.id.pspdf__text_selection_toolbar_item_speak
        )

        override fun onPrepareTextSelectionPopupToolbar(toolbar: PdfTextSelectionPopupToolbar) {
            // Makes sure only the menu items in `allowedTextSelectionItems` will be visible.
            toolbar.menuItems = toolbar.menuItems
                .filter { allowedTextSelectionItems.contains(it.id) }
        }
    }
}
