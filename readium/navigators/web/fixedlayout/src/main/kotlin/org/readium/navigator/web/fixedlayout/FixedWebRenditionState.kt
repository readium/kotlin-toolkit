/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.navigator.web.fixedlayout

import android.app.Application
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlin.coroutines.coroutineContext
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.readium.navigator.common.Decoration
import org.readium.navigator.common.DecorationController
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.Selection
import org.readium.navigator.common.SelectionController
import org.readium.navigator.common.SettingsController
import org.readium.navigator.common.SimpleOverflow
import org.readium.navigator.common.TextQuote
import org.readium.navigator.web.common.WebDecorationTemplate
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.navigator.web.fixedlayout.injection.injectHtmlFixedLayout
import org.readium.navigator.web.fixedlayout.layout.DoubleViewportSpread
import org.readium.navigator.web.fixedlayout.layout.Layout
import org.readium.navigator.web.fixedlayout.layout.LayoutResolver
import org.readium.navigator.web.fixedlayout.layout.Page
import org.readium.navigator.web.fixedlayout.layout.SingleViewportSpread
import org.readium.navigator.web.fixedlayout.preferences.FixedWebSettings
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.navigator.web.internals.server.WebViewServer.Companion.assetsBaseHref
import org.readium.navigator.web.internals.util.HyperlinkProcessor
import org.readium.navigator.web.internals.webapi.Decoration as WebApiDecoration
import org.readium.navigator.web.internals.webapi.FixedDoubleSelectionApi
import org.readium.navigator.web.internals.webapi.FixedSelectionApi
import org.readium.navigator.web.internals.webapi.FixedSingleSelectionApi
import org.readium.navigator.web.internals.webapi.Iframe
import org.readium.navigator.web.internals.webapi.Selection as WebApiSelection
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

/**
 * State holder for the rendition of a fixed Web publication.
 *
 * You can interact with it mainly through its [controller] witch will be available as soon
 * as the first composition has completed.
 */
@ExperimentalReadiumApi
@Stable
public class FixedWebRenditionState internal constructor(
    application: Application,
    internal val publication: FixedWebPublication,
    disableSelection: Boolean,
    initialSettings: FixedWebSettings,
    initialLocation: FixedWebGoLocation,
    configuration: FixedWebConfiguration,
    internal val preloadedData: FixedWebPreloadedData,
) : RenditionState<FixedWebRenditionController> {

    private val controllerState: MutableState<FixedWebRenditionController?> =
        mutableStateOf(null)

    override val controller: FixedWebRenditionController? by controllerState

    internal val layoutDelegate: FixedLayoutDelegate =
        FixedLayoutDelegate(
            publication.readingOrder,
            initialSettings
        )

    private val initialSpread = layoutDelegate.layout.value
        .spreadIndexForHref(initialLocation.href)
        ?: 0

    internal val pagerState: PagerState =
        PagerState(
            currentPage = initialSpread,
            pageCount = { layoutDelegate.layout.value.spreads.size }
        )

    internal val selectionDelegate: FixedSelectionDelegate =
        FixedSelectionDelegate(
            pagerState = pagerState,
            layout = layoutDelegate.layout
        )

    internal val decorationDelegate: FixedDecorationDelegate =
        FixedDecorationDelegate(configuration.decorationTemplates)

    internal val hyperlinkProcessor =
        HyperlinkProcessor(publication.container)

    private val webViewServer = run {
        val htmlInjector: (Resource, MediaType) -> Resource = { resource, mediaType ->
            resource.injectHtmlFixedLayout(
                charset = mediaType.charset,
                injectableScript = RelativeUrl("readium/navigator/web/internals/generated/fixed-injectable-script.js")!!,
                assetsBaseHref = assetsBaseHref,
                disableSelection = disableSelection
            )
        }

        WebViewServer(
            application = application,
            container = publication.container,
            mediaTypes = publication.mediaTypes,
            errorPage = RelativeUrl("readium/navigator/web/internals/error.xhtml")!!,
            htmlInjector = htmlInjector,
            servedAssets = configuration.servedAssets + listOf("readium/.*"),
            onResourceLoadFailed = { _, _ -> }
        )
    }

    internal val webViewClient: WebViewClient =
        WebViewClient(webViewServer)

    internal lateinit var navigationDelegate: FixedNavigationDelegate

    internal fun initController(location: FixedWebLocation) {
        navigationDelegate =
            FixedNavigationDelegate(
                pagerState,
                layoutDelegate.layout,
                layoutDelegate.overflow,
                location
            )
        controllerState.value =
            FixedWebRenditionController(
                navigationDelegate,
                layoutDelegate,
                decorationDelegate,
                selectionDelegate
            )
        navigationDelegate.updateLocation(location)
    }
}

@ExperimentalReadiumApi
@Stable
public class FixedWebRenditionController internal constructor(
    private val navigationDelegate: FixedNavigationDelegate,
    layoutDelegate: FixedLayoutDelegate,
    decorationDelegate: FixedDecorationDelegate,
    selectionDelegate: FixedSelectionDelegate,
) : NavigationController<FixedWebLocation, FixedWebGoLocation> by navigationDelegate,
    OverflowController by navigationDelegate,
    SettingsController<FixedWebSettings> by layoutDelegate,
    SelectionController<FixedWebSelectionLocation> by selectionDelegate,
    DecorationController<FixedWebDecorationLocation> by decorationDelegate

internal data class FixedWebPreloadedData(
    val fixedSingleContent: String,
    val fixedDoubleContent: String,
)

internal class FixedLayoutDelegate(
    readingOrder: FixedWebPublication.ReadingOrder,
    initialSettings: FixedWebSettings,
) : SettingsController<FixedWebSettings> {

    private val layoutResolver =
        LayoutResolver(readingOrder)

    override var settings: FixedWebSettings by mutableStateOf(initialSettings)

    val overflow: State<Overflow> = derivedStateOf {
        with(settings) {
            SimpleOverflow(
                readingProgression = readingProgression,
                scroll = false,
                axis = Axis.HORIZONTAL
            )
        }
    }

    val layout: State<Layout> = derivedStateOf {
        val spreads = layoutResolver.layout(settings)
        Layout(settings.readingProgression, spreads)
    }

    val fit: State<Fit> =
        derivedStateOf { settings.fit }
}

internal class FixedNavigationDelegate(
    private val pagerState: PagerState,
    private val layout: State<Layout>,
    overflowState: State<Overflow>,
    initialLocation: FixedWebLocation,
) : NavigationController<FixedWebLocation, FixedWebGoLocation>, OverflowController {

    private val locationMutable: MutableState<FixedWebLocation> =
        mutableStateOf(initialLocation)

    internal fun updateLocation(location: FixedWebLocation) {
        locationMutable.value = location
    }
    override val overflow: Overflow by overflowState

    override val location: FixedWebLocation by locationMutable

    override suspend fun goTo(url: Url) {
        goTo(FixedWebGoLocation(href = url.removeFragment()))
    }

    override suspend fun goTo(location: FixedWebGoLocation) {
        val spreadIndex = layout.value.spreadIndexForHref(location.href) ?: return
        pagerState.scrollToPage(spreadIndex)
    }

    override suspend fun goTo(location: FixedWebLocation) {
        goTo(FixedWebGoLocation(location.href))
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

internal class FixedDecorationDelegate(
    internal val decorationTemplates: WebDecorationTemplates,
) : DecorationController<FixedWebDecorationLocation> {

    override var decorations: PersistentMap<String, PersistentList<FixedWebDecoration>> by
        mutableStateOf(persistentMapOf<String, PersistentList<Decoration<FixedWebDecorationLocation>>>())
}

internal class FixedSelectionDelegate(
    private val pagerState: PagerState,
    private val layout: State<Layout>,
) : SelectionController<FixedWebSelectionLocation> {

    val selectionApis: SnapshotStateMap<Int, FixedSelectionApi?> =
        mutableStateMapOf()

    override suspend fun currentSelection(): Selection<FixedWebSelectionLocation>? {
        val visiblePages = pagerState.layoutInfo.visiblePagesInfo.map { it.index }
        val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
        val (page, selection) = visiblePages
            .mapNotNull { index -> selectionApis[index]?.let { index to it } }
            .map { (index, api) ->
                coroutineScope.async {
                    api.getCurrentSelection(index, layout.value)
                }
            }.awaitAll()
            .filterNotNull()
            .firstOrNull()
            ?: return null

        return Selection(
            selection.selectedText,
            selection.selectionRect,
            FixedWebSelectionLocation(
                href = page.href,
                mediaType = page.mediaType ?: MediaType.XHTML,
                selectedText = selection.selectedText,
                textQuote = TextQuote(
                    text = selection.selectedText,
                    prefix = selection.textBefore,
                    suffix = selection.textAfter,
                )
            )
        )
    }

    private suspend fun FixedSelectionApi.getCurrentSelection(
        index: Int,
        layout: Layout,
    ): Pair<Page, WebApiSelection>? = when (this) {
        is FixedDoubleSelectionApi -> {
            val (iframe, selection) = getCurrentSelection() ?: return null
            val spread = layout.spreads[index] as DoubleViewportSpread
            val page = when (iframe) {
                Iframe.Left -> spread.leftPage!!
                Iframe.Right -> spread.rightPage!!
            }
            page to selection
        }
        is FixedSingleSelectionApi -> {
            val selection = getCurrentSelection() ?: return null
            val page = (layout.spreads[index] as SingleViewportSpread).page
            page to selection
        }
    }

    override fun clearSelection() {
        for (api in selectionApis.values) {
            when (api) {
                is FixedDoubleSelectionApi -> {
                    api.clearSelection()
                }
                is FixedSingleSelectionApi -> {
                    api.clearSelection()
                }
                null -> {}
            }
        }
    }
}

internal fun FixedWebDecoration.toWebApiDecoration(
    template: WebDecorationTemplate,
): WebApiDecoration {
    val element = template.element(style)
    val cssSelector = when (location) {
        is FixedWebDecorationCssSelectorLocation ->
            (location as FixedWebDecorationCssSelectorLocation).cssSelector
        is FixedWebDecorationTextQuoteLocation ->
            (location as FixedWebDecorationTextQuoteLocation).cssSelector
    }
    val textQuote = (location as? FixedWebDecorationTextQuoteLocation)?.textQuote
    return WebApiDecoration(
        id = id,
        style = style,
        element = element,
        cssSelector = cssSelector,
        textQuote = textQuote
    )
}
