/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.image

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.image.databinding.ReadiumImageNavigatorFragmentLayoutBinding
import org.readium.navigator.image.preferences.ImagePreferences
import org.readium.navigator.image.preferences.ImageSettings
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression

/**
 * Navigator for bitmap-based publications, such as CBZ.
 */
@ExperimentalReadiumApi
class ImageNavigatorFragment private constructor(
    private val navigatorState: ImageNavigatorState,
    internal val listener: Listener? = null
) : Fragment(), VisualNavigator, Configurable<ImageSettings, ImagePreferences> {

    private var _binding: ReadiumImageNavigatorFragmentLayoutBinding? = null

    private val binding get() = _binding!!

    interface Listener : VisualNavigator.Listener

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val publication: Publication get() =
        navigatorState.publication

    override val currentLocator: StateFlow<Locator> get() =
        navigatorState.locator

    override val readingProgression: PublicationReadingProgression get() =
        when (navigatorState.presentation.value.readingProgression) {
            ReadingProgression.LTR -> PublicationReadingProgression.LTR
            ReadingProgression.RTL -> PublicationReadingProgression.RTL
        }

    override val presentation: StateFlow<VisualNavigator.Presentation> get() =
       navigatorState.presentation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding =  ReadiumImageNavigatorFragmentLayoutBinding.inflate(inflater, container, false)
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
               ImageNavigator(
                   modifier = Modifier.fillMaxSize(),
                   onTap = { offset -> listener?.onTap(offset.toPoint()) },
                   state = navigatorState
               )
            }
        }
        return binding.root
    }

    private fun Offset.toPoint() = PointF(x, y)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override val settings: StateFlow<ImageSettings> get() =
        navigatorState.settings

    override fun submitPreferences(preferences: ImagePreferences) {
        navigatorState.submitPreferences(preferences)
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        if (publication.readingOrder.indexOfFirstWithHref(locator.href) == null)
               return false

        coroutineScope.launch {
            navigatorState.go(locator)
            listener?.onJumpToLocator(locator)
        }
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        coroutineScope.launch { navigatorState.goForward() }
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        coroutineScope.launch { navigatorState.goBackward() }
        return true
    }

    companion object {

        /**
         * Factory for [ImageNavigatorFragment].
         *
         * @param navigatorState State of the image navigator.
         * @param listener Optional listener to implement to observe events, such as user taps.
         */
        fun createFactory(navigatorState: ImageNavigatorState, listener: Listener? = null): FragmentFactory =
            createFragmentFactory { ImageNavigatorFragment(navigatorState, listener) }
    }
}
