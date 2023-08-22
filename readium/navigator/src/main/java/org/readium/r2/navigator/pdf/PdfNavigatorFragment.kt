/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.graphics.PointF
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.input.CompositeInputListener
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyInterceptorView
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

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
public class PdfNavigatorFragment<S : Configurable.Settings, P : Configurable.Preferences<P>> internal constructor(
    override val publication: Publication,
    initialLocator: Locator? = null,
    initialPreferences: P,
    private val listener: Listener?,
    private val pdfEngineProvider: PdfEngineProvider<S, P, *>
) : Fragment(), VisualNavigator, Configurable<S, P> {

    public interface Listener : VisualNavigator.Listener {

        /**
         * Called when a PDF resource failed to be loaded, for example because of an [OutOfMemoryError].
         */
        public fun onResourceLoadFailed(link: Link, error: Resource.Exception) {}
    }

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
        public fun <S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>> createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            preferences: P? = null,
            listener: Listener? = null,
            pdfEngineProvider: PdfEngineProvider<S, P, E>
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(
                publication,
                initialLocator,
                preferences ?: pdfEngineProvider.createEmptyPreferences(),
                listener,
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

    private val viewModel: PdfNavigatorViewModel<S, P> by viewModels {
        PdfNavigatorViewModel.createFactory(
            requireActivity().application,
            publication,
            initialLocator,
            initialPreferences = initialPreferences,
            pdfEngineProvider = pdfEngineProvider
        )
    }

    private lateinit var documentFragment: StateFlow<PdfDocumentFragment<S>?>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Clears the savedInstanceState to prevent the child fragment manager from restoring the
        // pdfFragment, as the [ResourceDataProvider] is not [Parcelable].
        super.onCreate(null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = FragmentContainerView(inflater.context)
        view.id = R.id.readium_pdf_container
        return KeyInterceptorView(view, this, inputListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        documentFragment = viewModel.currentLocator
            .distinctUntilChanged { old, new ->
                old.href == new.href
            }
            .map { locator ->
                createPdfDocumentFragment(locator, settings.value)
            }
            .stateIn(viewLifecycleOwner.lifecycleScope, started = SharingStarted.Eagerly, null)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentFragment
                    .filterNotNull()
                    .onEach { fragment: PdfDocumentFragment<S> ->
                        childFragmentManager.commitNow {
                            replace(R.id.readium_pdf_container, fragment, "readium_pdf_fragment")
                        }
                    }
                    .launchIn(this)

                settings
                    .onEach { settings ->
                        documentFragment.value?.settings = settings
                    }
                    .launchIn(this)
            }
        }
    }

    private suspend fun createPdfDocumentFragment(locator: Locator, settings: S): PdfDocumentFragment<S>? {
        val link = viewModel.findLink(locator) ?: return null

        return try {
            val pageIndex = (locator.locations.page ?: 1) - 1
            pdfEngineProvider.createDocumentFragment(
                PdfDocumentFragmentInput(
                    publication = publication,
                    link = link,
                    initialPageIndex = pageIndex,
                    settings = settings,
                    listener = DocumentFragmentListener()
                )
            )
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

        override fun onTap(point: PointF): Boolean =
            inputListener.onTap(this@PdfNavigatorFragment, TapEvent(point))

        override fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
            listener?.onResourceLoadFailed(link, error)
        }
    }

    // Configurable

    @Suppress("Unchecked_cast")
    override val settings: StateFlow<S> get() = viewModel.settings as StateFlow<S>

    override fun submitPreferences(preferences: P) {
        viewModel.submitPreferences(preferences)
    }

    // Navigator

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
        get() = when (presentation.value.readingProgression) {
            ReadingProgression.LTR -> PublicationReadingProgression.LTR
            ReadingProgression.RTL -> PublicationReadingProgression.RTL
        }

    private val inputListener = CompositeInputListener()

    override fun addInputListener(listener: InputListener) {
        inputListener.add(listener)
    }

    override fun removeInputListener(listener: InputListener) {
        inputListener.remove(listener)
    }
}
