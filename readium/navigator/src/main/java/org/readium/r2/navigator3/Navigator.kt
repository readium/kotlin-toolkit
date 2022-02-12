package org.readium.r2.navigator3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator3.adapters.ImageContent
import org.readium.r2.navigator3.adapters.WebContent
import org.readium.r2.navigator3.viewer.LazyPager
import org.readium.r2.navigator3.viewer.LazyScroller
import org.readium.r2.navigator3.viewer.rememberLazyPagerState
import org.readium.r2.navigator3.viewer.rememberLazyScrollerState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun Navigator(
    modifier: Modifier = Modifier,
    publication: Publication,
    state: NavigatorState,
    baseUrl: String,
    links: List<Link> = publication.readingOrder,
) {
    val isVertical = when (state.readingProgression) {
        ReadingProgression.TTB, ReadingProgression.BTT -> true
        ReadingProgression.LTR, ReadingProgression.RTL -> false
    }

    val reverseDirection = when (state.readingProgression) {
        ReadingProgression.TTB, ReadingProgression.LTR -> false
        ReadingProgression.BTT, ReadingProgression.RTL -> true
    }

    val horizontalArrangement = if (!reverseDirection) Arrangement.Start else Arrangement.End

    val horizontalAlignment = Alignment.CenterHorizontally

    val verticalArrangement = if (!reverseDirection) Arrangement.Top else Arrangement.Bottom

    val verticalAlignment = Alignment.CenterVertically

    if (state.overflow == Overflow.SCROLLED) {

        val lazyScrollerState = rememberLazyScrollerState(isVertical)

        LazyScroller(
            modifier = modifier,
            isVertical = isVertical,
            state = lazyScrollerState,
            contentPadding = PaddingValues(0.dp),
            reverseDirection = reverseDirection,
            horizontalArrangement = horizontalArrangement,
            horizontalAlignment =  horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            count = links.size
        ) { index ->
            Resource(publication, links[index], baseUrl, lazyScrollerState.scaleState.value)
        }

    } else {

        val lazyPagerState = rememberLazyPagerState()

        LazyPager(
            modifier = modifier,
            isVertical = isVertical,
            state = lazyPagerState,
            contentPadding = PaddingValues(0.dp),
            reverseDirection = reverseDirection,
            horizontalArrangement = horizontalArrangement,
            horizontalAlignment =  horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            count = links.size
        ) { index, scale ->
            Resource(publication, links[index], baseUrl, scale)
        }
    }
}

@Composable
private fun Resource(
    publication: Publication,
    link: Link,
    baseUrl: String,
    scale: Float
) {
    val actualScale = if (scale == 1f) ContentScale.Fit else FixedScale(scale)
    when {
        link.mediaType.isBitmap ->
            ImageContent(publication, link, actualScale)
        link.mediaType.isHtml ->
            WebContent(link.withBaseUrl(baseUrl).href)
    }
}


/*internal fun Modifier.fitMaxWidth() = composed(
    factory = {
       FitMaxWidthModifier()
    }
)

private class FitMaxWidthModifier : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val childConstraints = constraints.copy(
            maxHeight = Constraints.Infinity,
            maxWidth = constraints.maxWidth
        )
        val placeable = measurable.measure(childConstraints)
        val width = placeable.width.coerceAtMost(constraints.maxWidth)
        val height = placeable.height.coerceAtMost(constraints.maxHeight)
        return layout(width, height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}

@Composable
fun Page(
    modifier: Modifier,
    content: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun flingBehavior(): FlingBehavior {
    val flingSpec = rememberSplineBasedDecay<Float>()
    return remember(flingSpec) {
        NavigatorFlingBehavior(flingSpec)
    }
}

private class NavigatorFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return if (abs(initialVelocity) > 1f) {
            var velocityLeft = initialVelocity
            var lastValue = 0f
            AnimationState(
                initialValue = 0f,
                initialVelocity = initialVelocity,
            ).animateDecay(flingDecay) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
            velocityLeft
        } else {
            initialVelocity
        }
    }
}

@Composable
fun TestContent() {
    Row {
        Box(
            modifier = Modifier
                .height(100.dp)
                .width(300.dp)
                .clip(RectangleShape)
                .background(Color.Red)
        )
        Box(
            modifier = Modifier
                .height(100.dp)
                .width(300.dp)
                .clip(RectangleShape)
                .background(Color.Yellow)
        )
    }
}*/
