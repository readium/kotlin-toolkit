package org.readium.r2.navigator3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator3.core.viewer.LazyPager
import org.readium.r2.navigator3.core.viewer.LazyScroller
import org.readium.r2.navigator3.html.HtmlSpread
import org.readium.r2.navigator3.html.HtmlSpreadState
import org.readium.r2.navigator3.image.ImageSpreadState
import org.readium.r2.navigator3.image.SingleImageResource
import org.readium.r2.shared.publication.Publication

@Composable
fun Navigator(
    modifier: Modifier = Modifier,
    state: NavigatorState,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null
) {
    val isVertical = state.layout.isVertical

    val reverseDirection = state.layout.reverseDirection

    val horizontalArrangement = if (!reverseDirection) Arrangement.Start else Arrangement.End

    val horizontalAlignment = Alignment.CenterHorizontally

    val verticalArrangement = if (!reverseDirection) Arrangement.Top else Arrangement.Bottom

    val verticalAlignment = Alignment.CenterVertically

    if (state.layout.isPaginated) {
        LazyPager(
            modifier = modifier,
            isVertical = isVertical,
            state = state.viewerState,
            contentPadding = PaddingValues(0.dp),
            reverseDirection = reverseDirection,
            horizontalArrangement = horizontalArrangement,
            horizontalAlignment =  horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            count = state.links.size
        ) { index, scaleState ->
            Spread(
                state.publication,
                state.layout, index,
                scaleState,
                onTap,
                onDoubleTap
            )
        }
    } else {
        val itemSize: (Int) -> Size = { index ->
            with(state.links[index]) {
                Size(width!!.toFloat(), height!!.toFloat())
            }
        }

        LazyScroller(
            modifier = modifier,
            isVertical = isVertical,
            state = state.viewerState,
            contentPadding = PaddingValues(0.dp),
            reverseDirection = reverseDirection,
            horizontalArrangement = horizontalArrangement,
            horizontalAlignment =  horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            count = state.links.size,
            itemSize = itemSize
        ) { index ->
            Spread(
                state.publication,
                state.layout,
                index,
                state.viewerState.zoomState.scaleState,
                onTap,
                onDoubleTap
            )
        }
    }
}

@Composable
private fun Spread(
    publication: Publication,
    layout: LayoutFactory.Layout,
    index: Int,
    scaleState: MutableState<Float>,
    onTap: ((Offset) -> Unit)?,
    onDoubleTap: ((Offset) -> Unit)?
) {
    when (val spread = layout.spreadStates[index]) {
        is ImageSpreadState ->
            SingleImageResource(publication, spread.link, scaleState, layout.isPaginated)
        is HtmlSpreadState ->
            HtmlSpread(publication, spread.link, layout.isPaginated, spread, onTap, onDoubleTap)
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

