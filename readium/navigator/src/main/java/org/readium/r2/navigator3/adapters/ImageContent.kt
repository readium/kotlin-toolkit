package org.readium.r2.navigator3.adapters

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.*
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImageContent(
    modifier: Modifier,
    publication: Publication,
    link: Link,
    contentScale: ContentScale
) {
    val bitmap = runBlocking {
        val bytes = publication.get(link).read().getOrThrow()
        BitmapFactory.decodeByteArray(bytes)!!
    }
    val painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    val alignment: Alignment = Alignment.Center
    /*val alpha: Float = DefaultAlpha
    val colorFilter: ColorFilter? = null*/

    Image(painter, null, modifier, alignment, contentScale)

    /*Layout(
        {},
        Modifier
            .paint(
                painter,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            )
    ) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }*/
}

/*val Fit = object : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
        computeFillMinDimension(srcSize, dstSize).let {
            ScaleFactor(it, it)
        }
}

private fun computeFillMaxDimension(srcSize: Size, dstSize: Size): Float {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return max(widthScale, heightScale)
}

private fun computeFillMinDimension(srcSize: Size, dstSize: Size): Float {
    Timber.d("srcSize $srcSize dstSize $dstSize")
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return min(widthScale, heightScale)
}

private fun computeFillWidth(srcSize: Size, dstSize: Size): Float =
    dstSize.width / srcSize.width

private fun computeFillHeight(srcSize: Size, dstSize: Size): Float =
    dstSize.height / srcSize.height
*/