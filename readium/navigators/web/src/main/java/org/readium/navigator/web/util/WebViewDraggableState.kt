@file:OptIn(ExperimentalFoundationApi::class)

package org.readium.navigator.web.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.plus
/*
internal class WebViewDraggableState(
    private val nestedScrollDispatcher: NestedScrollDispatcher
) : Draggable2DState {

    var webView: RelaxedWebView? = null

    suspend fun onDragStopped(coroutineScope: CoroutineScope, velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            val preConsumedByParent = nestedScrollDispatcher
                .dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling(
                    (available - available),
                    available
                )
            val totalLeft = available - consumedPost
            velocity - totalLeft
        }
    }

    override fun dispatchRawDelta(delta: Offset) {
        Timber.d("dispatchRawDelta $delta $this")
        val webViewNow = webView ?: return 0f

        val scrollX = webViewNow.scrollX
        val maxX = webViewNow.horizontalScrollRange - webViewNow.horizontalScrollExtent
        Timber.d("scrollX $scrollX maxX $maxX")
        val newX = (scrollX - delta).coerceIn(0f, maxX.toFloat())
        webViewNow.scrollTo(newX.roundToInt(), 0)
        val consumed = (scrollX - webViewNow.scrollX).toFloat()
        Timber.d("consumed $consumed $this")
        return consumed
    }

    private val drag2DScope: Drag2DScope = object : Drag2DScope {
        override fun dragBy(pixels: Offset) = dispatchDrag(pixels)
    }

    private fun Drag2DScope.dispatchDrag(offset: Offset) {
        val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(offset, NestedScrollSource.Drag)

        val scrollAvailableAfterPreScroll = offset - consumedByPreScroll

        val consumedBySelfScroll =
            dragBy(scrollAvailableAfterPreScroll)

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll = nestedScrollDispatcher.dispatchPostScroll(
            consumedBySelfScroll,
            deltaAvailableAfterScroll,
            NestedScrollSource.Drag
        )
        consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val drag2DMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend Drag2DScope.() -> Unit
    ): Unit = coroutineScope {
        drag2DMutex.mutateWith(drag2DScope, dragPriority, block)
    }
}
*/
