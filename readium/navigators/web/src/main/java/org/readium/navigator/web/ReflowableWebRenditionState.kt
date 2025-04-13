/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.app.Application
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.HyperlinkLocation
import org.readium.navigator.common.NavigationController
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.OverflowController
import org.readium.navigator.common.RenditionState
import org.readium.navigator.common.SettingsController
import org.readium.navigator.web.css.FontFamilyDeclaration
import org.readium.navigator.web.css.ReadiumCss
import org.readium.navigator.web.css.RsProperties
import org.readium.navigator.web.css.buildFontFamilyDeclaration
import org.readium.navigator.web.css.update
import org.readium.navigator.web.location.ReflowableWebGoLocation
import org.readium.navigator.web.location.ReflowableWebLocation
import org.readium.navigator.web.preferences.ReflowableWebSettings
import org.readium.navigator.web.reflowable.ReflowableResourceState
import org.readium.navigator.web.reflowable.ReflowableWebPublication
import org.readium.navigator.web.util.HyperlinkProcessor
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.util.WebViewServer.Companion.assetsBaseHref
import org.readium.navigator.web.util.injectHtmlReflowable
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

@ExperimentalReadiumApi
@Stable
public class ReflowableWebRenditionState internal constructor(
    application: Application,
    internal val publication: ReflowableWebPublication,
    initialSettings: ReflowableWebSettings,
    initialLocation: ReflowableWebGoLocation,
    private val rsProperties: RsProperties = RsProperties(),
    fontFamilyDeclarations: List<FontFamilyDeclaration>,
    disableSelection: Boolean,
) : RenditionState<ReflowableWebRenditionController> {

    override val controller: ReflowableWebRenditionController? get() =
        controllerState.value

    private val controllerState: MutableState<ReflowableWebRenditionController?> =
        mutableStateOf(null)

    private val initialIndex = publication.readingOrder
        .indexOfHref(initialLocation.href)
        ?: 0

    internal val resourceStates: List<ReflowableResourceState> =
        publication.readingOrder.items.mapIndexed { index, item ->
            ReflowableResourceState(
                index = index,
                href = item.href,
                progression = when {
                    index < initialIndex -> 1.0
                    index > initialIndex -> 0.0
                    else -> initialLocation.progression ?: 0.0
                }
            )
        }

    internal val layoutDelegate: ReflowableLayoutDelegate =
        ReflowableLayoutDelegate(
            initialSettings
        )

    internal val hyperlinkProcessor =
        HyperlinkProcessor(publication.container)

    internal val readiumCss: State<ReadiumCss> =
        derivedStateOf {
            ReadiumCss(
                assetsBaseHref = assetsBaseHref,
                readiumCssAssets = RelativeUrl("readium/navigators/web/generated/readium-css/")!!,
                rsProperties = rsProperties,
                fontFamilyDeclarations =
                buildList {
                    addAll(fontFamilyDeclarations)
                    add(
                        buildFontFamilyDeclaration(
                            fontFamily = FontFamily.OPEN_DYSLEXIC.name,
                            alternates = emptyList()
                        ) {
                            addFontFace {
                                addSource("readium/fonts/OpenDyslexic-Regular.otf")
                            }
                        }
                    )
                }
            ).update(
                settings = layoutDelegate.settings.value,
                useReadiumCssFontSize = true
            )
        }

    private val htmlInjector: (Resource, MediaType) -> Resource = { resource, mediaType ->
        resource.injectHtmlReflowable(
            charset = mediaType.charset,
            readiumCss = readiumCss.value,
            injectableScript = RelativeUrl("readium/navigators/web/generated/reflowable-injectable-script.js")!!,
            assetsBaseHref = assetsBaseHref,
            disableSelection = disableSelection
        )
    }

    internal val webViewServer =
        WebViewServer(
            application = application,
            container = publication.container,
            mediaTypes = publication.mediaTypes,
            errorPage = RelativeUrl("readium/navigators/web/error.xhtml")!!,
            htmlInjector = htmlInjector,
            servedAssets = listOf("readium/.*"),
            onResourceLoadFailed = { _, _ -> }
        )

    internal val webViewClient: WebViewClient =
        WebViewClient(webViewServer)

    internal val pagerState: PagerState =
        PagerState(
            currentPage = initialIndex,
            pageCount = { publication.readingOrder.size }
        )

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
                layoutDelegate
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
) : NavigationController<ReflowableWebLocation, ReflowableWebGoLocation> by navigationDelegate,
    OverflowController by navigationDelegate,
    SettingsController<ReflowableWebSettings> by layoutDelegate

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
internal class ReflowableLayoutDelegate(
    initialSettings: ReflowableWebSettings,
) : SettingsController<ReflowableWebSettings> {

    override val settings: MutableState<ReflowableWebSettings> =
        mutableStateOf(initialSettings)

    internal val overflow: State<Overflow> = derivedStateOf {
        with(settings.value) {
            SimpleOverflow(
                readingProgression = readingProgression,
                scroll = scroll,
                axis = if (scroll && !verticalText) Axis.VERTICAL else Axis.HORIZONTAL
            )
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
    override val overflow: State<Overflow>,
    initialLocation: ReflowableWebLocation,
) : NavigationController<ReflowableWebLocation, ReflowableWebGoLocation>, OverflowController {

    private val locationMutable: MutableState<ReflowableWebLocation> =
        mutableStateOf(initialLocation)

    internal fun updateLocation(location: ReflowableWebLocation) {
        val index = checkNotNull(readingOrder.indexOfHref(location.href))
        resourceStates[index].progression = location.progression
        locationMutable.value = location
    }

    override val location: State<ReflowableWebLocation> =
        locationMutable

    override suspend fun goTo(location: HyperlinkLocation) {
        goTo(ReflowableWebGoLocation(location.href))
    }

    override suspend fun goTo(location: ReflowableWebGoLocation) {
        val resourceIndex = readingOrder.indexOfHref(location.href) ?: return
        pagerState.scrollToPage(resourceIndex)
        location.progression?.let { // FIXME: goTo returns before the move has completed.
            resourceStates[resourceIndex].progression = it
            resourceStates[resourceIndex].scrollController.value?.moveToProgression(it)
        }
    }

    override suspend fun goTo(location: ReflowableWebLocation) {
        goTo(ReflowableWebGoLocation(location.href, location.progression))
    }

    // This information is not available when the WebView has not yet been composed or laid out.
    // We assume that the best UI behavior would be to have a possible forward button disabled
    // and then return false when we can't tell.
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
        when (overflow.value.scroll) {
            true -> moveBottom()
            false -> when (overflow.value.readingProgression) {
                ReadingProgression.LTR -> moveRight()
                ReadingProgression.RTL -> moveLeft()
            }
        }

    private fun WebViewScrollController.moveBackward() =
        when (overflow.value.scroll) {
            true -> moveTop()
            false -> when (overflow.value.readingProgression) {
                ReadingProgression.LTR -> moveLeft()
                ReadingProgression.RTL -> moveRight()
            }
        }

    private fun WebViewScrollController.canMoveForward(): Boolean =
        when (overflow.value.scroll) {
            true -> canMoveBottom
            false -> when (overflow.value.readingProgression) {
                ReadingProgression.LTR -> canMoveRight
                ReadingProgression.RTL -> canMoveLeft
            }
        }

    private fun WebViewScrollController.canMoveBackward(): Boolean =
        when (overflow.value.scroll) {
            true -> canMoveTop
            false -> when (overflow.value.readingProgression) {
                ReadingProgression.LTR -> canMoveLeft
                ReadingProgression.RTL -> canMoveRight
            }
        }
    private fun WebViewScrollController.moveToProgression(progression: Double) {
        moveToProgression(
            progression = progression,
            scroll = overflow.value.scroll,
            orientation = overflow.value.axis.toOrientation()
        )
    }
}

private fun Axis.toOrientation() = when (this) {
    Axis.HORIZONTAL -> Orientation.Horizontal
    Axis.VERTICAL -> Orientation.Vertical
}
