package org.readium.navigator.web

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.Configurable
import org.readium.navigator.common.Navigator
import org.readium.navigator.common.NavigatorState
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.Overflowable
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
public class FixedWebState internal constructor(
    publicationMetadata: Metadata,
    public val readingOrder: FixedWebReadingOrder,
    initialPreferences: FixedWebPreferences,
    defaults: FixedWebDefaults,
    initialLocation: Int,
    internal val webViewServer: WebViewServer,
    internal val preloadedData: FixedWebPreloadedData
) : NavigatorState<FixedWebNavigator> {
    init {
        require(initialLocation < readingOrder.items.size)
    }

    private val settingsResolver: FixedWebSettingsResolver =
        FixedWebSettingsResolver(publicationMetadata, defaults)

    internal val webViewClient: WebViewClient =
        WebViewClient(webViewServer)

    private val core: FixedWebCore =
        FixedWebCore(
            readingOrder = readingOrder,
            settingsResolver = settingsResolver,
            initialPreferences = initialPreferences,
            initialLocation = initialLocation
        )

    private val stateMutable: MutableState<NavigatorState.InitializationState<FixedWebNavigator>> =
        mutableStateOf(NavigatorState.InitializationState.Pending())

    public override val initState: State<NavigatorState.InitializationState<FixedWebNavigator>> =
        stateMutable

    override val navigator: FixedWebNavigator? get() =
        initState.value.navigator

    public val preferences: MutableState<FixedWebPreferences> =
        core.preferences

    public val settings: State<FixedWebSettings> =
        core.settings

    internal val layout: State<Layout> =
        core.layout

    internal val fit: State<Fit> =
        core.fit

    internal val pagerState: PagerState =
        core.pagerState

    private fun initNavigatorIfNeeded(location: FixedWebLocation): FixedWebNavigator {
        when (val initStateNow = stateMutable.value) {
            is NavigatorState.InitializationState.Initialized<*> -> {
                return initStateNow.navigator as FixedWebNavigator
            }
            is NavigatorState.InitializationState.Pending -> {
                val navigator = FixedWebNavigator(core, location)
                stateMutable.value = NavigatorState.InitializationState.Initialized(navigator)
                return navigator
            }
        }
    }

    internal fun updateLocation(location: FixedWebLocation) {
        val navigator = initNavigatorIfNeeded(location)
        navigator.updateLocation(location)
    }
}

@ExperimentalReadiumApi
@Stable
public class FixedWebNavigator internal constructor(
    private val core: FixedWebCore,
    initialLocation: FixedWebLocation
) : Navigator<FixedWebReadingOrder, FixedWebLocation, FixedWebGoLocation>,
    Configurable<FixedWebSettings, FixedWebPreferences>,
    Overflowable by core {

    private val locationMutable: MutableState<FixedWebLocation> =
        mutableStateOf(initialLocation)

    override val readingOrder: FixedWebReadingOrder =
        core.readingOrder

    override val location: State<FixedWebLocation> =
        locationMutable

    override suspend fun goTo(targetLocation: FixedWebGoLocation) {
        core.goTo(targetLocation)
    }

    override suspend fun goTo(location: FixedWebLocation) {
        core.goTo(location)
    }

    override val preferences: MutableState<FixedWebPreferences> =
        core.preferences

    override val settings: State<FixedWebSettings> =
        core.settings

    internal fun updateLocation(location: FixedWebLocation) {
        locationMutable.value = location
    }

    override suspend fun goTo(link: Link) {
        val href = link.url().removeFragment()
        val location = HrefLocation(href)
        goTo(location)
    }
}

@OptIn(ExperimentalReadiumApi::class)
internal class FixedWebCore(
    val readingOrder: FixedWebReadingOrder,
    private val settingsResolver: FixedWebSettingsResolver,
    initialPreferences: FixedWebPreferences,
    initialLocation: Int
) : Overflowable {
    private val layoutResolver =
        LayoutResolver(readingOrder)

    val preferences: MutableState<FixedWebPreferences> =
        mutableStateOf(initialPreferences)

    val settings: State<FixedWebSettings> =
        derivedStateOf { settingsResolver.settings(preferences.value) }

    val layout: State<Layout> =
        derivedStateOf {
            val spreads = layoutResolver.layout(settings.value)
            Layout(settings.value.readingProgression, spreads)
        }

    val fit: State<Fit> =
        derivedStateOf { settings.value.fit }

    val pagerState: PagerState =
        PagerState(
            currentPage = layout.value.spreadIndexForPage(initialLocation),
            pageCount = { layout.value.spreads.size }
        )

    suspend fun goTo(targetLocation: FixedWebGoLocation) {
        when (targetLocation) {
            is HrefLocation -> {
                val pageIndex = checkNotNull(readingOrder.indexOfHref(targetLocation.href))
                pagerState.scrollToPage(layout.value.spreadIndexForPage(pageIndex))
            }
        }
    }

    suspend fun goTo(location: FixedWebLocation) {
        return goTo(HrefLocation(location.href))
    }

    @ExperimentalReadiumApi
    @OptIn(InternalReadiumApi::class)
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

internal data class FixedWebPreloadedData(
    val prepaginatedSingleContent: String,
    val prepaginatedDoubleContent: String
)
