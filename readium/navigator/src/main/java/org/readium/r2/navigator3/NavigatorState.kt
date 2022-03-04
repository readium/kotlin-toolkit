package org.readium.r2.navigator3

import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.navigator3.core.viewer.LazyViewerState
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Try
import timber.log.Timber

class NavigatorState private constructor(
    val publication: Publication,
    val links: List<Link>,
    private val layoutFactory: LayoutFactory,
    initialLocator: Locator?
) {
    internal lateinit var currentLayout: LayoutFactory.Layout
        private set

    internal lateinit var viewerState: LazyViewerState
        private set

    internal lateinit var layoutCoroutineScope: CoroutineScope
        private set

    internal fun layout(width: Int, height: Int) {
        val viewport = IntSize(width, height)

        val isFirstLayout = !::currentLayout.isInitialized

        if (!isFirstLayout && currentLayout.viewport == viewport) {
            return
        }

        Timber.v("New layout ${viewport.width} ${viewport.height}.")

        if (::layoutCoroutineScope.isInitialized) {
            layoutCoroutineScope.cancel()
        }

        currentLayout =
            layoutFactory.layout(viewport)

        viewerState =
            LazyViewerState(
                isVertical = currentLayout.isVertical,
                isPaginated = currentLayout.isPaginated,
                initialFirstVisibleItemIndex = 0,
                initialFirstVisibleItemScrollOffset = 0,
                initialScale = 1f
            )

        layoutCoroutineScope =
            MainScope()

        layoutCoroutineScope.launch {
            go(nextLocator ?: currentLocator.value)
                .onSuccess {
                    Timber.v("Locator ${currentLocator.value} successfully restored.")
                }
                .onFailure { exception ->
                    Timber.e(exception)
                }
        }

        /*layoutCoroutineScope.launch {
            snapshotFlow {
                val currentIndex = viewerState.firstVisibleItemIndex
                val link = links[currentIndex]
                val spread = currentLayout.spreadStates[currentIndex]
                val basicLocator = requireNotNull(publication.locatorFromLink(link))
                basicLocator.copy(locations = spread.locations.value)
            }.collect { locator ->
                if (layoutCompleted) {
                    _currentLocator.value = locator
                }
            }
        }*/
    }

    private val currentLocatorMutable: MutableStateFlow<Locator> =
        MutableStateFlow(Locator(href="#", type=""))

    private  var nextLocator: Locator? =
        initialLocator ?: publication.locatorFromLink(publication.readingOrder.first())!!

    val readingProgression: ReadingProgression
        get() = currentLayout.readingProgression

    val currentLocator: StateFlow<Locator>
        get() = currentLocatorMutable

    suspend fun go(locator: Locator): Try<Unit, Exception> {
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        nextLocator = locator
        viewerState.scrollToItem(itemIndex)
        currentLayout.spreadStates[itemIndex].go(locator)
        updateLocator()
        nextLocator = null
        return Try.success(Unit)
    }

    suspend fun go(link: Link): Try<Unit, Exception>  {
        val locator = publication.locatorFromLink(link)
            ?: return Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}."))
        return go(locator)
    }

    suspend fun goForward(): Try<Unit, Exception> {
        val currentItemIndex = viewerState.firstVisibleItemIndex
        val currentSpread = currentLayout.spreadStates[currentItemIndex]
        if (currentSpread.goForward()) {
            updateLocator()
            return Try.success(Unit)
        }

        if (currentItemIndex + 1 == viewerState.totalItemsCount) {
            return Try.failure(Exception.InvalidState("Reached end."))
        }
        viewerState.scrollToItem(currentItemIndex + 1)
        currentLayout.spreadStates[currentItemIndex + 1].goBeginning()
        updateLocator()
        return Try.success(Unit)
    }

    suspend fun goBackward(): Try<Unit, Exception> {
        val currentItemIndex = viewerState.firstVisibleItemIndex
        val currentSpread = currentLayout.spreadStates[currentItemIndex]
        if (currentSpread.goBackward()) {
            updateLocator()
            return Try.success(Unit)
        }

        if (currentItemIndex == 0) {
            return Try.failure(Exception.InvalidState("Reached beginning."))
        }
        viewerState.scrollToItem(currentItemIndex - 1)
        currentLayout.spreadStates[currentItemIndex - 1].goEnd()
        updateLocator()
        return Try.success(Unit)
    }

    private fun updateLocator() {
        val currentIndex = viewerState.firstVisibleItemIndex
        val link = links[currentIndex]
        val spread = currentLayout.spreadStates[currentIndex]
        val basicLocator = requireNotNull(publication.locatorFromLink(link))
        val locator = basicLocator.copy(locations = spread.locations.value)
        currentLocatorMutable.value = locator
    }

    sealed class Exception(override val message: String) : kotlin.Exception(message) {

        class InvalidArgument(message: String): Exception(message)

        class InvalidState(message: String) : Exception(message)
    }

    companion object {

        suspend fun create(
            publication: Publication,
            initialLocator: Locator? = null,
            links: List<Link> = publication.readingOrder,
        ): NavigatorState {
            val preprocessedLinks = preprocessLinks(links, publication)
            val layoutFactory = LayoutFactory(publication, preprocessedLinks)
            return NavigatorState(publication, preprocessedLinks, layoutFactory, initialLocator)
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
