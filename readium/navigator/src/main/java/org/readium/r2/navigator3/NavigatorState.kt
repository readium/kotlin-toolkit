package org.readium.r2.navigator3

import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.navigator3.core.viewer.LazyViewerState
import org.readium.r2.navigator3.html.HtmlSpreadStateFactory
import org.readium.r2.navigator3.image.ImageSpreadStateFactory
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Try

class NavigatorState private constructor(
    val publication: Publication,
    val links: List<Link>,
) {

    private val defaultSpreadStateFactories: List<SpreadState.Factory> =
        run {
            val htmlSpreadFactory = HtmlSpreadStateFactory(publication)
            val imageSpreadFactory = ImageSpreadStateFactory(publication)
            listOf(htmlSpreadFactory, imageSpreadFactory)
        }

    private val layoutFactory: LayoutFactory =
        LayoutFactory(
            publication,
            links,
            defaultSpreadStateFactories
        )

    internal var layout: LayoutFactory.Layout =
        layoutFactory.createLayout()

    internal var viewerState: LazyViewerState =
        LazyViewerState(
            isVertical = layout.isVertical,
            isPaginated = layout.isPaginated,
            initialFirstVisibleItemIndex = 0,
            initialFirstVisibleItemScrollOffset = 0,
            initialScale = 1f
        )

    internal val spreadStates: List<SpreadState>
        get() = layout.spreadStates


    val readingProgression: ReadingProgression =
        layout.readingProgression


    suspend fun go(locator: Locator): Try<Unit, Exception> {
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        viewerState.scrollToItem(itemIndex)
        return Try.success(Unit)
    }

    suspend fun go(link: Link): Try<Unit, Exception>  {
        val locator = publication.locatorFromLink(link)
            ?: return Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}."))
        return go(locator)
    }

    suspend fun goForward(): Try<Unit, Exception> {
        val currentItemIndex = viewerState.firstVisibleItemIndex
        val currentSpread = layout.spreadStates[currentItemIndex]
        if (currentSpread.goForward()) {
            return Try.success(Unit)
        }

        if (currentItemIndex + 1 == links.size) {
            return Try.failure(Exception.InvalidState("Reached end."))
        }
        viewerState.scrollToItem(currentItemIndex + 1)
        return Try.success(Unit)
    }

    suspend fun goBackward(): Try<Unit, Exception> {
        val currentItemIndex = viewerState.firstVisibleItemIndex
        val currentSpread = layout.spreadStates[currentItemIndex]
        if (currentSpread.goBackward()) {
            return Try.success(Unit)
        }

        if (currentItemIndex == 0) {
            return Try.failure(Exception.InvalidState("Reached beginning."))
        }
        viewerState.scrollToItem(currentItemIndex - 1)
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
