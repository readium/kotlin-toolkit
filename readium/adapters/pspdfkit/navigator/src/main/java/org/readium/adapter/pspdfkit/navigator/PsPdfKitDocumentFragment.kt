/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.pspdfkit.navigator

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.adapter.pspdfkit.document.PsPdfKitDocument
import org.readium.adapter.pspdfkit.document.PsPdfKitDocumentFactory
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadTry
import org.readium.r2.shared.util.pdf.cachedIn
import timber.log.Timber

@ExperimentalReadiumApi
public class PsPdfKitDocumentFragment internal constructor(
    private val publication: Publication,
    private val href: Url,
    initialPageIndex: Int,
    initialSettings: PsPdfKitSettings,
    private val listener: Listener?,
) : PdfDocumentFragment<PsPdfKitSettings>() {

    internal interface Listener {
        fun onResourceLoadFailed(href: Url, error: ReadError)
        fun onConfigurePdfView(builder: PdfConfiguration.Builder): PdfConfiguration.Builder
        fun onTap(point: PointF): Boolean
    }

    private companion object {
        private const val pdfFragmentTag = "com.pspdfkit.ui.PdfFragment"
    }

    private var pdfFragment: PdfFragment? = null
        set(value) {
            field = value
            value?.apply {
                setOnPreparePopupToolbarListener(psPdfKitListener)
                addDocumentListener(psPdfKitListener)
            }
        }

    private val psPdfKitListener = PsPdfKitListener()

    private class DocumentViewModel(
        document: suspend () -> ReadTry<PsPdfKitDocument>,
    ) : ViewModel() {

        private val _document: Deferred<ReadTry<PsPdfKitDocument>> =
            viewModelScope.async { document() }

        suspend fun loadDocument(): ReadTry<PsPdfKitDocument> =
            _document.await()

        @OptIn(ExperimentalCoroutinesApi::class)
        val document: PsPdfKitDocument? get() =
            _document.run {
                if (isCompleted) {
                    getCompleted().getOrNull()
                } else {
                    null
                }
            }
    }

    private val viewModel: DocumentViewModel by viewModels {
        createViewModelFactory {
            DocumentViewModel(
                document = {
                    val resource = requireNotNull(publication.get(href))
                    PsPdfKitDocumentFactory(requireContext())
                        .cachedIn(publication)
                        .open(resource, null)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restores the PdfFragment after a configuration change.
        pdfFragment = (childFragmentManager.findFragmentByTag(pdfFragmentTag) as? PdfFragment)
            ?.apply {
                val document = checkNotNull(viewModel.document) {
                    "Should have a document when restoring the PdfFragment."
                }
                setCustomPdfSources(document.document.documentSources)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        FragmentContainerView(inflater.context)
            .apply {
                id = R.id.readium_pspdfkit_fragment
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (pdfFragment == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadDocument()
                    .onFailure { error ->
                        listener?.onResourceLoadFailed(href, error)
                    }
                    .onSuccess { resetPdfFragment() }
            }
        } else {
            resetPdfFragment()
        }
    }

    /**
     * Recreates the [PdfFragment] with the current settings.
     */
    private fun resetPdfFragment() {
        if (isStateSaved || view == null) return
        val doc = viewModel.document ?: return

        doc.document.pageBinding = settings.readingProgression.pageBinding

        val fragment = PdfFragment.newInstance(doc.document, configForSettings(settings))
            .also { pdfFragment = it }

        childFragmentManager.commitNow {
            replace(R.id.readium_pspdfkit_fragment, fragment, pdfFragmentTag)
        }
    }

    private fun configForSettings(settings: PsPdfKitSettings): PdfConfiguration {
        var config = PdfConfiguration.Builder()
            .animateScrollOnEdgeTaps(false)
            .annotationReplyFeatures(AnnotationReplyFeatures.READ_ONLY)
            .automaticallyGenerateLinks(true)
            .autosaveEnabled(false)
            .disableAnnotationEditing()
            .disableAnnotationRotation()
            .disableAutoSelectNextFormElement()
            .disableFormEditing()
            .enableMagnifier(true)
            .excludedAnnotationTypes(emptyList())
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

        // Customization point for integrators.
        listener?.let {
            config = it.onConfigurePdfView(config)
        }

        // Settings-specific configuration
        config = config
            .fitMode(settings.fit.fitMode)
            .layoutMode(settings.spread.pageLayout)
            .firstPageAlwaysSingle(settings.offsetFirstPage)
            .pagePadding(settings.pageSpacing.roundToInt())
            .restoreLastViewedPage(false)
            .scrollDirection(
                if (!settings.scroll) {
                    PageScrollDirection.HORIZONTAL
                } else {
                    settings.scrollAxis.scrollDirection
                }
            )
            .scrollMode(settings.scroll.scrollMode)

        if (publication.isProtected) {
            config = config.disableCopyPaste()
        }

        return config.build()
    }

    private val _pageIndex = MutableStateFlow(initialPageIndex)
    override val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    override fun goToPageIndex(index: Int, animated: Boolean): Boolean {
        val fragment = pdfFragment ?: return false
        if (!isValidPageIndex(index)) {
            return false
        }
        fragment.setPageIndex(index, animated)
        return true
    }

    private fun isValidPageIndex(pageIndex: Int): Boolean {
        val validRange = 0 until (pdfFragment?.pageCount ?: 0)
        return validRange.contains(pageIndex)
    }

    private var settings: PsPdfKitSettings = initialSettings

    override fun applySettings(settings: PsPdfKitSettings) {
        if (this.settings == settings) {
            return
        }

        this.settings = settings
        resetPdfFragment()
    }

    private inner class PsPdfKitListener : DocumentListener, OnPreparePopupToolbarListener {
        override fun onPageChanged(document: PdfDocument, pageIndex: Int) {
            _pageIndex.value = pageIndex
        }

        override fun onDocumentClick(): Boolean {
            val center = view?.run { PointF(width.toFloat() / 2, height.toFloat() / 2) }
            return center?.let { listener?.onTap(it) } ?: false
        }

        override fun onPageClick(
            document: PdfDocument,
            pageIndex: Int,
            event: MotionEvent?,
            pagePosition: PointF?,
            clickedAnnotation: Annotation?,
        ): Boolean {
            if (
                pagePosition == null || clickedAnnotation is LinkAnnotation ||
                clickedAnnotation is SoundAnnotation
            ) {
                return false
            }

            checkNotNull(pdfFragment).viewProjection.toViewPoint(pagePosition, pageIndex)
            return listener?.onTap(pagePosition) ?: false
        }

        private val allowedTextSelectionItems: List<Int> by lazy {
            buildList {
                add(com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_speak)

                if (!publication.isProtected) {
                    add(com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_share)
                    add(com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_copy)
                }
            }
        }

        override fun onPrepareTextSelectionPopupToolbar(toolbar: PdfTextSelectionPopupToolbar) {
            // Makes sure only the menu items in `allowedTextSelectionItems` will be visible.
            toolbar.menuItems = toolbar.menuItems
                .filter { allowedTextSelectionItems.contains(it.id) }
        }

        override fun onDocumentLoaded(document: PdfDocument) {
            val index = pageIndex.value
            if (index < 0 || index >= document.pageCount) {
                Timber.w(
                    "Tried to restore page index $index, but the document has ${document.pageCount} pages"
                )
                return
            }

            checkNotNull(pdfFragment).setPageIndex(index, false)
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
