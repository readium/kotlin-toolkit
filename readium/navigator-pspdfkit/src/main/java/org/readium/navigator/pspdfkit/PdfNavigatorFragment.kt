/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.fragmentParameters
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.mediatype.MediaType

@PdfSupport
@OptIn(InternalReadiumApi::class, ExperimentalCoroutinesApi::class)
class PdfNavigatorFragment private constructor(
    override val publication: Publication,
    initialLocator: Locator? = null,
    private val listener: Listener?,
    private val documentFragmentFactory: PdfDocumentFragmentFactory
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
            documentFragmentFactory: PdfDocumentFragmentFactory,
        ): FragmentFactory = createFragmentFactory {
            PdfNavigatorFragment(publication, initialLocator, listener, documentFragmentFactory)
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
        PdfNavigatorViewModel.createFactory(requireActivity().application, publication, initialLocator)
    }

    private val documentFragment: StateFlow<PdfDocumentFragment?> by lazy {
        viewModel.currentDocument
            .mapLatest { link -> documentFragmentFactory(publication, link, documentFragmentListener) }
            .stateIn(viewLifecycleOwner.lifecycleScope, started = SharingStarted.Eagerly, null)
    }

    private val documentFragmentListener = DocumentFragmentListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Clears the savedInstanceState to prevent the child fragment manager from restoring the
        // pdfFragment, as the [ResourceDataProvider] is not [Parcelable].
        super.onCreate(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = FragmentContainerView(inflater.context)
        savedInstanceState?.clear()
        view.id = R.id.readium_pspdfkit_container

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                documentFragment
                    .filterNotNull()
                    .onEach { fragment ->
                        childFragmentManager.commit {
                            replace(R.id.readium_pspdfkit_container, fragment, "readium_pdf_fragment")
                        }
                    }
                    .launchIn(this)
            }
        }
//           go(currentLocator.value, animated = false)

        return view
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

    private inner class DocumentFragmentListener : PdfDocumentFragment.Listener {
        override fun onPageChanged(pageIndex: Int) {
            viewModel.onPageChanged(pageIndex)
        }

        override fun onTap(point: PointF): Boolean {
            return listener?.onTap(point) ?: false
        }
    }
}

@OptIn(InternalReadiumApi::class)
private val Locator.Locations.page: Int? get() =
    fragmentParameters["page"]?.toIntOrNull()
