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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.viewModels
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.readium.navigator.pspdfkit.databinding.ReadiumPspdfkitFragmentBinding
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.fragmentParameters
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

@PdfSupport
@OptIn(InternalReadiumApi::class)
class PdfNavigatorFragment private constructor(
    override val publication: Publication,
    initialLocator: Locator? = null,
    private val listener: Listener?
) : Fragment(), VisualNavigator {

    companion object {

        /**
         * Creates a factory for [PdfNavigatorFragment].
         *
         * @param publication PDF publication to render in the navigator.
         * @param initialLocator The first location which should be visible when rendering the
         * publication. Can be used to restore the last reading location.
         * @param listener Optional listener to implement to observe events, such as user taps.
         */
        fun createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            listener: Listener? = null,
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(publication, initialLocator, listener)
        }
    }

    interface Listener : VisualNavigator.Listener

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }

        require(
            publication.readingOrder.isNotEmpty() &&
            publication.readingOrder.all { it.mediaType.matches(MediaType.PDF) }
        ) { "[PdfNavigatorFragment] supports only publications with PDFs in the reading order" }
    }

    private val viewModel: PdfNavigatorViewModel by viewModels {
        PdfNavigatorViewModel.createFactory(requireActivity().application, publication, initialLocator, PsPdfKitDocumentFactory(requireContext()))
    }

    private lateinit var pdfFragment: PdfFragment
    private val psPdfKitListener = PsPdfKitListener()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = ReadiumPspdfkitFragmentBinding.inflate(inflater, container, false)

        val document = runBlocking { PsPdfKitDocumentFactory(requireContext()).open(publication.get(publication.readingOrder.first()), null) }
            as PsPdfKitDocument

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
            .loadingProgressDrawable(null)
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

        val tag = "readium_pdf_fragment"
        pdfFragment = childFragmentManager.findFragmentByTag(tag) as? PdfFragment
            ?: PdfFragment.newInstance(document.document, config.build())
                .also {
                    childFragmentManager.beginTransaction()
                        .add(R.id.readium_pspdfkit_container, it, tag)
                        .commit()
                }

        pdfFragment.addDocumentListener(psPdfKitListener)
        pdfFragment.setOnPreparePopupToolbarListener(psPdfKitListener)

        go(currentLocator.value, animated = false)

        return binding.root
    }

    override val readingProgression: ReadingProgression
        get() = ReadingProgression.AUTO

    override val currentLocator: StateFlow<Locator>
        get() = viewModel.currentLocator

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        listener?.onJumpToLocator(locator)
        val pageNumber = locator.locations.page ?: locator.locations.position ?: 1
        return goToPageIndex(pageNumber - 1, animated, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean =
        goToPageIndex(pdfFragment.pageIndex + 1, animated, completion)

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean =
        goToPageIndex(pdfFragment.pageIndex - 1, animated, completion)

    private fun goToPageIndex(pageIndex: Int, animated: Boolean, completion: () -> Unit): Boolean {
        if (!isValidPageIndex(pageIndex)) {
            return false
        }
        pdfFragment.setPageIndex(pageIndex, animated)
        completion()
        return true
    }

    private fun isValidPageIndex(pageIndex: Int): Boolean {
        val validRange = 0 until pdfFragment.pageCount
        return validRange.contains(pageIndex)
    }

    private inner class PsPdfKitListener : DocumentListener, OnPreparePopupToolbarListener {
        override fun onPageChanged(document: PdfDocument, pageIndex: Int) {
            viewModel.onPageChanged(pageIndex)
        }

        override fun onDocumentClick(): Boolean {
            val center = view?.run { PointF(width.toFloat() / 2, height.toFloat() / 2) }
            if (center != null && listener != null) {
                return listener.onTap(center)
            }
            return false
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

@OptIn(InternalReadiumApi::class)
private val Locator.Locations.page: Int? get() =
    fragmentParameters["page"]?.toIntOrNull()
