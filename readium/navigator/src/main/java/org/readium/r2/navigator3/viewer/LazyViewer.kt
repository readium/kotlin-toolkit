package org.readium.r2.navigator3.viewer

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator3.gestures.ScrollState
import org.readium.r2.navigator3.gestures.scrollable
import org.readium.r2.navigator3.gestures.scrolling
import org.readium.r2.navigator3.gestures.zoomable
import org.readium.r2.navigator3.lazy.LazyList
import org.readium.r2.navigator3.lazy.LazyListScope
import org.readium.r2.navigator3.lazy.LazyListState
import org.readium.r2.navigator3.lazy.rememberStateOfItemsProvider

@Composable
internal fun LazyViewer(
    modifier: Modifier = Modifier,
    isVertical: Boolean,
    isZoomable: Boolean,
    state: LazyViewerState = rememberLazyViewerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalArrangement: Arrangement.Horizontal? = null,
    verticalAlignment: Alignment.Vertical? = null,
    horizontalAlignment: Alignment.Horizontal? = null,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // reverse scroll by default, to have "natural" gesture that goes reversed to layout
    // if rtl and horizontal, do not reverse to make it right-to-left
    val reverseScrollDirection = if (!isVertical && isRtl) reverseLayout else !reverseLayout

    @Suppress("NAME_SHADOWING")
    val modifier = (if (isZoomable) modifier.zoomable(state.scaleState) else modifier)
        .scrollable(
            horizontalState = if (isVertical) state.otherScrollState else state.lazyListState,
            verticalState = if (isVertical) state.lazyListState else state.otherScrollState,
            reverseDirection = reverseScrollDirection,
            interactionSource = state.lazyListState.internalInteractionSource,
            flingBehavior = flingBehavior
        )
        .scrolling(
            state = state.otherScrollState,
            isVertical = !isVertical,
            reverseScrolling = reverseScrollDirection
        )

    LazyList(
        modifier = modifier,
        stateOfItemsProvider = rememberStateOfItemsProvider(content),
        state = state.lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = isVertical,
        reverseLayout = reverseLayout
    )
}


internal class LazyViewerState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    oppositeDirectionScrollOffset: Int = 0,
    scale: Float = 1f,
) {
    var scaleState: MutableState<Float> =
        mutableStateOf(scale)

    val lazyListState: LazyListState =
        LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    val otherScrollState: ScrollState =
        ScrollState(oppositeDirectionScrollOffset)

    val scale: Float
        get() = scaleState.value

    companion object {
        val Saver: Saver<LazyViewerState, *> = listSaver(
            save = {
                listOf(
                    it.lazyListState.firstVisibleItemIndex,
                    it.lazyListState.firstVisibleItemScrollOffset,
                    it.otherScrollState.value,
                    it.scale
                )
            },
            restore = {
                LazyViewerState(
                    firstVisibleItemIndex = it[0] as Int,
                    firstVisibleItemScrollOffset = it[1] as Int,
                    oppositeDirectionScrollOffset = it[2] as Int,
                    scale = it[3] as Float
                )
            }
        )
    }
}

@Composable
internal fun rememberLazyViewerState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialOppositeDirectionScrollOffset: Int = 0,
    initialScale: Float = 1f,
): LazyViewerState {
    return rememberSaveable(saver = LazyViewerState.Saver) {
        LazyViewerState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            initialOppositeDirectionScrollOffset,
            initialScale
        )
    }
}