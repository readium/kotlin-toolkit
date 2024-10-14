package org.readium.navigator.pdf

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.Configurable
import org.readium.navigator.common.NavigatorState
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
@Stable
public class PdfNavigatorState<S : Settings, P : Preferences<P>> internal constructor(
    internal val pdfNavigatorFactory: PdfNavigatorFactory<S, P, *>,
    private val settingsResolver: (P) -> S,
    initialLocator: Locator,
    initialPreferences: P
) : NavigatorState, Configurable<S, P> {

    override val preferences: MutableState<P> =
        mutableStateOf(initialPreferences)

    override val settings: State<S> =
        derivedStateOf { settingsResolver.invoke(preferences.value) }

    internal val locator: MutableState<Locator> =
        mutableStateOf(initialLocator)
}
