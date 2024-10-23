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
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
@Stable
public class PdfNavigatorState<S : Settings, P : Preferences<P>> internal constructor(
    override val readingOrder: PdfReadingOrder,
    internal val pdfNavigatorFactory: PdfNavigatorFactory<S, P, *>,
    private val settingsResolver: (P) -> S,
    initialLocator: Locator,
    initialPreferences: P
) : Navigator<PdfReadingOrder, PdfNavigatorLocation>, Configurable<S, P> {

    override val preferences: MutableState<P> =
        mutableStateOf(initialPreferences)

    override val settings: State<S> =
        derivedStateOf { settingsResolver.invoke(preferences.value) }

    internal val locator: MutableState<Locator> =
        mutableStateOf(initialLocator)

    internal val pendingLocator: MutableState<Locator?> =
        mutableStateOf(null)

    override val location: State<PdfNavigatorLocation> =
        derivedStateOf { PdfNavigatorLocation(locator.value) }

    override suspend fun goTo(location: PdfNavigatorLocation) {
        pendingLocator.value = locator.value
    }

    override suspend fun goTo(readingOrderItem: Int) {
        pendingLocator.value = locator.value.copy(locations = Locator.Locations(position = 0))
    }
}
