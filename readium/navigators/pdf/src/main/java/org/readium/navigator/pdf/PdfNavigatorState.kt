package org.readium.navigator.pdf

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.common.Configurable
import org.readium.navigator.common.Navigator
import org.readium.navigator.common.Overflow
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
@Stable
public class PdfNavigatorState<S : Settings, P : Preferences<P>> internal constructor(
    override val readingOrder: ReadingOrder,
    internal val pdfNavigatorFactory: PdfNavigatorFactory<S, P, *>,
    private val settingsResolver: (P) -> S,
    private val overflowResolver: (S) -> Overflow,
    private val initialLocator: Locator,
    initialPreferences: P
) : Navigator<ReadingOrder>, Configurable<S, P> { // , Overflowable {

    override val preferences: MutableState<P> =
        mutableStateOf(initialPreferences)

    override val settings: State<S> =
        derivedStateOf { settingsResolver.invoke(preferences.value) }

    internal val locator: MutableState<Locator> =
        mutableStateOf(initialLocator)

    private val currentPage: Int get() =
        locator.value.locations.position ?: initialLocator.locations.position ?: 0
    override suspend fun goTo(item: Int) {
        throw NotImplementedError()
    }

    /*@ExperimentalReadiumApi
    public val overflow: State<Overflow> =
        derivedStateOf { overflowResolver.invoke(settings.value) }
    public val canMoveForward: Boolean
        get() = currentPage < readingOrder.items[0].pageCount - 1
    public val canMoveBackward: Boolean
        get() = currentPage > 0*/
}
