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
import org.readium.navigator.web.css.FontFamilyDeclaration
import org.readium.navigator.web.css.ReadiumCss
import org.readium.navigator.web.css.RsProperties
import org.readium.navigator.web.css.buildFontFamilyDeclaration
import org.readium.navigator.web.css.update
import org.readium.navigator.web.layout.ReadingOrder
import org.readium.navigator.web.location.ReflowableWebGoLocation
import org.readium.navigator.web.location.ReflowableWebLocation
import org.readium.navigator.web.preferences.ReflowableWebSettings
import org.readium.navigator.web.util.HyperlinkProcessor
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.util.WebViewServer.Companion.assetsBaseHref
import org.readium.navigator.web.util.injectHtmlReflowable
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

@ExperimentalReadiumApi
@Stable
public class ReflowableWebRenditionState internal constructor(
    application: Application,
    internal val readingOrder: ReadingOrder,
    container: Container<Resource>,
    resourceMediaTypes: Map<Url, MediaType>,
    isRestricted: Boolean,
    initialSettings: ReflowableWebSettings,
    initialLocation: ReflowableWebGoLocation,
    private val rsProperties: RsProperties = RsProperties(),
    fontFamilyDeclarations: List<FontFamilyDeclaration>,
) : RenditionState<ReflowableWebRenditionController> {

    private val navigatorState: MutableState<ReflowableWebRenditionController?> =
        mutableStateOf(null)

    override val controller: ReflowableWebRenditionController? get() =
        navigatorState.value

    internal val layoutDelegate: ReflowableLayoutDelegate =
        ReflowableLayoutDelegate(
            initialSettings
        )

    internal val hyperlinkProcessor =
        HyperlinkProcessor(container)

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
            disableSelection = isRestricted
        )
    }

    private val webViewServer =
        WebViewServer(
            application = application,
            container = container,
            mediaTypes = resourceMediaTypes,
            errorPage = RelativeUrl("readium/navigators/web/error.xhtml")!!,
            htmlInjector = htmlInjector,
            servedAssets = listOf("readium/.*"),
            onResourceLoadFailed = { _, _ -> }
        )

    internal val webViewClient: WebViewClient =
        WebViewClient(webViewServer)

    internal val pagerState: PagerState =
        PagerState(
            currentPage = readingOrder.indexOfHref(initialLocation.href) ?: 0,
            pageCount = { readingOrder.items.size }
        )

    internal suspend fun computeHyperlinkContext(originUrl: Url, outerHtml: String): LinkContext? =
        hyperlinkProcessor.computeLinkContext(originUrl, outerHtml)

    private lateinit var navigationDelegate: ReflowableNavigationDelegate

    internal fun updateLocation(location: ReflowableWebLocation) {
        initControllerIfNeeded(location)
        navigationDelegate.updateLocation(location)
    }

    private fun initControllerIfNeeded(location: ReflowableWebLocation) {
        if (controller != null) {
            return
        }

        navigationDelegate =
            ReflowableNavigationDelegate(
                readingOrder,
                pagerState,
                layoutDelegate.settings,
                location
            )
        navigatorState.value =
            ReflowableWebRenditionController(
                navigationDelegate,
                layoutDelegate
            )
    }
}

@ExperimentalReadiumApi
@Stable
public class ReflowableWebRenditionController internal constructor(
    private val navigationDelegate: ReflowableNavigationDelegate,
    layoutDelegate: ReflowableLayoutDelegate,
) : NavigationController<ReflowableWebLocation, ReflowableWebGoLocation> by navigationDelegate,
    OverflowController by navigationDelegate,
    SettingsController<ReflowableWebSettings> by layoutDelegate

@OptIn(ExperimentalReadiumApi::class)
internal class ReflowableLayoutDelegate(
    initialSettings: ReflowableWebSettings,
) : SettingsController<ReflowableWebSettings> {

    override val settings: MutableState<ReflowableWebSettings> =
        mutableStateOf(initialSettings)
}

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
internal class ReflowableNavigationDelegate(
    private val readingOrder: ReadingOrder,
    private val pagerState: PagerState,
    private val settings: State<ReflowableWebSettings>,
    initialLocation: ReflowableWebLocation,
) : NavigationController<ReflowableWebLocation, ReflowableWebGoLocation>, OverflowController {

    private val locationMutable: MutableState<ReflowableWebLocation> =
        mutableStateOf(initialLocation)

    internal fun updateLocation(location: ReflowableWebLocation) {
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
    }

    override suspend fun goTo(location: ReflowableWebLocation) {
        goTo(ReflowableWebGoLocation(location.href))
    }

    override val overflow: State<Overflow> =
        derivedStateOf {
            with(settings.value) {
                SimpleOverflow(
                    readingProgression = readingProgression,
                    scroll = scroll,
                    axis = if (scroll && !verticalText) Axis.VERTICAL else Axis.HORIZONTAL
                )
            }
        }

    override val canMoveForward: Boolean
        get() = pagerState.currentPage < readingOrder.items.size - 1

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
