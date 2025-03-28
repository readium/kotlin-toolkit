package org.readium.navigator.web.pager

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost

internal class PageNestedScrollConnection(
    private val index: Int,
    private val pagerState: PagerState,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }

        val pageInfo = pagerState.layoutInfo.visiblePagesInfo.firstOrNull { it.index == index }
            ?: return Offset.Zero

        // If the current resource doesn't start with the pager (i.e pageInfo.offset != 0), allow
        // the pager to scroll just as much as needed to make this resource the only visible one.
        // If the user moves in the opposite direction, do not allow the pager to move of course.
        val availableForPager = when {
            available.x > 0 && pageInfo.offset < 0 ->
                available.x.fastCoerceAtMost(-pageInfo.offset.toFloat())
            available.x < 0 && pageInfo.offset > 0 ->
                available.x.fastCoerceAtLeast(-pageInfo.offset.toFloat())
            else ->
                0f
        }

        val consumed = -pagerState.dispatchRawDelta(-availableForPager)
        return Offset(consumed, 0f)
    }
}
