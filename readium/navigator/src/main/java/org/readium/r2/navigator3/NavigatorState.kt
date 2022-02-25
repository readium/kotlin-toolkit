package org.readium.r2.navigator3

import androidx.compose.runtime.mutableStateOf
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.allAreBitmap
import org.readium.r2.shared.publication.allAreHtml

class NavigatorState private constructor(
    val publication: Publication,
    val links: List<Link> = publication.readingOrder,
) {
    private val overflowState = mutableStateOf(Overflow.SCROLLED)

    private val readingProgressionState = mutableStateOf(ReadingProgression.TTB)

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

    /*val currentLocator: StateFlow<Locator> =


    fun go(locator: Locator): Try<Unit, Exception> {

    }

    fun go(link: Link): Try<Unit, Exception> =
        go(link.toLocator())*/

    sealed class Exception : kotlin.Exception() {
        abstract override val message: String
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
