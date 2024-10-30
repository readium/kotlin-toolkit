/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.Configurable
import org.readium.navigator.common.Navigator
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.Overflowable
import org.readium.navigator.common.RenditionState
import org.readium.navigator.web.layout.Layout
import org.readium.navigator.web.layout.LayoutResolver
import org.readium.navigator.web.layout.ReadingOrder
import org.readium.navigator.web.location.FixedWebGoLocation
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.location.HrefLocation
import org.readium.navigator.web.preferences.FixedWebSettings
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link

@ExperimentalReadiumApi
@Stable
public class FixedWebRenditionState internal constructor(
    internal val readingOrder: ReadingOrder,
    initialSettings: FixedWebSettings,
    initialLocation: FixedWebGoLocation,
    internal val webViewServer: WebViewServer,
    internal val preloadedData: FixedWebPreloadedData
) : RenditionState<FixedWebNavigator> {

    private val navigatorState: MutableState<FixedWebNavigator?> =
        mutableStateOf(null)

    override val navigator: FixedWebNavigator? get() =
        navigatorState.value

    internal val layoutDelegate: LayoutDelegate =
        LayoutDelegate(
            readingOrder,
            initialSettings
        )

    internal val webViewClient: WebViewClient =
        WebViewClient(webViewServer)

    internal val pagerState: PagerState = run {
        val initialPage = when (initialLocation) {
            is HrefLocation -> layoutDelegate.layout.value.spreadIndexForPage(initialLocation.href)
        }

        PagerState(
            currentPage = layoutDelegate.layout.value.spreadIndexForPage(initialPage),
            pageCount = { layoutDelegate.layout.value.spreads.size }
        )
    }

    private lateinit var navigationDelegate: NavigationDelegate

    internal fun updateLocation(location: FixedWebLocation) {
        initNavigatorIfNeeded(location)
        navigationDelegate.updateLocation(location)
    }

    private fun initNavigatorIfNeeded(location: FixedWebLocation) {
        if (navigator != null) {
            return
        }

        navigationDelegate =
            NavigationDelegate(
                readingOrder,
                pagerState,
                layoutDelegate.layout,
                layoutDelegate.settings,
                location
            )
        navigatorState.value =
            FixedWebNavigator(
                navigationDelegate,
                layoutDelegate
            )
    }
}

@ExperimentalReadiumApi
@Stable
public class FixedWebNavigator internal constructor(
    private val navigationDelegate: NavigationDelegate,
    layoutDelegate: LayoutDelegate
) : Navigator<FixedWebLocation, FixedWebGoLocation> by navigationDelegate,
    Overflowable by navigationDelegate,
    Configurable<FixedWebSettings> by layoutDelegate

internal data class FixedWebPreloadedData(
    val fixedSingleContent: String,
    val fixedDoubleContent: String
)

@OptIn(ExperimentalReadiumApi::class)
internal class LayoutDelegate(
    readingOrder: ReadingOrder,
    initialSettings: FixedWebSettings
) : Configurable<FixedWebSettings> {

    private val layoutResolver =
        LayoutResolver(readingOrder)

    override val settings: MutableState<FixedWebSettings> =
        mutableStateOf(initialSettings)

    val layout: State<Layout> =
        derivedStateOf {
            val spreads = layoutResolver.layout(settings.value)
            Layout(settings.value.readingProgression, spreads)
        }

    val fit: State<Fit> =
        derivedStateOf { settings.value.fit }
}

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
internal class NavigationDelegate(
    private val readingOrder: ReadingOrder,
    private val pagerState: PagerState,
    private val layout: State<Layout>,
    private val settings: State<FixedWebSettings>,
    initialLocation: FixedWebLocation
) : Navigator<FixedWebLocation, FixedWebGoLocation>, Overflowable {

    private val locationMutable: MutableState<FixedWebLocation> =
        mutableStateOf(initialLocation)

    internal fun updateLocation(location: FixedWebLocation) {
        locationMutable.value = location
    }

    override val location: State<FixedWebLocation> =
        locationMutable

    override suspend fun goTo(link: Link) {
        val href = link.url().removeFragment()
        val location = HrefLocation(href)
        goTo(location)
    }

    override suspend fun goTo(location: FixedWebGoLocation) {
        when (location) {
            is HrefLocation -> {
                val pageIndex = checkNotNull(readingOrder.indexOfHref(location.href))
                pagerState.scrollToPage(layout.value.spreadIndexForPage(pageIndex))
            }
        }
    }

    override suspend fun goTo(location: FixedWebLocation) {
        return goTo(HrefLocation(location.href))
    }

    override val overflow: State<Overflow> =
        derivedStateOf {
            SimpleOverflow(
                settings.value.readingProgression,
                false,
                Axis.HORIZONTAL
            )
        }

    override val canMoveForward: Boolean
        get() = pagerState.currentPage < layout.value.spreads.size - 1

    override val canMoveBackward: Boolean
        get() = pagerState.currentPage > 0

    override suspend fun moveForward() {
        if (canMoveForward) {
            pagerState.scrollToPage(pagerState.currentPage + 1)
        }
    }

    override suspend fun moveBackward() {
        if (canMoveBackward) {
            pagerState.scrollToPage(pagerState.currentPage - 1)
        }
    }
}
