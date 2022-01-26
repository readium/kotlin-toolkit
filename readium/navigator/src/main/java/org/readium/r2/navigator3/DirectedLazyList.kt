package org.readium.r2.navigator3

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.readium.r2.navigator3.lazy.LazyColumn
import org.readium.r2.navigator3.lazy.LazyListScope
import org.readium.r2.navigator3.lazy.LazyListState
import org.readium.r2.navigator3.lazy.LazyRow

@Composable
internal fun DirectedLazyList(
    direction: Direction,
    modifier: Modifier,
    state: LazyListState,
    contentPadding: PaddingValues,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit
) {
    when (direction) {
        Direction.TTB ->
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = false,
                flingBehavior = flingBehavior,
                content = content
            )
        Direction.BTT ->
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = true,
                flingBehavior = flingBehavior,
                content = content
            )
        Direction.LTR ->
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = false,
                flingBehavior = flingBehavior,
                content = content
            )
        Direction.RTL -> {
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = true,
                flingBehavior = flingBehavior,
                content = content
            )
        }
    }
}