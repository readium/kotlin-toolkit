/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import android.content.pm.ActivityInfo
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
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.presentation.*
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.use
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Navigator for PDF publications.
 *
 * To use this [Fragment], create a factory with `PdfNavigatorFragment.createFactory()`.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPresentation::class)
class PdfNavigatorFragment internal constructor(
    override val publication: Publication,
    private val initialLocator: Locator?,
    private val listener: Listener?,
    private val config: Configuration,
) : Fragment(), VisualNavigator, PresentableNavigator {

    interface Listener : VisualNavigator.Listener {

        /** Called when configuring [PDFView]. */
        fun onConfigurePdfView(configurator: PDFView.Configurator) {}

        /**
         * Called when a PDF resource failed to be loaded, for example because of an [OutOfMemoryError].
         */
        fun onResourceLoadFailed(link: Link, error: Resource.Exception) {}

    }

    @OptIn(ExperimentalPresentation::class)
    data class Configuration(
        val settings: PresentationValues = PresentationValues(),
        val defaultSettings: PresentationValues = PresentationValues(),
    )

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }
    }

    lateinit var pdfView: PDFView

    private val currentPage: Int get() =
        if (!::pdfView.isInitialized) 0
        else pdfView.currentPage

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

    private fun goToPageIndex(index: Int): Boolean {
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
                    val values = presentationProperties.value.values
                    activity?.requestedOrientation = when (requireNotNull(values.orientation)) {
                        Orientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        Orientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        Orientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }

                    val paginated = (requireNotNull(values.overflow) == Overflow.PAGINATED)
                    val pageSpacing = requireNotNull(values.pageSpacing?.double)
                    val fit = requireNotNull(values.fit)

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
                        .spacing(pageSpacingForValue(pageSpacing))
                        // Customization of [PDFView] is done before setting the listeners,
                        // to avoid overriding them in reading apps, which would break the
                        // navigator.
                        .apply { listener?.onConfigurePdfView(this) }
                        .defaultPage(page)
                        .pageFitPolicy(when (fit) {
                            Fit.WIDTH -> FitPolicy.WIDTH
                            Fit.HEIGHT -> FitPolicy.HEIGHT
                            else -> FitPolicy.BOTH
                        })
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

    private fun pageSpacingForValue(value: Double): Int = (50 * value).roundToInt()

    // Navigator

    override val currentLocator: StateFlow<Locator> get() = _currentLocator
    private val _currentLocator = MutableStateFlow(initialLocator ?: publication.readingOrder.first().toLocator())

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        listener?.onJumpToLocator(locator)
        // FIXME: `position` is relative to the full publication, which would cause an issue for a publication containing several PDFs resources. Only publications with a single PDF resource are supported at the moment, so we're fine.
        val pageNumber = locator.locations.page ?: locator.locations.position ?: 1
        return goToHref(locator.href, pageNumberToIndex(pageNumber), animated, forceReload = false, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        goToHref(link.href, pageNumberToIndex(1), animated, forceReload = false, completion)

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pageIndexToNumber(currentPage)
        if (page >= positionCount) return false

        pdfView.jumpTo(pageNumberToIndex(page + 1), animated)
        completion()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pageIndexToNumber(currentPage)
        if (page <= 1) return false

        pdfView.jumpTo(pageNumberToIndex(page - 1), animated)
        completion()
        return true
    }

    // VisualNavigator

    @OptIn(ExperimentalPresentation::class)
    override val readingProgression: ReadingProgression get() =
        (presentationProperties.value.values.readingProgression ?: ReadingProgression.TTB)

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

    // PresentableNavigator

    private val _presentationProperties = MutableStateFlow(
        createPresentationProperties(
            settings = config.settings,
            fallback = PresentationValues()
        )
    )
    override val presentationProperties = _presentationProperties.asStateFlow()

    override suspend fun applyPresentationSettings(settings: PresentationValues) {
        val page = pageIndexToNumber(currentPage)
        _presentationProperties.value = createPresentationProperties(
            settings = settings,
            fallback = presentationProperties.value.values
        )

        currentHref?.let { href ->
            goToHref(href, page = pageNumberToIndex(page), animated = false, forceReload = true)
        }
    }

    private fun createPresentationProperties(
        settings: PresentationValues,
        fallback: PresentationValues,
        defaults: PresentationValues = config.defaultSettings,
        metadata: Metadata = publication.metadata
    ): PresentationProperties {
        fun <T> List<T>?.firstIn(vararg values: T?): T? = this?.let {
            values.firstOrNull { it != null && contains(it) }
        }

        val fits = listOf(
            Fit.CONTAIN, Fit.WIDTH, Fit.HEIGHT
        )
        val fit = fits.firstIn(settings.fit, fallback.fit?.takeIf { settings.fit != null })
            ?: fits.firstIn(metadata.presentation.fit, defaults.fit)
            ?: Fit.CONTAIN

        val orientations = listOf(
            Orientation.PORTRAIT, Orientation.LANDSCAPE
        )
        val orientation = orientations.firstIn(
            settings.orientation,
            fallback.orientation?.takeIf { settings.orientation != null })
            ?: orientations.firstIn(metadata.presentation.orientation, defaults.orientation)
            ?: Orientation.AUTO

        val overflows = listOf(
            Overflow.PAGINATED, Overflow.SCROLLED
        )
        val overflow = overflows.firstIn(
            settings.overflow,
            fallback.overflow?.takeIf { settings.overflow != null })
            ?: overflows.firstIn(metadata.presentation.overflow, defaults.overflow)
            ?: Overflow.SCROLLED

        val pageSpacing = settings.pageSpacing?.double
            ?: fallback.pageSpacing?.double?.takeIf { settings.pageSpacing != null }
            ?: defaults.pageSpacing?.double
            ?: 0.0

        val readingProgressions = listOf(
            ReadingProgression.LTR, ReadingProgression.RTL,
            ReadingProgression.TTB, ReadingProgression.BTT,
        )
        val readingProgression = readingProgressions.firstIn(
                settings.readingProgression,
                fallback.readingProgression?.takeIf { settings.readingProgression != null }
            )
            ?: readingProgressions.firstIn(metadata.readingProgression, defaults.readingProgression)
            ?: ReadingProgression.TTB

        return PresentationProperties(
            PresentationProperty(
                key = PresentationKey.FIT,
                value = fit,
                constraints = PresentationEnumConstraints(supportedValues = fits)
            ),
            PresentationProperty(
                key = PresentationKey.ORIENTATION,
                value = orientation,
                constraints = PresentationEnumConstraints(supportedValues = orientations)
            ),
            PresentationProperty(
                key = PresentationKey.OVERFLOW,
                value = overflow,
                constraints = PresentationEnumConstraints(supportedValues = overflows)
            ),
            PresentationProperty(
                key = PresentationKey.PAGE_SPACING,
                value = PresentationRange(pageSpacing),
                constraints = PresentationRangeConstraints(stepCount = 20)
                    .require(PresentationValues(overflow = Overflow.PAGINATED))
            ),
            PresentationProperty(
                key = PresentationKey.READING_PROGRESSION,
                value = readingProgression,
                constraints = PresentationEnumConstraints(supportedValues = readingProgressions)
            ),
        )
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
            config: Configuration = Configuration(),
        ): FragmentFactory =
            createFragmentFactory { PdfNavigatorFragment(publication, initialLocator, listener, config) }

    }
}
