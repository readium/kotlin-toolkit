package org.readium.navigator.pdf

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.Configurable
import org.readium.navigator.common.Navigator
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
@Stable
public class PdfNavigatorState<S : Settings, P : Preferences<P>> internal constructor(
    override val readingOrder: PdfReadingOrder,
    internal val pdfNavigatorFactory: PdfNavigatorFactory<S, P, *>,
    private val settingsResolver: (P) -> S,
    initialLocator: Locator,
    initialPreferences: P
) : Navigator<PdfReadingOrder, PdfLocation, PdfGoLocation>, Configurable<S, P> {

    override val preferences: MutableState<P> =
        mutableStateOf(initialPreferences)

    override val settings: State<S> =
        derivedStateOf { settingsResolver.invoke(preferences.value) }

    internal val locator: MutableState<Locator> =
        mutableStateOf(initialLocator)

    internal val pendingLocator: MutableState<Locator?> =
        mutableStateOf(null)

    override val location: State<PdfLocation> =
        derivedStateOf {
            PdfLocation(href = locator.value.href, page = locator.value.locations.position!!)
        }

    override suspend fun goTo(link: Link) {
        goTo(PageLocation(link.url(), 0))
    }

    override suspend fun goTo(location: PdfLocation) {
        pendingLocator.value = locator.value.copyWithLocations(position = location.page)
    }

    override suspend fun goTo(goLocation: PdfGoLocation) {
        pendingLocator.value = when (goLocation) {
            is PositionLocation -> locator.value.copyWithLocations(position = goLocation.position)
            is PageLocation -> locator.value.copyWithLocations(position = goLocation.page)
        }
    }
}
