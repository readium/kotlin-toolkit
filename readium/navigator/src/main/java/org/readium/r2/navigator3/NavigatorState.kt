package org.readium.r2.navigator3

import androidx.compose.runtime.mutableStateOf
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

class NavigatorState private constructor(
    val publication: Publication,
    readingProgression: ReadingProgression,
    overflow: Overflow,
    val baseUrl: String,
    val links: List<Link> = publication.readingOrder,
) {
    private val overflowState = mutableStateOf(overflow)

    private val readingProgressionState = mutableStateOf(readingProgression)

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


    companion object {

        suspend fun create(
            publication: Publication,
            readingProgression: ReadingProgression,
            overflow: Overflow,
            baseUrl: String,
            links: List<Link> = publication.readingOrder,
        ): NavigatorState {
            return NavigatorState(
                publication,
                readingProgression,
                overflow,
                baseUrl,
                links.map { preprocessLink(it, baseUrl, publication) }            )
        }

        private suspend fun preprocessLink(link: Link, baseUrl: String, publication: Publication): Link =
            when  {
                link.mediaType.isHtml -> link.withBaseUrl(baseUrl)
                link.mediaType.isBitmap -> preprocessBitmapLink(link, publication)
                else -> link
            }

        private suspend fun preprocessBitmapLink(link: Link, publication: Publication): Link {
            if (link.width != null && link.height != null) {
                return link
            }

            val resourceStream = ResourceInputStream(publication.get(link))
            val size = BitmapFactory.getBitmapSize(resourceStream)
            return link.copy(width = size.width, height = size.height)
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
