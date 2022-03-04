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
import org.readium.r2.navigator3.image.ImageSpread
import org.readium.r2.navigator3.image.ImageSpreadState
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

@Composable
fun Navigator(
    state: NavigatorState,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null
) {
    BoxWithConstraints(
        propagateMinConstraints = true
    ) {
        state.layout(constraints.maxWidth, constraints.maxHeight)

        val isVertical = state.currentLayout.isVertical

        val reverseDirection = state.currentLayout.reverseDirection

        val horizontalArrangement = if (!reverseDirection) Arrangement.Start else Arrangement.End

        val horizontalAlignment = Alignment.CenterHorizontally

        val verticalArrangement = if (!reverseDirection) Arrangement.Top else Arrangement.Bottom

        val verticalAlignment = Alignment.CenterVertically

        val onTapWithLog: (Offset) -> Unit =
            { offset: Offset ->
                Timber.v("layoutInfo ${state.viewerState.lazyListState.layoutInfo.viewportStartOffset} ${state.viewerState.lazyListState.layoutInfo.viewportEndOffset} ")
                state.viewerState.lazyListState.layoutInfo.visibleItemsInfo.forEach {
                    Timber.v("itemLayoutInfo ${it.index} ${it.key} ${it.offset} ${it.size}")
                }
                onTap?.invoke(offset) }

        if (state.currentLayout.isPaginated) {
            LazyPager(
                modifier = Modifier,
                isVertical = isVertical,
                state = state.viewerState,
                contentPadding = PaddingValues(0.dp),
                reverseDirection = reverseDirection,
                horizontalArrangement = horizontalArrangement,
                horizontalAlignment =  horizontalAlignment,
                verticalArrangement = verticalArrangement,
                verticalAlignment = verticalAlignment,
                userScrollable = state.currentLayout.viewerScrollable,
                onTap = onTapWithLog,
                onDoubleTap = onDoubleTap,
                count = state.links.size
            ) { index, scaleState ->
                Spread(
                    state.publication,
                    state.currentLayout, index,
                    scaleState,
                    onTapWithLog,
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
                modifier = Modifier,
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
                    state.currentLayout,
                    index,
                    state.viewerState.zoomState.scaleState,
                    onTapWithLog,
                    onDoubleTap
                )
            }
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
            ImageSpread(publication, spread.link, scaleState, layout.isPaginated)
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
