package org.readium.r2.navigator3

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
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

    internal lateinit var navigatorScope: NavigatorScope
        private set

    internal lateinit var layoutCoroutineScope: CoroutineScope
        private set

    private var layoutCompleted: Boolean = false


    internal fun layout(width: Int, height: Int) {
        val viewport = IntSize(width, height)

        val isFirstLayout = !::currentLayout.isInitialized

        if (!isFirstLayout && currentLayout.viewport == viewport) {
            return
        }

        layoutCompleted = false

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

        navigatorScope =
            NavigatorScope(viewerState, currentLayout)

        layoutCoroutineScope =
            MainScope()

        layoutCoroutineScope.launch {
            go(_currentLocator.value)
                .onSuccess {
                    Timber.v("Locator ${currentLocator.value} successfully restored.")
                }
                .onFailure { exception ->
                    Timber.e(exception)
                }
            layoutCompleted = true
        }

        layoutCoroutineScope.launch {
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
        }
    }

    private val _currentLocator: MutableStateFlow<Locator> =
        MutableStateFlow(
            initialLocator
                ?: publication.locatorFromLink(publication.readingOrder.first())!!
        )

    val readingProgression: ReadingProgression
        get() = currentLayout.readingProgression

    val currentLocator: StateFlow<Locator>
        get() = _currentLocator

    suspend fun go(locator: Locator): Try<Unit, Exception> {
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        viewerState.scrollToItem(itemIndex)
        currentLayout.spreadStates[itemIndex].go(locator)
        return Try.success(Unit)
    }

    suspend fun go(link: Link): Try<Unit, Exception>  {
        val locator = publication.locatorFromLink(link)
            ?: return Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}."))
        return go(locator)
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
