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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

/**
 * Navigator for PDF publications.
 *
 * The PDF navigator delegates the actual PDF rendering to third-party engines like PDFium or
 * PSPDFKit. You must use an implementation of [PdfDocumentFragmentFactory] provided by the PDF
 * engine of your choice.
 *
 * To use this [Fragment], create a factory with [PdfNavigatorFragment.createFactory].
 */
@OptIn(ExperimentalReadiumApi::class)
@PdfSupport
class PdfNavigatorFragment private constructor(
    override val publication: Publication,
    initialLocator: Locator? = null,
    private val initialSettings: PdfDocumentFragment.Settings,
    private val defaultSettings: PdfDocumentFragment.Settings,
    private val listener: Listener?,
    private val documentFragmentFactory: PdfDocumentFragmentFactory
) : Fragment(), VisualNavigator {

    interface Listener : VisualNavigator.Listener {

        /**
         * Called when a PDF resource failed to be loaded, for example because of an [OutOfMemoryError].
         */
        fun onResourceLoadFailed(link: Link, error: Resource.Exception) {}
    }

    companion object {

        /**
         * Creates a factory for [PdfNavigatorFragment].
         *
         * @param publication PDF publication to render in the navigator.
         * @param initialLocator The first location which should be visible when rendering the
         * publication. Can be used to restore the last reading location.
         * @param listener Optional listener to implement to observe events, such as user taps.
         * @param documentFragmentFactory Factory for a [PdfDocumentFragment], provided by third-
         * party PDF engine adapters.
         */
        @OptIn(ExperimentalReadiumApi::class)
        fun createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            listener: Listener? = null,
            documentFragmentFactory: PdfDocumentFragmentFactory,
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(
                publication, initialLocator,
                initialSettings = PdfDocumentFragment.Settings(), defaultSettings = PdfDocumentFragment.Settings(),
                listener, documentFragmentFactory
            )
        }

        /**
         * Creates a factory for [PdfNavigatorFragment].
         *
         * @param publication PDF publication to render in the navigator.
         * @param initialLocator The first location which should be visible when rendering the
         * publication. Can be used to restore the last reading location.
         * @param settings User presentation settings.
         * @param defaultSettings Presentation settings used as fallbacks when a user settings is
         * missing or set to "auto".
         * @param listener Optional listener to implement to observe events, such as user taps.
         * @param documentFragmentFactory Factory for a [PdfDocumentFragment], provided by third-
         * party PDF engine adapters.
         */
        @ExperimentalReadiumApi
        fun createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            settings: PdfDocumentFragment.Settings,
            defaultSettings: PdfDocumentFragment.Settings = PdfDocumentFragment.Settings(),
            listener: Listener? = null,
            documentFragmentFactory: PdfDocumentFragmentFactory,
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(
                publication, initialLocator,
                initialSettings = settings, defaultSettings = defaultSettings,
                listener, documentFragmentFactory
            )
        }
    }

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }

        require(
            publication.readingOrder.count() == 1 &&
                publication.readingOrder.first().mediaType.matches(MediaType.PDF)
        ) { "[PdfNavigatorFragment] currently supports only publications with a single PDF for reading order" }
    }

    /**
     * Current user presentation settings.
     */
    var settings: PdfDocumentFragment.Settings
        get() = viewModel.state.value.userSettings
        set(value) { viewModel.setUserSettings(value) }

    private val viewModel: PdfNavigatorViewModel by viewModels {
        PdfNavigatorViewModel.createFactory(
            requireActivity().application,
            publication,
            initialLocator,
            settings = initialSettings,
            defaultSettings = defaultSettings
        )
    }

    private lateinit var documentFragment: StateFlow<PdfDocumentFragment?>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Clears the savedInstanceState to prevent the child fragment manager from restoring the
        // pdfFragment, as the [ResourceDataProvider] is not [Parcelable].
        super.onCreate(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = FragmentContainerView(inflater.context)
        view.id = R.id.readium_pdf_container
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        documentFragment = viewModel.state
            .distinctUntilChanged { old, new ->
                old.locator.href == new.locator.href
            }
            .map { state ->
                createPdfDocumentFragment(state.locator, state.appliedSettings)
            }
            .stateIn(viewLifecycleOwner.lifecycleScope, started = SharingStarted.Eagerly, null)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentFragment
                    .filterNotNull()
                    .onEach { fragment ->
                        childFragmentManager.commit {
                            replace(R.id.readium_pdf_container, fragment, "readium_pdf_fragment")
                        }
                    }
                    .launchIn(this)

                viewModel.state
                    .map { it.appliedSettings }
                    .distinctUntilChanged()
                    .onEach { settings ->
                        documentFragment.value?.settings = settings
                    }
                    .launchIn(this)
            }
        }
    }

    private suspend fun createPdfDocumentFragment(locator: Locator, settings: PdfDocumentFragment.Settings): PdfDocumentFragment? {
        val link = publication.linkWithHref(locator.href) ?: return null

        return try {
            val pageIndex = (locator.locations.page ?: 1) - 1
            documentFragmentFactory(PdfDocumentFragmentInput(
                publication = publication,
                link = link,
                initialPageIndex = pageIndex,
                settings = settings,
                listener = DocumentFragmentListener()
            ))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load PDF resource ${link.href}")
            listener?.onResourceLoadFailed(link, Resource.Exception.wrap(e))
            null
        }
    }

    private inner class DocumentFragmentListener : PdfDocumentFragment.Listener {
        override fun onPageChanged(pageIndex: Int) {
            viewModel.onPageChanged(pageIndex)
        }

        override fun onTap(point: PointF): Boolean {
            return listener?.onTap(point) ?: false
        }

        override fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
            listener?.onResourceLoadFailed(link, error)
        }
    }

    override val readingProgression: ReadingProgression
        get() = viewModel.state.value.appliedSettings.readingProgression

    override val currentLocator: StateFlow<Locator>
        get() = viewModel.state
            .mapStateIn(lifecycleScope) { it.locator }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        listener?.onJumpToLocator(locator)
        val pageNumber = locator.locations.page ?: locator.locations.position ?: 1
        return goToPageIndex(pageNumber - 1, animated, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val fragment = documentFragment.value ?: return false
        return goToPageIndex(fragment.pageIndex + 1, animated, completion)
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val fragment = documentFragment.value ?: return false
        return goToPageIndex(fragment.pageIndex - 1, animated, completion)
    }

    private fun goToPageIndex(pageIndex: Int, animated: Boolean, completion: () -> Unit): Boolean {
        val fragment = documentFragment.value ?: return false
        val success = fragment.goToPageIndex(pageIndex, animated = animated)
        if (success) { completion() }
        return success
    }
}
