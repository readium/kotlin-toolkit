/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator.image

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.NavigatorFragment
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.RestorationNotSupportedException
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.databinding.ReadiumNavigatorViewpagerBinding
import org.readium.r2.navigator.dummyPublication
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.navigator.input.CompositeInputListener
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyInterceptorView
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pager.R2CbzPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Navigator for bitmap-based publications, such as CBZ.
 */
@OptIn(ExperimentalReadiumApi::class, DelicateReadiumApi::class)
public class ImageNavigatorFragment private constructor(
    publication: Publication,
    private val initialLocator: Locator? = null,
    internal val listener: Listener? = null,
) : NavigatorFragment(publication), OverflowableNavigator {

    public interface Listener : VisualNavigator.Listener

    internal lateinit var positions: List<Locator>
    private lateinit var resourcePager: R2ViewPager

    internal lateinit var adapter: R2PagerAdapter
    private lateinit var currentActivity: FragmentActivity

    override val currentLocator: StateFlow<Locator> get() = _currentLocator
    private val _currentLocator = MutableStateFlow(
        initialLocator?.let { publication.normalizeLocator(it) }
            ?: requireNotNull(publication.locatorFromLink(publication.readingOrder.first()))
    )

    private var currentPagerPosition: Int = 0
    internal var resources: List<String> = emptyList()

    private var _binding: ReadiumNavigatorViewpagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = createFragmentFactory {
            R2CbzPageFragment(publication) { x, y ->
                inputListener.onTap(
                    TapEvent(PointF(x, y))
                )
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        currentActivity = requireActivity()
        _binding = ReadiumNavigatorViewpagerBinding.inflate(inflater, container, false)
        val view = binding.root

        resourcePager = binding.resourcePager
        resourcePager.publicationType = R2ViewPager.PublicationType.CBZ

        positions = runBlocking { publication.positions() }

        resourcePager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                notifyCurrentLocation()
            }
        })

        val resources = publication.readingOrder
            .map { R2PagerAdapter.PageResource.Cbz(it) }
        adapter = R2PagerAdapter(childFragmentManager, resources)

        resourcePager.adapter = adapter

        if (currentPagerPosition == 0) {
            if (requireActivity().layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resources.size - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = currentPagerPosition
            }
        } else {
            resourcePager.currentItem = currentPagerPosition
        }

        if (initialLocator != null) {
            go(initialLocator)
        }

        return KeyInterceptorView(view, inputListener)
    }

    override fun onStart() {
        super.onStart()

        // OnPageChangeListener.onPageSelected is not called on the first page of the book, so we
        // trigger the locationDidChange event manually.
        notifyCurrentLocation()
    }

    override fun onResume() {
        super.onResume()

        if (publication == dummyPublication) {
            throw RestorationNotSupportedException
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun notifyCurrentLocation() {
        val locator = positions.getOrNull(resourcePager.currentItem)
            ?.takeUnless { it == _currentLocator.value }
            ?: return

        _currentLocator.value = locator
    }

    override fun go(locator: Locator, animated: Boolean): Boolean {
        @Suppress("NAME_SHADOWING")
        val locator = publication.normalizeLocator(locator)

        val resourceIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return false

        listener?.onJumpToLocator(locator)
        currentPagerPosition = resourceIndex
        resourcePager.currentItem = currentPagerPosition

        return true
    }

    override fun go(link: Link, animated: Boolean): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated)
    }

    override fun goForward(animated: Boolean): Boolean {
        val current = resourcePager.currentItem
        if (requireActivity().layoutDirectionIsRTL()) {
            // The view has RTL layout
            resourcePager.currentItem = current - 1
        } else {
            // The view has LTR layout
            resourcePager.currentItem = current + 1
        }

        notifyCurrentLocation()
        return current != resourcePager.currentItem
    }

    override fun goBackward(animated: Boolean): Boolean {
        val current = resourcePager.currentItem
        if (requireActivity().layoutDirectionIsRTL()) {
            // The view has RTL layout
            resourcePager.currentItem = current + 1
        } else {
            // The view has LTR layout
            resourcePager.currentItem = current - 1
        }

        notifyCurrentLocation()
        return current != resourcePager.currentItem
    }

    // VisualNavigator

    override val publicationView: View
        get() = requireView()

    @ExperimentalReadiumApi
    override val overflow: StateFlow<OverflowableNavigator.Overflow> =
        MutableStateFlow(
            SimpleOverflow(
                readingProgression = when (publication.metadata.readingProgression) {
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    else -> ReadingProgression.LTR
                },
                scroll = false,
                axis = Axis.HORIZONTAL
            )
        ).asStateFlow()

    private val inputListener = CompositeInputListener()

    override fun addInputListener(listener: InputListener) {
        inputListener.add(listener)
    }

    override fun removeInputListener(listener: InputListener) {
        inputListener.remove(listener)
    }

    public companion object {

        /**
         * Factory for [ImageNavigatorFragment].
         *
         * @param publication Bitmap-based publication to render in the navigator.
         * @param initialLocator The first location which should be visible when rendering the
         *        publication. Can be used to restore the last reading location.
         * @param listener Optional listener to implement to observe events, such as user taps.
         */
        public fun createFactory(
            publication: Publication,
            initialLocator: Locator? = null,
            listener: Listener? = null,
        ): FragmentFactory =
            createFragmentFactory { ImageNavigatorFragment(publication, initialLocator, listener) }

        /**
         * Creates a factory for a dummy [ImageNavigatorFragment].
         *
         * Used when Android restore the [ImageNavigatorFragment] after the process was killed. You
         * need to make sure the fragment is removed from the screen before `onResume` is called.
         */
        public fun createDummyFactory(): FragmentFactory = createFragmentFactory {
            ImageNavigatorFragment(
                publication = dummyPublication,
                initialLocator = Locator(href = Url("#")!!, mediaType = MediaType.JPEG),
                listener = null
            )
        }
    }
}
