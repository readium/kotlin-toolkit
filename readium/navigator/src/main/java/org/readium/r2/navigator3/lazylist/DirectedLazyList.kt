package org.readium.r2.navigator3.lazylist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun DirectedLazyList(
    direction: Direction,
    modifier: Modifier,
    state: LazyListState,
    contentPadding: PaddingValues,
    content: LazyListScope.() -> Unit
) {
    when (direction) {
        Direction.TTB ->
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = false,
                content = content
            )
        Direction.BTT ->
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = true,
                content = content
            )
        Direction.LTR ->
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = false,
                content = content
            )
        Direction.RTL -> {
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = true,
                content = content
            )
        }
    }
}