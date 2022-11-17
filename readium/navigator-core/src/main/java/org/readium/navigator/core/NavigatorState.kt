package org.readium.navigator.core

import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Try
import timber.log.Timber

class NavigatorState private constructor(
    val publication: Publication,
    val links: List<Link>,
    private val layoutFactory: LayoutFactory,
    private val initialLocator: Locator?
) {
    val readingProgression: ReadingProgression
        get() = currentLayout.readingProgression

    val currentLocator: StateFlow<Locator>
        get() = currentLocatorMutable

    internal lateinit var currentLayout: LayoutFactory.Layout
        private set

    internal lateinit var viewerState: LazyViewerState
        private set

    internal lateinit var layoutCoroutineScope: CoroutineScope
        private set

    private val mutex: NavigatorMutex =
        NavigatorMutex()

    private val currentLocatorMutable: MutableStateFlow<Locator> =
        MutableStateFlow(Locator(href = "#", type = ""))

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

        refreshLayout(viewport)
    }

    private fun refreshLayout(viewport: IntSize) {
        Timber.d("Computing a new layout")
        layoutCoroutineScope =
            MainScope()

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

        layoutCoroutineScope.launch {
            try {
                Timber.v("restore coroutine")
                // Make ongoing go command restart if any.
                mutex.refreshLayout {
                    Timber.d("layout block content")
                }

                // As relative go commands, previous location restoration should not override ongoing go.
                mutex.restoreLocation {
                    restoreLocationAfterLayout()
                }
            } catch (e: CancellationException) {
                Timber.e(e, "restore coroutine cancelled")
                throw e
            }

        }
    }

    private suspend fun restoreLocationAfterLayout() {
        Timber.v("restoreLocationAfterLayout")
        val locator = currentLocator.value
            .takeUnless { it == Locator(href = "#", type = "") }
            ?: initialLocator
            ?: publication.locatorFromLink(links.first())!!
        goWithoutLock(locator)
            .onSuccess {
                Timber.v("Locator ${currentLocator.value} successfully restored.")
            }
            .onFailure { exception ->
                Timber.e(exception)
            }
    }

    /**
     * Go forward through the publication.
     *
     * The command execution will be canceled on new layouts.
     */
    suspend fun goForward(): Try<Unit, Exception> =
        mutex.relativeGo {
            goForwardWithoutLock()
        }

    private suspend fun goForwardWithoutLock(): Try<Unit, Exception> {
        Timber.v("goForwardWithoutLock")
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

    /**
     * Go forward through the publication.
     *
     * The command execution will be canceled on new layouts.
     */
    suspend fun goBackward(): Try<Unit, Exception> =
        mutex.relativeGo {
            goBackwardWithoutLock()
        }

    private suspend fun goBackwardWithoutLock(): Try<Unit, Exception> {
        Timber.v("goBackwardWithoutLock")
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

    /**
     * Go to the beginning of the given link. The command will survive new layouts.
     */
    suspend fun go(link: Link): Try<Unit, Exception>  =
        mutex.absoluteGo {
            publication.locatorFromLink(link)
                ?.let { goWithoutLock(it) }
                ?: Try.failure(Exception.InvalidArgument("Resource not found at ${link.href}."))
    }

    /**
     * Go to the given locator. The command will survive new layouts.
     */
    suspend fun go(locator: Locator): Try<Unit, Exception> =
        mutex.absoluteGo {
            goWithoutLock(locator)
        }

    private suspend fun goWithoutLock(locator: Locator): Try<Unit, Exception> {
        val itemIndex = currentLayout.spreadStates.indexOfHref(locator.href)
            ?: return Try.failure(Exception.InvalidArgument("Invalid href ${locator.href}."))
        viewerState.scrollToItem(itemIndex)
        currentLayout.spreadStates[itemIndex].go(locator)
        updateLocator()
        return Try.success(Unit)
    }

    private fun List<SpreadState>.indexOfHref(href: String): Int? =
        indexOfFirst { spread -> spread.containsHref(href) }

    private fun SpreadState.containsHref(href: String): Boolean =
        href in resources.map { resource -> resource.link.href }

    private fun updateLocator() {
        val currentIndex = viewerState.firstVisibleItemIndex
        val spread = currentLayout.spreadStates[currentIndex]
        val link = spread.resources.first().link
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
            spreadStateFactories: List<SpreadState.Factory>
        ): NavigatorState {
            val preprocessedLinks = preprocessLinks(links, publication)
            val layoutFactory = LayoutFactory(publication, preprocessedLinks, spreadStateFactories)
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
