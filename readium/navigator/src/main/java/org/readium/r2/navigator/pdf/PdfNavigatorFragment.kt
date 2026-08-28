/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

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
import org.readium.r2.navigator.NavigatorFragment
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.R
import org.readium.r2.navigator.RestorationNotSupportedException
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.dummyPublication
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
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Navigator for PDF publications.
 *
 * The PDF navigator delegates the actual PDF rendering to third-party engines like PDFium or
 * PSPDFKit.
 *
 * To use this [Fragment], create a factory with [PdfNavigatorFactory.createFragmentFactory].
 */
@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class PdfNavigatorFragment<S : Configurable.Settings, P : Configurable.Preferences<P>> internal constructor(
    publication: Publication,
    private val initialLocator: Locator? = null,
    private val initialPreferences: P,
    private val listener: Listener?,
    private val pdfEngineProvider: PdfEngineProvider<S, P, *>,
) : NavigatorFragment(publication), VisualNavigator, OverflowableNavigator, Configurable<S, P> {

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
        public fun <P : Configurable.Preferences<P>> createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            preferences: P? = null,
            listener: Listener? = null,
            pdfEngineProvider: PdfEngineProvider<*, P, *>,
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(
                publication,
                initialLocator,
                preferences ?: pdfEngineProvider.createEmptyPreferences(),
                listener,
                pdfEngineProvider
            )
        }

        /**
         * Creates a factory for a dummy [PdfNavigatorFragment].
         *
         * Used when Android restore the [PdfNavigatorFragment] after the process was killed. You need
         * to make sure the fragment is removed from the screen before `onResume` is called.
         */
        public fun <P : Configurable.Preferences<P>> createDummyFactory(
            pdfEngineProvider: PdfEngineProvider<*, P, *>,
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(
                publication = dummyPublication,
                initialLocator = Locator(href = Url("#")!!, mediaType = MediaType.PDF),
                initialPreferences = pdfEngineProvider.createEmptyPreferences(),
                listener = null,
                pdfEngineProvider = pdfEngineProvider
            )
        }
    }

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }

        if (publication != dummyPublication) {
            require(
                publication.readingOrder.count() == 1 &&
                    publication.readingOrder.first().mediaType?.matches(MediaType.PDF) == true
            ) { "[PdfNavigatorFragment] currently supports only publications with a single PDF for reading order" }
        }
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

    private lateinit var documentFragment: PdfDocumentFragment<S>

    private val documentFragmentFactory: SingleFragmentFactory<*> by lazy {
        val locator = viewModel.currentLocator.value
        pdfEngineProvider.createDocumentFragmentFactory(
            PdfDocumentFragmentInput(
                publication = publication,
                href = locator.href,
                pageIndex = locator.locations.pageIndex,
                settings = viewModel.settings.value,
                navigatorListener = listener,
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
        savedInstanceState: Bundle?,
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
        documentFragment = childFragmentManager.findFragmentByTag(tag) as PdfDocumentFragment<S>

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

    override fun onResume() {
        super.onResume()

        if (publication == dummyPublication) {
            throw RestorationNotSupportedException
        }
    }

    // Configurable

    override val settings: StateFlow<S> get() = viewModel.settings

    override fun submitPreferences(preferences: P) {
        viewModel.submitPreferences(preferences)
    }

    // Navigator

    override val currentLocator: StateFlow<Locator> get() = viewModel.currentLocator

    override fun go(locator: Locator, animated: Boolean): Boolean {
        @Suppress("NAME_SHADOWING")
        val locator = publication.normalizeLocator(locator)
        listener?.onJumpToLocator(locator)
        return goToPageIndex(locator.locations.pageIndex, animated)
    }

    override fun go(link: Link, animated: Boolean): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated)
    }

    override fun goForward(animated: Boolean): Boolean {
        val pageIndex = currentLocator.value.locations.pageIndex + 1
        return goToPageIndex(pageIndex, animated)
    }

    override fun goBackward(animated: Boolean): Boolean {
        val pageIndex = currentLocator.value.locations.pageIndex - 1
        return goToPageIndex(pageIndex, animated)
    }

    private fun goToPageIndex(pageIndex: Int, animated: Boolean): Boolean {
        return documentFragment.goToPageIndex(pageIndex, animated = animated)
    }

    // VisualNavigator

    override val publicationView: View
        get() = requireView()

    @ExperimentalReadiumApi
    override val overflow: StateFlow<OverflowableNavigator.Overflow>
        get() = settings.mapStateIn(lifecycleScope) { settings ->
            pdfEngineProvider.computeOverflow(settings)
        }

    override fun addInputListener(listener: InputListener) {
        inputListener.add(listener)
    }

    override fun removeInputListener(listener: InputListener) {
        inputListener.remove(listener)
    }
}

private val Locator.Locations.pageIndex: Int get() =
    (page ?: position ?: 1) - 1
