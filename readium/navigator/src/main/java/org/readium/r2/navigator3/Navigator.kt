package org.readium.r2.navigator3

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator3.adapters.ImageContent
import org.readium.r2.navigator3.adapters.WebContent
import org.readium.r2.navigator3.lazy.items
import org.readium.r2.navigator3.viewer.LazyViewer
import org.readium.r2.navigator3.viewer.rememberLazyViewerState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import kotlin.math.abs

@Composable
fun Navigator(
    modifier: Modifier = Modifier,
    publication: Publication,
    baseUrl: String,
    links: List<Link> = publication.readingOrder,
) {
    val isVertical = when (publication.metadata.readingProgression) {
        ReadingProgression.AUTO, ReadingProgression.BTT, ReadingProgression.TTB -> true
        ReadingProgression.LTR, ReadingProgression.RTL -> false
    }

    val reverseLayout = when (publication.metadata.readingProgression) {
        ReadingProgression.AUTO, ReadingProgression.TTB, ReadingProgression.LTR -> false
        ReadingProgression.BTT, ReadingProgression.RTL -> true
    }

    val state = rememberLazyViewerState(isVertical)

    LazyViewer(
        modifier = modifier.fillMaxSize(),
        isVertical = isVertical,
        isZoomable = true,
        state = state,
        contentPadding = PaddingValues(0.dp),
        flingBehavior = flingBehavior(),
        reverseLayout = reverseLayout,
        horizontalArrangement = if (!reverseLayout) Arrangement.Start else Arrangement.End,
        horizontalAlignment =  Alignment.Start,
        verticalArrangement = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
        verticalAlignment = Alignment.Top
    ) {
        items(links) { item ->
            Column(
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
            ){
                Spacer(modifier = Modifier.padding(1.dp))
                //TestContent()
                when {
                    item.mediaType.isBitmap ->
                        ImageContent(publication, item, state.scaleState.value)
                    item.mediaType.isHtml ->
                        WebContent(item.withBaseUrl(baseUrl).href)
                }
            }
        }
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
}