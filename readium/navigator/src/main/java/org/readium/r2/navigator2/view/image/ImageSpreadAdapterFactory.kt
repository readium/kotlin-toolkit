package org.readium.r2.navigator2.view.image

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.View
import org.readium.r2.navigator.R
import org.readium.r2.navigator2.view.SpreadAdapter
import org.readium.r2.navigator2.view.SpreadAdapterFactory
import org.readium.r2.navigator2.view.layout.EffectiveReadingProgression
import org.readium.r2.navigator2.view.layout.LayoutHelpers
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.presentation.page

internal class ImageSpreadAdapterFactory(
    private val publication: Publication,
    private val readingProgression: EffectiveReadingProgression,
    private val resolveSpreadHint: (Link) -> Boolean,
    private val errorBitmap: (Size) -> Bitmap,
    private val emptyBitmap: (Size) -> Bitmap
): SpreadAdapterFactory {

    override fun createSpread(
        links: List<Link>,
    ): Pair<SpreadAdapter, List<Link>>? {
        require(links.isNotEmpty())

        val first = links.first()
        if (!first.mediaType.isBitmap) {
            return null
        }
        if (links.size == 1 || !readingProgression.isHorizontal) {
            val spread = createSingleSpread(first)
            return Pair(spread, links.subList(1, links.size))
        }

        val second = links[1]
        if (!second.mediaType.isBitmap) {
            val spread = createSingleSpread(first)
            return Pair(spread, links.subList(1, links.size))
        }

        val doubleSpread = resolveSpreadHint(first) && resolveSpreadHint(second)
        val canBeSpread = LayoutHelpers.arePagesCompatible(first.properties.page, second.properties.page, readingProgression)

        if (!doubleSpread || !canBeSpread) {
            val spread = createSingleSpread(first)
            return Pair(spread, links.subList(1, links.size))
        }

        val spread = createDoubleSpread(first, second)
        return Pair(spread, links.subList(2, links.size))
    }

    private fun createSingleSpread(link: Link): ImageSpreadAdapter =
        ImageSpreadAdapter(
            links = listOf(link),
            publication = publication,
            readingProgression = readingProgression,
            errorBitmap = errorBitmap,
            emptyBitmap = emptyBitmap
        )

    private fun createDoubleSpread(first: Link, second: Link): ImageSpreadAdapter =
        ImageSpreadAdapter(
            links = listOf(first, second),
            publication = publication,
            readingProgression = readingProgression,
            errorBitmap = errorBitmap,
            emptyBitmap = emptyBitmap
        )

    override fun createView(context: Context, viewportSize: Size): View {
        return View.inflate(context, R.layout.navigator2_item_photo_view, null)
    }
}