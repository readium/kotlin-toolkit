package org.readium.r2.navigator3

import androidx.compose.runtime.mutableStateOf
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.navigator3.core.viewer.LazyViewerState
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Try

class NavigatorState private constructor(
    val publication: Publication,
    val links: List<Link> = publication.readingOrder,
) {
    private val overflowState = mutableStateOf(Overflow.PAGINATED)

    private val readingProgressionState = mutableStateOf(ReadingProgression.LTR)

    var overflow: Overflow
        get() = overflowState.value
        set(value) {
            overflowState.value = value
        }

    var readingProgression: ReadingProgression
        get() = readingProgressionState.value
        set(value) {
            readingProgressionState.value = value
        }

    private val isVertical get() = when (readingProgression) {
        ReadingProgression.TTB, ReadingProgression.BTT -> true
        ReadingProgression.LTR, ReadingProgression.RTL -> false
    }

    internal var viewerState: LazyViewerState =
        LazyViewerState(
            isVertical = isVertical,
            isPaginated = overflow == Overflow.PAGINATED,
            initialFirstVisibleItemIndex = 0,
            initialFirstVisibleItemScrollOffset = 0,
            initialScale = 1f
        )

    /*val currentLocator: StateFlow<Locator> =*/


    suspend fun go(locator: Locator): Try<Unit, Exception> {
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        viewerState.lazyListState.scrollToItem(itemIndex)
        return Try.success(Unit)
    }

    suspend fun go(link: Link): Try<Unit, Exception>  {
        val locator = publication.locatorFromLink(link)
            ?: return Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}."))
        return go(locator)
    }

    suspend fun goForward(): Try<Unit, Exception> {
        val currentItem = viewerState.lazyListState.firstVisibleItemIndex
        if (currentItem + 1 == links.size) {
            return Try.failure(Exception.InvalidState("Reached end."))
        }
        viewerState.lazyListState.scrollToItem(currentItem + 1)
        return Try.success(Unit)
    }

    suspend fun goBackward(): Try<Unit, Exception> {
        val currentItem = viewerState.lazyListState.firstVisibleItemIndex
        if (currentItem == 0) {
            return Try.failure(Exception.InvalidState("Reached beginning."))
        }
        viewerState.lazyListState.scrollToItem(currentItem - 1)
        return Try.success(Unit)
    }


    sealed class Exception(override val message: String) : kotlin.Exception(message) {

        class InvalidArgument(message: String): Exception(message)

        class InvalidState(message: String) : Exception(message)
    }

    companion object {

        suspend fun create(
            publication: Publication,
            links: List<Link> = publication.readingOrder,
        ): NavigatorState {
            return NavigatorState(
                publication,
                preprocessLinks(links, publication)
            )
        }

        private suspend fun preprocessLinks(links: List<Link>, publication: Publication): List<Link> =
            when {
                links.allAreHtml -> preprocessWebpub(links, publication)
                links.allAreBitmap -> preprocessComic(links, publication)
                else -> throw IllegalArgumentException()
            }

        private fun preprocessWebpub(links: List<Link>, publication: Publication): List<Link> =
            links

        private suspend fun preprocessComic(links: List<Link>, publication: Publication): List<Link> =
            links.map {
                if (it.width != null && it.height != null) {
                    it
                } else {
                    val resourceStream = ResourceInputStream(publication.get(it))
                    val size = BitmapFactory.getBitmapSize(resourceStream)
                    it.copy(width = size.width, height = size.height)
                }
            }
    }
}

enum class ReadingProgression(val value: String) {
    /** Right to left */
    RTL("rtl"),
    /** Left to right */
    LTR("ltr"),
    /** Top to bottom */
    TTB("ttb"),
    /** Bottom to top */
    BTT("btt");
}

enum class Fit(val value: String) {
    WIDTH("width"),
    HEIGHT("height"),
    CONTAIN("contain");
}

enum class Overflow(val value: String) {
    PAGINATED("paginated"),
    SCROLLED("scrolled");
}
