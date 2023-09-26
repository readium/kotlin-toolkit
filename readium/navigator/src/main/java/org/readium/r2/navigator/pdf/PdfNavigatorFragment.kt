/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.input.CompositeInputListener
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyInterceptorView
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.util.SingleFragmentFactory
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Navigator for PDF publications.
 *
 * The PDF navigator delegates the actual PDF rendering to third-party engines like PDFium or
 * PSPDFKit. You must use an implementation of [PdfDocumentFragmentFactory] provided by the PDF
 * engine of your choice.
 *
 * To use this [Fragment], create a factory with [PdfNavigatorFactory.createFragmentFactory].
 */
@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class PdfNavigatorFragment<F : PdfDocumentFragment<L, S>, L : PdfDocumentFragment.Listener, S : Configurable.Settings, P : Configurable.Preferences<P>> internal constructor(
    override val publication: Publication,
    private val initialLocator: Locator? = null,
    private val initialPreferences: P,
    private val listener: Listener?,
    private val documentFragmentListener: L?,
    private val pdfEngineProvider: PdfEngineProvider<F, L, S, P, *>
) : Fragment(), VisualNavigator, Configurable<S, P> {

    public interface Listener : VisualNavigator.Listener

    public companion object {

        /**
         * Creates a factory for [PdfNavigatorFragment].
         *
         * @param publication PDF publication to render in the navigator.
         * @param initialLocator The first location which should be visible when rendering the
         * publication. Can be used to restore the last reading location.
         * @param preferences Initial set of user preferences.
         * @param listener Optional listener to implement to observe events, such as user taps.
         * @param pdfEngineProvider provider for third-party PDF engine adapter.
         */
        @ExperimentalReadiumApi
        public fun <L : PdfDocumentFragment.Listener, P : Configurable.Preferences<P>> createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            preferences: P? = null,
            listener: Listener? = null,
            documentFragmentListener: L? = null,
            pdfEngineProvider: PdfEngineProvider<*, L, *, P, *>
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(
                publication,
                initialLocator,
                preferences ?: pdfEngineProvider.createEmptyPreferences(),
                listener,
                documentFragmentListener,
                pdfEngineProvider
            )
        }
    }

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }

        require(
            publication.readingOrder.count() == 1 &&
                publication.readingOrder.first().mediaType?.matches(MediaType.PDF) == true
        ) { "[PdfNavigatorFragment] currently supports only publications with a single PDF for reading order" }
    }

    private val inputListener = CompositeInputListener()

    private val viewModel: PdfNavigatorViewModel<S, P> by viewModels {
        PdfNavigatorViewModel.createFactory(
            requireActivity().application,
            publication,
            initialLocator?.locations,
            initialPreferences,
            pdfEngineProvider
        )
    }

    private lateinit var documentFragment: PdfDocumentFragment<L, S>

    private val documentFragmentFactory: SingleFragmentFactory<F> by lazy {
        val locator = viewModel.currentLocator.value
        pdfEngineProvider.createDocumentFragmentFactory(
            PdfDocumentFragmentInput(
                publication = publication,
                href = locator.href,
                pageIndex = locator.locations.pageIndex,
                settings = viewModel.settings.value,
                listener = documentFragmentListener,
                inputListener = inputListener
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = documentFragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = FragmentContainerView(inflater.context)
        view.id = R.id.readium_pdf_container
        return KeyInterceptorView(view, inputListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tag = "documentFragment"
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                replace(
                    R.id.readium_pdf_container,
                    documentFragmentFactory(),
                    tag
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        documentFragment = childFragmentManager.findFragmentByTag(tag) as PdfDocumentFragment<L, S>

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentFragment.pageIndex
                    .onEach { viewModel.onPageChanged(it) }
                    .launchIn(this)

                viewModel.settings
                    .onEach { documentFragment.applySettings(it) }
                    .launchIn(this)
            }
        }
    }

    // Configurable

    override val settings: StateFlow<S> get() = viewModel.settings

    override fun submitPreferences(preferences: P) {
        viewModel.submitPreferences(preferences)
    }

    // Navigator

    override val currentLocator: StateFlow<Locator> get() = viewModel.currentLocator

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        @Suppress("NAME_SHADOWING")
        val locator = publication.normalizeLocator(locator)
        listener?.onJumpToLocator(locator)
        return goToPageIndex(locator.locations.pageIndex, animated, completion)
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val pageIndex = currentLocator.value.locations.pageIndex + 1
        return goToPageIndex(pageIndex, animated, completion)
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val pageIndex = currentLocator.value.locations.pageIndex - 1
        return goToPageIndex(pageIndex, animated, completion)
    }

    private fun goToPageIndex(pageIndex: Int, animated: Boolean, completion: () -> Unit): Boolean {
        val success = documentFragment.goToPageIndex(pageIndex, animated = animated)
        if (success) { completion() }
        return success
    }

    // VisualNavigator

    override val publicationView: View
        get() = requireView()

    @ExperimentalReadiumApi
    override val presentation: StateFlow<VisualNavigator.Presentation>
        get() = settings.mapStateIn(lifecycleScope) { settings ->
            pdfEngineProvider.computePresentation(settings)
        }

    @Deprecated(
        "Use `presentation.value.readingProgression` instead",
        replaceWith = ReplaceWith("presentation.value.readingProgression"),
        level = DeprecationLevel.ERROR
    )
    override val readingProgression: PublicationReadingProgression
        get() = throw NotImplementedError()

    override fun addInputListener(listener: InputListener) {
        inputListener.add(listener)
    }

    override fun removeInputListener(listener: InputListener) {
        inputListener.remove(listener)
    }
}

private val Locator.Locations.pageIndex: Int get() =
    (page ?: position ?: 1) - 1
