package org.readium.navigator.internal.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.readium.navigator.internal.gestures.ScrollState
import org.readium.navigator.internal.gestures.rememberScrollState
import org.readium.navigator.internal.gestures.scrollable
import org.readium.navigator.internal.gestures.scrolling
import org.readium.navigator.internal.lazy.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedLazyColumn(
    modifier: Modifier,
    lazyListState: LazyListState,
    horizontalScrollState: ScrollState,
    reverseLayout: Boolean,
    snap: Boolean,
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: EnhancedLazyListScope.() -> Unit
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        LazyColumn(
            modifier = Modifier
                .scrollable(
                    horizontalState = horizontalScrollState,
                    verticalState = lazyListState,
                    reverseDirection = !reverseLayout,
                    interactionSource = lazyListState.internalInteractionSource,
                    flingBehavior = when (snap) {
                        true -> rememberSnapFlingBehavior(lazyListState)
                        false -> ScrollableDefaults.flingBehavior()
                    },
                    enabled = userScrollable
                )
                .scrolling(
                    state = horizontalScrollState,
                    isVertical = false,
                    reverseScrolling = ScrollableDefaults.reverseDirection(
                        LocalLayoutDirection.current, Orientation.Horizontal, reverseLayout
                    ),
                ),
            state = lazyListState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            userScrollEnabled = false
        ) {
            val lazyItemScope = EnhancedLazyItemScope()
            lazyItemScope.setMaxSize(constraints.maxWidth, constraints.maxHeight)
            val lazyListScope = EnhancedLazyListScope(this, lazyItemScope)
            lazyListScope.content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedLazyRow(
    modifier: Modifier,
    lazyListState: LazyListState,
    verticalScrollState: ScrollState,
    reverseLayout: Boolean,
    snap: Boolean,
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: EnhancedLazyListScope.() -> Unit
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        LazyRow(
            modifier = Modifier
                .scrollable(
                    horizontalState = lazyListState,
                    verticalState = verticalScrollState,
                    reverseDirection = ScrollableDefaults.reverseDirection(
                        LocalLayoutDirection.current, Orientation.Horizontal, reverseLayout
                    ),
                    interactionSource = lazyListState.internalInteractionSource,
                    flingBehavior = when (snap) {
                        true -> rememberSnapFlingBehavior(lazyListState)
                        false -> ScrollableDefaults.flingBehavior()
                    },
                    enabled = userScrollable
                )
                .scrolling(
                    state = verticalScrollState,
                    isVertical = true,
                    reverseScrolling = !reverseLayout
                ),
            state = lazyListState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            userScrollEnabled = false,
        ) {
            val lazyItemScope = EnhancedLazyItemScope()
            lazyItemScope.setMaxSize(constraints.maxWidth, constraints.maxHeight)
            val lazyListScope = EnhancedLazyListScope(this, lazyItemScope)
            lazyListScope.content()
        }
    }
}

@Composable
@Preview
private fun EnhancedLazyColumnPreview() {
    EnhancedLazyColumn(
        modifier = Modifier,
        lazyListState = rememberLazyListState(),
        horizontalScrollState = rememberScrollState(),
        reverseLayout = false,
        snap = false,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 0 until 100) {
            item {
                TestRow()
            }
        }
    }
}

@Composable
@Preview
private fun EnhancedLazyRowPreview() {
    EnhancedLazyRow(
        modifier = Modifier,
        lazyListState = rememberLazyListState(),
        verticalScrollState = rememberScrollState(),
        reverseLayout = false,
        snap = false,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        for (i in 0 until 100) {
            item {
                TestColumn()
            }
        }
    }
}

@Composable
private fun TestRow() {
    Row(
        modifier = Modifier
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .height(50.dp)
                .width(3000.dp)
                .clip(RectangleShape)
                .background(Color.Red)
        )
        Box(
            modifier = Modifier
                .height(50.dp)
                .width(3000.dp)
                .clip(RectangleShape)
                .background(Color.Yellow)
        )
    }
}

@Composable
private fun TestColumn() {
    Column(
        modifier = Modifier
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .height(3000.dp)
                .width(50.dp)
                .clip(RectangleShape)
                .background(Color.Red)
        )
        Box(
            modifier = Modifier
                .height(3000.dp)
                .width(50.dp)
                .clip(RectangleShape)
                .background(Color.Yellow)
        )
    }
}
