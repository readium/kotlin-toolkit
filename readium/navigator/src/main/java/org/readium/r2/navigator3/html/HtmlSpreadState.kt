package org.readium.r2.navigator3.html

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.navigator3.ResourceState
import org.readium.r2.navigator3.SpreadState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

internal class HtmlSpreadState(
    val publication: Publication,
    override val link: Link,
    val viewportSize: IntSize,
): SpreadState, ResourceState {

    data class ScrollData(
        val offset: Int,
        val range: Int,
        val extent: Int
    ) {
        val maxOffset: Int
            get() = range - extent - 1

        val progression: Double
            get() = (offset.toDouble() / range.toDouble())
                .coerceIn(0.0, 1.0)

        fun canScrollForward(): Boolean =
            offset < maxOffset

        fun canScrollBackward(): Boolean =
            offset > 0
    }

    val horizontalScrollData: MutableState<ScrollData?> =
        mutableStateOf(null)

    val verticalScrollData: MutableState<ScrollData?> =
        mutableStateOf(null)

    override val locations: State<Locator.Locations> = derivedStateOf {
        horizontalScrollData.value
            ?.let { Locator.Locations(progression = it.progression) }
            ?: Locator.Locations()
    }

    override val resources: List<ResourceState>
        get() = listOf(this)

    override suspend fun goForward(): Boolean =
        submitScrollCommand { scrollData ->
            if (!scrollData.canScrollForward()) {
                false
            } else {
                val newOffset = (scrollData.offset + viewportSize.width)
                    .coerceAtMost(scrollData.maxOffset)
                horizontalScrollData.value = scrollData.copy(offset = newOffset)
                true
            }
        }

    override suspend fun goBackward(): Boolean =
        submitScrollCommand { scrollData ->
            if (!scrollData.canScrollBackward()) {
                false
            } else {
                val newOffset = (scrollData.offset - viewportSize.width)
                    .coerceAtLeast(0)
                horizontalScrollData.value = scrollData.copy(offset = newOffset)
                true
            }
        }

    override suspend fun goBeginning(): Boolean =
        submitScrollCommand { scrollData ->
            horizontalScrollData.value = scrollData.copy(offset = 0)
            true
        }

    override suspend fun goEnd(): Boolean =
        submitScrollCommand { scrollData ->
            horizontalScrollData.value = scrollData.copy(offset = scrollData.maxOffset)
            true
        }

    override suspend fun go(locator: Locator): Boolean =
        submitScrollCommand { scrollData ->
            val progression = locator.locations.progression ?: 0.0
            val offset = (scrollData.range * progression).roundToInt()
            val value = offset + 1
            val newOffset = (value + - (value % scrollData.extent))
            horizontalScrollData.value = scrollData.copy(offset = newOffset)
            true
        }

    private val queueExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private fun waitForScrollData(): ScrollData? {
        var i = 0
        while (horizontalScrollData.value == null && i < 300) {
            Thread.sleep(100)
            i++
        }
        return horizontalScrollData.value
    }

    private suspend fun submitScrollCommand(command: (ScrollData) -> Boolean): Boolean =
        suspendCancellableCoroutine { continuation ->
            queueExecutor.submit {
                val scrollData = waitForScrollData()
                if (scrollData == null) {
                    continuation.resume(false)
                } else {
                    val result = command(scrollData)
                    continuation.resume(result)
                }
            }
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

        val spread = HtmlSpreadState(publication, first, viewportSize)
        return Pair(spread, links.subList(1, links.size))
    }
}
