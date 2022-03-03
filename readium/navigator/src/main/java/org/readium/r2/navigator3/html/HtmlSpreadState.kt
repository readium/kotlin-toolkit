package org.readium.r2.navigator3.html

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
import org.readium.r2.navigator3.SpreadState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

internal class HtmlSpreadState(
    val publication: Publication,
    val link: Link,
    val viewportSize: IntSize,
    val offset: MutableState<Int>,
): SpreadState {

    var canScrollRight: Boolean = false
    var canScrollLeft: Boolean = false
    var horizontalRange: MutableState<Int?> = mutableStateOf(null)
    var verticalRange: Int? = null
    val pendingProgression: MutableState<Double?> = mutableStateOf(null)

    override val locations: State<Locator.Locations> = derivedStateOf {
        val progression = horizontalRange.value?.let { range ->
            (offset.value / range.toDouble()).coerceIn(0.0, 1.0)
        } ?: 0.0
        Locator.Locations(progression = progression)
    }

    override suspend fun goForward(): Boolean {
        if (!canScrollRight) {
            return false
        }

        pendingProgression.value = null
        offset.value += viewportSize.width

        return true
    }

    override suspend fun goBackward(): Boolean {
        if (!canScrollLeft) {
            return false
        }

        pendingProgression.value = null
        offset.value -= viewportSize.width
        return true
    }

    override suspend fun goBeginning() {
        pendingProgression.value = null
        offset.value = 0
    }

    override suspend fun goEnd() {
        pendingProgression.value = 1.0
    }

    override suspend fun go(locator: Locator) {
        val progression = locator.locations.progression ?: return
        pendingProgression.value = progression
    }
}

internal class HtmlSpreadStateFactory(
    private val publication: Publication,
    private val viewportSize: IntSize
): SpreadState.Factory {

    override fun createSpread(links: List<Link>): Pair<SpreadState, List<Link>>? {
        require(links.isNotEmpty())

        val first = links.first()
        if (!first.mediaType.isHtml) {
            return null
        }

        val spread = HtmlSpreadState(publication, first, viewportSize, mutableStateOf(0))
        return Pair(spread, links.subList(1, links.size))
    }
}