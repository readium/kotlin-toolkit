/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable

import android.app.Application
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.readium.navigator.common.DecorationController
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.Progression
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.Selection
import org.readium.navigator.common.SelectionController
import org.readium.navigator.common.SettingsController
import org.readium.navigator.common.SimpleOverflow
import org.readium.navigator.common.TextQuote
import org.readium.navigator.web.common.FontFamilyDeclaration
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.navigator.web.internals.pager.RenditionScrollState
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.navigator.web.internals.server.WebViewServer.Companion.assetsBaseHref
import org.readium.navigator.web.internals.util.HyperlinkProcessor
import org.readium.navigator.web.internals.util.toLayoutDirection
import org.readium.navigator.web.internals.util.toOrientation
import org.readium.navigator.web.internals.webapi.ReflowableSelectionApi
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.navigator.web.reflowable.css.PaginatedLayoutResolver
import org.readium.navigator.web.reflowable.css.ReadiumCssInjector
import org.readium.navigator.web.reflowable.css.RsProperties
import org.readium.navigator.web.reflowable.css.UserProperties
import org.readium.navigator.web.reflowable.css.withLayout
import org.readium.navigator.web.reflowable.css.withSettings
import org.readium.navigator.web.reflowable.injection.injectHtmlReflowable
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.navigator.web.reflowable.resource.ReflowableResourceState
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

/**
 * State holder for the rendition of a reflowable Web publication.
 *
 * You can interact with it mainly through its [controller] witch will be available as soon
 * as the first composition has completed.
 */
@ExperimentalReadiumApi
@Stable
public class ReflowableWebRenditionState internal constructor(
    application: Application,
    internal val publication: ReflowableWebPublication,
    initialSettings: ReflowableWebSettings,
    initialLocation: ReflowableWebGoLocation,
    configuration: ReflowableWebConfiguration,
    disableSelection: Boolean,
) : RenditionState<ReflowableWebRenditionController> {

    private val controllerState: MutableState<ReflowableWebRenditionController?> =
        mutableStateOf(null)

    override val controller: ReflowableWebRenditionController? by controllerState

    private val initialResource = publication.readingOrder
        .indexOfHref(initialLocation.href)
        ?: 0

    internal val resourceStates: List<ReflowableResourceState> =
        publication.readingOrder.items.mapIndexed { index, item ->
            val progression = when {
                index < initialResource -> 1.0
                index > initialResource -> 0.0
                else -> initialLocation.progression?.value ?: 0.0
            }
            ReflowableResourceState(
                index = index,
                href = item.href,
                progression = Progression(progression)!!
            )
        }

    private val fontFamilyDeclarations: List<FontFamilyDeclaration> =
        buildList {
            addAll(configuration.fontFamilyDeclarations.declarations)
            add(
                FontFamilyDeclaration(
                    fontFamily = FontFamily.OPEN_DYSLEXIC.name,
                    alternates = persistentListOf()
                ) {
                    addFontFace {
                        addSource("readium/fonts/OpenDyslexic-Regular.otf")
                    }
                }
            )
        }

    internal val layoutDelegate: ReflowableLayoutDelegate =
        ReflowableLayoutDelegate(
            fontFamilyDeclarations,
            initialSettings
        )

    internal val pagerState: PagerState =
        PagerState(
            currentPage = initialResource,
            pageCount = { publication.readingOrder.size }
        )

    internal val scrollState: RenditionScrollState =
        RenditionScrollState(
            pagerState = pagerState,
            pageStates = resourceStates,
            overflow = layoutDelegate.overflow,
        )

    internal val selectionDelegate: ReflowableSelectionDelegate =
        ReflowableSelectionDelegate(
            publication = publication,
            pagerState = pagerState
        )

    internal val decorationDelegate: ReflowableDecorationDelegate =
        ReflowableDecorationDelegate(configuration.decorationTemplates)

    internal val hyperlinkProcessor =
        HyperlinkProcessor(publication.container)

    internal val webViewClient: WebViewClient = run {
        val htmlInjector: (Resource, MediaType) -> Resource = { resource, mediaType ->
            resource.injectHtmlReflowable(
                charset = mediaType.charset,
                readiumCss = layoutDelegate.readiumCssInjector,
                injectableScript = RelativeUrl("readium/navigator/web/internals/generated/reflowable-injectable-script.js")!!,
                assetsBaseHref = assetsBaseHref,
                disableSelection = disableSelection
            )
        }

        val webViewServer =
            WebViewServer(
                application = application,
                container = publication.container,
                mediaTypes = publication.mediaTypes,
                errorPage = RelativeUrl("readium/navigator/web/internals/error.xhtml")!!,
                htmlInjector = htmlInjector,
                servedAssets = configuration.servedAssets + listOf("readium/.*"),
                onResourceLoadFailed = { _, _ -> } // TODO: pass errors to the app
            )

        WebViewClient(webViewServer)
    }

    private lateinit var navigationDelegate: ReflowableNavigationDelegate

    internal fun initController(location: ReflowableWebLocation) {
        navigationDelegate =
            ReflowableNavigationDelegate(
                publication.readingOrder,
                resourceStates,
                pagerState,
                layoutDelegate.overflow,
                location
            )
        controllerState.value =
            ReflowableWebRenditionController(
                navigationDelegate,
                layoutDelegate,
                decorationDelegate,
                selectionDelegate
            )
        updateLocation(location)
    }

    internal fun updateLocation(location: ReflowableWebLocation) {
        navigationDelegate.updateLocation(location)
    }
}

@ExperimentalReadiumApi
@Stable
public class ReflowableWebRenditionController internal constructor(
    navigationDelegate: ReflowableNavigationDelegate,
    layoutDelegate: ReflowableLayoutDelegate,
    decorationDelegate: ReflowableDecorationDelegate,
    selectionDelegate: ReflowableSelectionDelegate,
) : NavigationController<ReflowableWebLocation, ReflowableWebGoLocation> by navigationDelegate,
    OverflowController by navigationDelegate,
    SettingsController<ReflowableWebSettings> by layoutDelegate,
    DecorationController<ReflowableWebDecorationLocation> by decorationDelegate,
    SelectionController<ReflowableWebSelectionLocation> by selectionDelegate

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
internal class ReflowableLayoutDelegate(
    fontFamilyDeclarations: List<FontFamilyDeclaration>,
    initialSettings: ReflowableWebSettings,
) : SettingsController<ReflowableWebSettings> {

    private val paginatedLayoutResolver =
        PaginatedLayoutResolver(
            baseMinMargins = 15.dp,
            baseMinLineLength = 200.dp,
            baseOptimalLineLength = 400.dp,
            baseMaxLineLength = 600.dp
        )

    internal var viewportSize: DpSize? by mutableStateOf(null)

    internal var fontScale: Float? by mutableStateOf(null)

    override var settings: ReflowableWebSettings by mutableStateOf(initialSettings)

    internal val overflow: State<Overflow> = derivedStateOf {
        with(settings) {
            SimpleOverflow(
                readingProgression = readingProgression,
                scroll = scroll,
                axis = if (scroll && !verticalText) Axis.VERTICAL else Axis.HORIZONTAL
            )
        }
    }

    internal val readiumCssInjector: ReadiumCssInjector by derivedStateOf {
        ReadiumCssInjector(
            assetsBaseHref = assetsBaseHref,
            readiumCssAssets = RelativeUrl("readium/navigator/web/internals/generated/readium-css/")!!,
            rsProperties = RsProperties(disableVerticalPagination = true),
            userProperties = UserProperties(),
            googleFonts = emptyList(),
            fontFamilyDeclarations = fontFamilyDeclarations
        ).withSettings(
            settings = settings,
        ).let { injector ->
            if (viewportSize == null || fontScale == null || settings.scroll) {
                injector
            } else {
                injector.withLayout(
                    paginatedLayoutResolver.layout(
                        settings = settings,
                        systemFontScale = fontScale!!,
                        viewportWidth = viewportSize!!.width
                    )
                )
            }
        }
    }

    internal val orientation: Orientation get() =
        overflow.value.axis.toOrientation()
}

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
internal class ReflowableNavigationDelegate(
    private val readingOrder: ReflowableWebPublication.ReadingOrder,
    private val resourceStates: List<ReflowableResourceState>,
    private val pagerState: PagerState,
    overflowState: State<Overflow>,
    initialLocation: ReflowableWebLocation,
) : NavigationController<ReflowableWebLocation, ReflowableWebGoLocation>, OverflowController {

    private val locationMutable: MutableState<ReflowableWebLocation> =
        mutableStateOf(initialLocation)

    internal fun updateLocation(location: ReflowableWebLocation) {
        val index = checkNotNull(readingOrder.indexOfHref(location.href))
        resourceStates[index].progression = location.progression
        locationMutable.value = location
    }

    override val overflow: Overflow by overflowState

    override val location: ReflowableWebLocation by locationMutable

    override suspend fun goTo(url: Url) {
        val location = ReflowableWebGoLocation(
            href = url.removeFragment()
            // TODO: use fragment
        )
        goTo(location)
    }

    override suspend fun goTo(location: ReflowableWebGoLocation) {
        val resourceIndex = readingOrder.indexOfHref(location.href) ?: return
        pagerState.scrollToPage(resourceIndex)
        location.progression?.let { // FIXME: goTo returns before the move has completed.
            resourceStates[resourceIndex].progression = it
            // If the scrollController is not available yet, progression will be applied
            // when it becomes available.
            resourceStates[resourceIndex].scrollController.value?.moveToProgression(it)
        }
    }

    override suspend fun goTo(location: ReflowableWebLocation) {
        goTo(ReflowableWebGoLocation(location.href, location.progression))
    }

    // This information is not available when the WebView has not yet been composed or laid out.
    // We assume that the best UI behavior would be to have a possible forward button disabled
    // and return false when we can't tell.
    override val canMoveForward: Boolean
        get() = pagerState.currentPage < readingOrder.items.size - 1 || run {
            val currentResourceState = resourceStates[pagerState.currentPage]
            val scrollController = currentResourceState.scrollController.value ?: return false
            return scrollController.canMoveForward()
        }

    override val canMoveBackward: Boolean
        get() = pagerState.currentPage > 0 || run {
            val currentResourceState = resourceStates[0]
            val scrollController = currentResourceState.scrollController.value ?: return false
            return scrollController.canMoveBackward()
        }

    override suspend fun moveForward() {
        val currentResourceState = resourceStates[pagerState.currentPage]
        val scrollController = currentResourceState.scrollController.value ?: return
        if (scrollController.canMoveForward()) {
            scrollController.moveForward()
        } else if (pagerState.currentPage < readingOrder.items.size - 1) {
            pagerState.scrollToPage(pagerState.currentPage + 1)
        }
    }

    override suspend fun moveBackward() {
        val currentResourceState = resourceStates[pagerState.currentPage]
        val scrollController = currentResourceState.scrollController.value ?: return
        if (scrollController.canMoveBackward()) {
            scrollController.moveBackward()
        } else if (pagerState.currentPage > 0) {
            pagerState.scrollToPage(pagerState.currentPage - 1)
        }
    }

    private fun WebViewScrollController.moveForward() =
        moveForward(
            orientation = overflow.axis.toOrientation(),
            direction = overflow.readingProgression.toLayoutDirection()
        )

    private fun WebViewScrollController.moveBackward() =
        moveBackward(
            orientation = overflow.axis.toOrientation(),
            direction = overflow.readingProgression.toLayoutDirection()
        )

    private fun WebViewScrollController.canMoveForward(): Boolean =
        canMoveForward(
            orientation = overflow.axis.toOrientation(),
            direction = overflow.readingProgression.toLayoutDirection()
        )
    private fun WebViewScrollController.canMoveBackward(): Boolean =
        canMoveBackward(
            orientation = overflow.axis.toOrientation(),
            direction = overflow.readingProgression.toLayoutDirection()
        )

    private fun WebViewScrollController.moveToProgression(progression: Progression) {
        moveToProgression(
            progression = progression.value,
            snap = !overflow.scroll,
            orientation = overflow.axis.toOrientation(),
            direction = overflow.readingProgression.toLayoutDirection()
        )
    }
}

internal class ReflowableDecorationDelegate(
    val decorationTemplates: WebDecorationTemplates,
) : DecorationController<ReflowableWebDecorationLocation> {

    override var decorations: PersistentMap<String, PersistentList<ReflowableWebDecoration>> by
        mutableStateOf(persistentMapOf<String, PersistentList<ReflowableWebDecoration>>())
}

internal class ReflowableSelectionDelegate(
    private val publication: ReflowableWebPublication,
    private val pagerState: PagerState,
) : SelectionController<ReflowableWebSelectionLocation> {

    val selectionApis: SnapshotStateMap<Int, ReflowableSelectionApi?> =
        mutableStateMapOf()

    override suspend fun currentSelection(): Selection<ReflowableWebSelectionLocation>? {
        val visiblePages = pagerState.layoutInfo.visiblePagesInfo.map { it.index }
        val (index, selection) = visiblePages
            .mapNotNull { index -> selectionApis[index]?.let { index to it } }
            .firstNotNullOfOrNull { (index, api) -> api.getCurrentSelection()?.let { index to it } }
            ?: return null

        val selectionItem = publication.readingOrder.items[index]

        return Selection(
            selection.selectedText,
            selection.selectionRect,
            ReflowableWebSelectionLocation(
                href = selectionItem.href,
                mediaType = selectionItem.mediaType,
                selectedText = selection.selectedText,
                textQuote = TextQuote(
                    text = selection.selectedText,
                    prefix = selection.textBefore,
                    suffix = selection.textAfter
                )
            )
        )
    }

    override fun clearSelection() {
        for (api in selectionApis.values) {
            api?.clearSelection()
        }
    }
}
