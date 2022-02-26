package org.readium.r2.navigator3.html

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.readium.r2.navigator3.SpreadState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

internal typealias JavaScriptCommand = (JavaScriptExecutor).() -> Unit

internal class HtmlSpreadState(
    val publication: Publication,
    val link: Link,
    val pendingCommands: MutableState<List<JavaScriptCommand>>
): SpreadState {

    var canScrollRight: Boolean = false
    var canScrollLeft: Boolean = false

    override suspend fun goForward(): Boolean {
        if (!canScrollRight) {
            return false
        }

        val scrollRight: (JavaScriptExecutor).() -> Unit = {
            scrollRight()
        }

        pendingCommands.value = pendingCommands.value + scrollRight

        return true
    }

    override suspend fun goBackward(): Boolean {
        if (!canScrollLeft) {
            return false
        }

        val scrollLeft: (JavaScriptExecutor).() -> Unit = {
            scrollLeft()
        }

        pendingCommands.value = pendingCommands.value + scrollLeft

        return true
    }
}

internal class HtmlSpreadStateFactory(
    private val publication: Publication
): SpreadState.Factory {

    override fun createSpread(links: List<Link>): Pair<SpreadState, List<Link>>? {
        require(links.isNotEmpty())

        val first = links.first()
        if (!first.mediaType.isHtml) {
            return null
        }

        val stateOfPendingCommands: MutableState<List<JavaScriptCommand>> =
            mutableStateOf(emptyList())

        val spread = HtmlSpreadState(publication, first, stateOfPendingCommands)
        return Pair(spread, links.subList(1, links.size))
    }
}