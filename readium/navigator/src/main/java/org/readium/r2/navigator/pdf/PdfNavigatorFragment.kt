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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.model.LinkTapEvent
import com.github.barteksc.pdfviewer.util.FitPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.R
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.presentation.Presentation
import org.readium.r2.navigator.presentation.PresentationKey
import org.readium.r2.navigator.presentation.PresentationSettings
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.use
import timber.log.Timber

/**
 * Navigator for PDF publications.
 *
 * To use this [Fragment], create a factory with `PdfNavigatorFragment.createFactory()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PdfNavigatorFragment @OptIn(ExperimentalPresentation::class) internal constructor(
    override val publication: Publication,
    private val initialLocator: Locator?,
    private val listener: Listener?,
    settings: PresentationSettings,
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

    private fun goToPageIndex(index: Int, completion: () -> Unit = {}): Boolean {
        val href = currentHref ?: return false
        return goToHref(href, index, animated = false, forceReload = false)
    }

    @OptIn(ExperimentalPresentation::class)
    private fun goToHref(href: String, page: Int, animated: Boolean, forceReload: Boolean, completion: () -> Unit = {}): Boolean {
        val link = publication.linkWithHref(href) ?: return false

        if (currentHref == href && !forceReload) {
            pdfView.jumpTo(page, animated)
            completion()

        } else {
            lifecycleScope.launch {
                try {
                    val paginated = presentation.value.overflow?.value == Overflow.PAGINATED

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
                        .pageSnap(paginated)
                        .autoSpacing(paginated)
                        .pageFling(paginated)
                        .spacing(10)
                        // Customization of [PDFView] is done before setting the listeners,
                        // to avoid overriding them in reading apps, which would break the
                        // navigator.
                        .apply { listener?.onConfigurePdfView(this) }
                        .defaultPage(page)
                        .pageFitPolicy(FitPolicy.WIDTH)
                        .onPageChange { index, _ -> onPageChanged(pageIndexToNumber(index)) }
                        .onTap { event -> onTap(event) }
                        .linkHandler { event -> onTapLink(event) }
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
    private val _currentLocator = MutableStateFlow(initialLocator ?: publication.readingOrder.first().toLocator())

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        // FIXME: `position` is relative to the full publication, which would cause an issue for a publication containing several PDFs resources. Only publications with a single PDF resource are supported at the moment, so we're fine.
        val pageNumber = locator.locations.page ?: locator.locations.position ?: 1
        return goToHref(locator.href, pageNumberToIndex(pageNumber), animated, forceReload = false, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        goToHref(link.href, pageNumberToIndex(1), animated, forceReload = false, completion)

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

    @ExperimentalPresentation
    private var _presentation = MutableStateFlow(createPresentation(settings, fallback = null))
    @ExperimentalPresentation
    override val presentation: StateFlow<Presentation> get() = _presentation.asStateFlow()

    @ExperimentalPresentation
    override suspend fun applySettings(settings: PresentationSettings) {
        val page = pageIndexToNumber(pdfView.currentPage)
        _presentation.value = createPresentation(settings, fallback = presentation.value)

        currentHref?.let { href ->
            goToHref(href, page = pageNumberToIndex(page), animated = false, forceReload = true)
        }
    }

    @ExperimentalPresentation
    private fun createPresentation(settings: PresentationSettings, fallback: Presentation?): Presentation {
        val supportedReadingProgressions = listOf(
            ReadingProgression.LTR, ReadingProgression.RTL,
            ReadingProgression.TTB, ReadingProgression.BTT,
        )

        val supportedOverflows = listOf(
            Overflow.PAGINATED, Overflow.SCROLLED
        )

        return Presentation(
            PresentationKey.READING_PROGRESSION to Presentation.StringProperty(
                ReadingProgression,
                value = settings.readingProgression?.takeIf { supportedReadingProgressions.contains(it) }
                    ?: fallback?.readingProgression?.value
                    ?: publication.metadata.readingProgression.takeIf { supportedReadingProgressions.contains(it) }
                    ?: ReadingProgression.TTB,
                supportedValues = supportedReadingProgressions,
                labelForValue = { c, v -> c.getString(when (v) {
                    ReadingProgression.RTL -> R.string.readium_navigator_presentation_readingProgression_rtl
                    ReadingProgression.LTR -> R.string.readium_navigator_presentation_readingProgression_ltr
                    ReadingProgression.TTB -> R.string.readium_navigator_presentation_readingProgression_ttb
                    ReadingProgression.BTT -> R.string.readium_navigator_presentation_readingProgression_btt
                    ReadingProgression.AUTO -> R.string.readium_navigator_presentation_default
                }) }
            ),

            PresentationKey.OVERFLOW to Presentation.StringProperty(
                Overflow,
                value = settings.overflow?.takeIf { supportedOverflows.contains(it) }
                    ?: fallback?.overflow?.value
                    ?: publication.metadata.presentation.overflow?.takeIf { supportedOverflows.contains(it) }
                    ?: Overflow.SCROLLED,
                supportedValues = supportedOverflows,
                labelForValue = { c, v -> c.getString(when (v) {
                    Overflow.PAGINATED -> R.string.readium_navigator_presentation_overflow_paginated
                    Overflow.SCROLLED -> R.string.readium_navigator_presentation_overflow_scrolled
                    Overflow.AUTO -> R.string.readium_navigator_presentation_default
                }) }
            )
        )
    }


    // VisualNavigator

    @OptIn(ExperimentalPresentation::class)
    override val readingProgression: ReadingProgression get() =
        (presentation.value.readingProgression?.value ?: ReadingProgression.TTB)

    /**
     * Indicates whether the order of the [PDFView] pages is reversed to take into account
     * right-to-left and bottom-to-top reading progressions.
     */
    private val isPagesOrderReversed: Boolean get() =
        (readingProgression == ReadingProgression.RTL || readingProgression == ReadingProgression.BTT)


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

    private fun onTapLink(event: LinkTapEvent) {
        val page = event.link.destPageIdx
        val uri = event.link.uri
        if (page != null) {
            goToPageIndex(page)

        } else if (uri != null) {
            openExternalUri(uri)
        }
    }

    private fun openExternalUri(uri: String) {
        val context = context ?: return

        tryOrLog {
            var url = Uri.parse(uri)
            if (url.scheme == null) {
                url = url.buildUpon().scheme("http").build()
            }

            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, url)
        }
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
        @OptIn(ExperimentalPresentation::class)
        fun createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            listener: Listener? = null,
            settings: PresentationSettings = PresentationSettings(),
        ): FragmentFactory =
            createFragmentFactory { PdfNavigatorFragment(publication, initialLocator, listener, settings) }

    }

}
