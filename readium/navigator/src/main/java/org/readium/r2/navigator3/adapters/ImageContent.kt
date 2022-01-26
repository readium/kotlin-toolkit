package org.readium.r2.navigator3.adapters

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.navigator3.TestContent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun ImageContent(
    publication: Publication,
    link: Link
) {
    val bitmap = runBlocking {
        val bytes = publication.get(link).read().getOrThrow()
        BitmapFactory.decodeByteArray(bytes)!!
    }
    val painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    val alignment: Alignment = Alignment.Center
    val contentScale: ContentScale = ContentScale.Fit
    val alpha: Float = DefaultAlpha
    val colorFilter: ColorFilter? = null
    Row {
        TestContent()
        Layout(
            {},
            Modifier.clipToBounds().paint(
                painter,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            )
        ) { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        }
        /*Image(
            bitmap = bitmapState.value.asImageBitmap(),
            contentDescription = link.title
        )*/
        TestContent()
        Layout(
            {},
            Modifier.clipToBounds().paint(
                painter,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            )
        ) { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        }
    }

}