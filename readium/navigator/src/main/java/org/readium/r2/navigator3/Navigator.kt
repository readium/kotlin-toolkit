package org.readium.r2.navigator3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator3.adapters.ImageContent
import org.readium.r2.navigator3.adapters.WebContent
import org.readium.r2.navigator3.lazy.LazyListScope
import org.readium.r2.navigator3.lazy.items
import org.readium.r2.navigator3.settings.Overflow
import org.readium.r2.navigator3.settings.ReadingProgression
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
    baseUrl: String,
    links: List<Link> = publication.readingOrder,
) {
    val readingProgression = ReadingProgression.LTR
    val overflow = Overflow.PAGINATED

    val isVertical = when (readingProgression) {
        ReadingProgression.TTB, ReadingProgression.BTT -> true
        ReadingProgression.LTR, ReadingProgression.RTL -> false
    }

    val reverseLayout = when (readingProgression) {
        ReadingProgression.TTB, ReadingProgression.LTR -> false
        ReadingProgression.BTT, ReadingProgression.RTL -> true
    }

    val horizontalArrangement = if (!reverseLayout) Arrangement.Start else Arrangement.End

    val horizontalAlignment = Alignment.CenterHorizontally

    val verticalArrangement = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom

    val verticalAlignment = Alignment.CenterVertically

    if (overflow == Overflow.SCROLLED) {

        val state = rememberLazyScrollerState(isVertical)

        val content: (LazyListScope).() -> Unit = { resources(publication, links, baseUrl, overflow, isVertical, state.scaleState) }

        LazyScroller(
            modifier = modifier,
            isVertical = isVertical,
            state = state,
            contentPadding = PaddingValues(0.dp),
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            horizontalAlignment =  horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )
    } else {

        val state = rememberLazyPagerState(isVertical)

        val fixedScale = remember { mutableStateOf(1f) }

        val content: (LazyListScope).() -> Unit = { resources(publication, links, baseUrl, overflow, isVertical, fixedScale) }

        LazyPager(
            modifier = modifier,
            isVertical = isVertical,
            state = state,
            contentPadding = PaddingValues(0.dp),
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            horizontalAlignment =  horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )
    }
}

private fun LazyListScope.resources(
    publication: Publication,
    links: List<Link>,
    baseUrl: String,
    overflow: Overflow,
    isVertical: Boolean,
    scaleState: State<Float>
) {
    items(links) { item ->

        val itemModifier =
            when  {
                overflow == Overflow.SCROLLED && isVertical -> Modifier.fillParentMaxWidth()
                overflow == Overflow.SCROLLED -> Modifier.fillParentMaxHeight()
                else -> Modifier.fillParentMaxSize()
        }

        when {
            item.mediaType.isBitmap ->
                ImageContent(itemModifier, publication, item, FixedScale(scaleState.value))
            item.mediaType.isHtml ->
                WebContent(item.withBaseUrl(baseUrl).href)
        }
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
