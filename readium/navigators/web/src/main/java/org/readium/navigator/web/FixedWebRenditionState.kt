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
import org.readium.navigator.web.layout.FixedWebReadingOrder
import org.readium.navigator.web.layout.Layout
import org.readium.navigator.web.layout.LayoutResolver
import org.readium.navigator.web.location.FixedWebGoLocation
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.location.HrefLocation
import org.readium.navigator.web.preferences.FixedWebDefaults
import org.readium.navigator.web.preferences.FixedWebPreferences
import org.readium.navigator.web.preferences.FixedWebSettings
import org.readium.navigator.web.preferences.FixedWebSettingsResolver
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
@Stable
public class FixedWebRenditionState internal constructor(
    public val readingOrder: FixedWebReadingOrder,
    publicationMetadata: Metadata,
    defaults: FixedWebDefaults,
    initialPreferences: FixedWebPreferences,
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
            publicationMetadata,
            defaults,
            initialPreferences
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
) : Navigator<FixedWebReadingOrder, FixedWebLocation, FixedWebGoLocation> by navigationDelegate,
    Overflowable by navigationDelegate,
    Configurable<FixedWebSettings, FixedWebPreferences> by layoutDelegate

internal data class FixedWebPreloadedData(
    val fixedSingleContent: String,
    val fixedDoubleContent: String
)

@OptIn(ExperimentalReadiumApi::class)
internal class LayoutDelegate(
    readingOrder: FixedWebReadingOrder,
    publicationMetadata: Metadata,
    defaults: FixedWebDefaults,
    initialPreferences: FixedWebPreferences
) : Configurable<FixedWebSettings, FixedWebPreferences> {

    private val settingsResolver: FixedWebSettingsResolver =
        FixedWebSettingsResolver(publicationMetadata, defaults)

    private val layoutResolver =
        LayoutResolver(readingOrder)

    override val preferences: MutableState<FixedWebPreferences> =
        mutableStateOf(initialPreferences)

    override val settings: State<FixedWebSettings> =
        derivedStateOf { settingsResolver.settings(preferences.value) }

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
    override val readingOrder: FixedWebReadingOrder,
    private val pagerState: PagerState,
    private val layout: State<Layout>,
    private val settings: State<FixedWebSettings>,
    initialLocation: FixedWebLocation
) : Navigator<FixedWebReadingOrder, FixedWebLocation, FixedWebGoLocation>, Overflowable {

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

    override suspend fun goTo(targetLocation: FixedWebGoLocation) {
        when (targetLocation) {
            is HrefLocation -> {
                val pageIndex = checkNotNull(readingOrder.indexOfHref(targetLocation.href))
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
