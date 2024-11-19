/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.app.Application
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.HyperlinkLocation
import org.readium.navigator.common.LinkContext
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.SettingsController
import org.readium.navigator.web.layout.Layout
import org.readium.navigator.web.layout.LayoutResolver
import org.readium.navigator.web.layout.ReadingOrder
import org.readium.navigator.web.location.FixedWebGoLocation
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.preferences.FixedWebSettings
import org.readium.navigator.web.util.HyperlinkProcessor
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

@ExperimentalReadiumApi
@Stable
public class FixedWebRenditionState internal constructor(
    application: Application,
    internal val readingOrder: ReadingOrder,
    container: Container<Resource>,
    resourceMediaTypes: Map<Url, MediaType>,
    isRestricted: Boolean,
    initialSettings: FixedWebSettings,
    initialLocation: FixedWebGoLocation,
    internal val preloadedData: FixedWebPreloadedData,
) : RenditionState<FixedWebRenditionController> {

    private val navigatorState: MutableState<FixedWebRenditionController?> =
        mutableStateOf(null)

    override val controller: FixedWebRenditionController? get() =
        navigatorState.value

    internal val layoutDelegate: LayoutDelegate =
        LayoutDelegate(
            readingOrder,
            initialSettings
        )

    internal val hyperlinkProcessor =
        HyperlinkProcessor(container)

    private val webViewServer =
        WebViewServer(
            application = application,
            container = container,
            mediaTypes = resourceMediaTypes,
            errorPage = RelativeUrl("readium/navigators/web/error.xhtml")!!,
            injectableScript = RelativeUrl("readium/navigators/web/fixed-injectable-script.js")!!,
            servedAssets = listOf("readium/.*"),
            disableSelection = isRestricted,
            onResourceLoadFailed = { _, _ -> }
        )

    internal val webViewClient: WebViewClient =
        WebViewClient(webViewServer)

    internal val pagerState: PagerState = run {
        val initialSpread = layoutDelegate.layout.value
            .spreadIndexForHref(initialLocation.href)
            ?: 0

        PagerState(
            currentPage = initialSpread,
            pageCount = { layoutDelegate.layout.value.spreads.size }
        )
    }

    internal suspend fun computeHyperlinkContext(originUrl: Url, outerHtml: String): LinkContext? =
        hyperlinkProcessor.computeLinkContext(originUrl, outerHtml)

    private lateinit var navigationDelegate: NavigationDelegate

    internal fun updateLocation(location: FixedWebLocation) {
        initControllerIfNeeded(location)
        navigationDelegate.updateLocation(location)
    }

    private fun initControllerIfNeeded(location: FixedWebLocation) {
        if (controller != null) {
            return
        }

        navigationDelegate =
            NavigationDelegate(
                pagerState,
                layoutDelegate.layout,
                layoutDelegate.settings,
                location
            )
        navigatorState.value =
            FixedWebRenditionController(
                navigationDelegate,
                layoutDelegate
            )
    }
}

@ExperimentalReadiumApi
@Stable
public class FixedWebRenditionController internal constructor(
    private val navigationDelegate: NavigationDelegate,
    layoutDelegate: LayoutDelegate,
) : NavigationController<FixedWebLocation, FixedWebGoLocation> by navigationDelegate,
    OverflowController by navigationDelegate,
    SettingsController<FixedWebSettings> by layoutDelegate

internal data class FixedWebPreloadedData(
    val fixedSingleContent: String,
    val fixedDoubleContent: String,
)

@OptIn(ExperimentalReadiumApi::class)
internal class LayoutDelegate(
    readingOrder: ReadingOrder,
    initialSettings: FixedWebSettings,
) : SettingsController<FixedWebSettings> {

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
    private val pagerState: PagerState,
    private val layout: State<Layout>,
    private val settings: State<FixedWebSettings>,
    initialLocation: FixedWebLocation,
) : NavigationController<FixedWebLocation, FixedWebGoLocation>, OverflowController {

    private val locationMutable: MutableState<FixedWebLocation> =
        mutableStateOf(initialLocation)

    internal fun updateLocation(location: FixedWebLocation) {
        locationMutable.value = location
    }

    override val location: State<FixedWebLocation> =
        locationMutable

    override suspend fun goTo(location: HyperlinkLocation) {
        goTo(FixedWebGoLocation(location.href))
    }

    override suspend fun goTo(location: FixedWebGoLocation) {
        val spreadIndex = layout.value.spreadIndexForHref(location.href) ?: return
        pagerState.scrollToPage(spreadIndex)
    }

    override suspend fun goTo(location: FixedWebLocation) {
        goTo(FixedWebGoLocation(location.href))
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
