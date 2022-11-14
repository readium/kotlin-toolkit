package org.readium.navigator.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
internal fun Image(
    bitmap: ImageBitmap,
) {
    Layout(
        {},
        Modifier
            .paint(
                remember(bitmap) { BitmapPainter(bitmap) },
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                alpha = DefaultAlpha,
                colorFilter = null
            )
    ) { _, constraints ->
        Timber.d("Image child size ${constraints.minWidth} ${constraints.minHeight}")
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

/**
 * Paint the content using [painter].
 *
 * @param alignment specifies alignment of the [painter] relative to content
 * @param alpha opacity of [painter]
 * @param colorFilter optional [ColorFilter] to apply to [painter]
 */
private fun Modifier.paint(
    painter: BitmapPainter,
    contentScale: ContentScale,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = this.then(
    PainterModifier(
        painter = painter,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        colorFilter = colorFilter,
        inspectorInfo = debugInspectorInfo {
            name = "paint"
            properties["painter"] = painter
            properties["contentScale"] = contentScale
            properties["alignment"] = alignment
            properties["alpha"] = alpha
            properties["colorFilter"] = colorFilter
        }
    )
)

/**
 * [DrawModifier] used to draw the provided [BitmapPainter] followed by the contents
 * of the component itself
 */
private class PainterModifier(
    val painter: BitmapPainter,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val alpha: Float = DefaultAlpha,
    val colorFilter: ColorFilter? = null,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, DrawModifier, InspectorValueInfo(inspectorInfo) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        Timber.d("constraintsToModify ${constraints.minWidth} ${constraints.minHeight} ${constraints.maxWidth} ${constraints.maxHeight}")
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        return constraints.copy(minWidth = constraints.maxWidth, minHeight = constraints.maxHeight)
    }


    override fun ContentDrawScope.draw() {
        val srcSize = painter.intrinsicSize

        // Compute the offset to translate the content based on the given alignment
        // and size to draw based on the ContentScale parameter
        val scaledSize = if (size.width != 0f && size.height != 0f) {
            srcSize * contentScale.computeScaleFactor(srcSize, size)
        } else {
            Size.Zero
        }

        val alignedPosition = alignment.align(
            IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection
        )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // Only translate the current drawing position while delegating the Painter to draw
        // with scaled size.
        // Individual Painter implementations should be responsible for scaling their drawing
        // content accordingly to fit within the drawing area.
        translate(dx, dy) {
            with(painter) {
                draw(size = scaledSize, alpha = alpha, colorFilter = colorFilter)
            }
        }
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + contentScale.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + (colorFilter?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? PainterModifier ?: return false
        return painter == otherModifier.painter &&
                alignment == otherModifier.alignment &&
                contentScale == otherModifier.contentScale &&
                alpha == otherModifier.alpha &&
                colorFilter == otherModifier.colorFilter
    }

    override fun toString(): String =
        "PainterModifier(" +
                "painter=$painter, " +
                "alignment=$alignment, " +
                "alpha=$alpha, " +
                "colorFilter=$colorFilter)"
}
