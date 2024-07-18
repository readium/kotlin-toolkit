package org.readium.navigator.web.util

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.mutableStateOf
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import org.readium.navigator.web.webview.RelaxedWebView
import timber.log.Timber

internal class WebViewScrollableState() : ScrollableState {

    init {
        Timber.d("Init $this")
    }

    var webView: RelaxedWebView? = null

    override fun dispatchRawDelta(delta: Float): Float {
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

    private val scrollScope: ScrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float {
            if (pixels.isNaN()) return 0f
            return dispatchRawDelta(pixels)
        }
    }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            try {
                block()
            } finally {
                isScrollingState.value = false
            }
        }
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    override val canScrollForward: Boolean
        get() = webView?.canScrollHorizontally(1) ?: false

    override val canScrollBackward: Boolean
        get() = webView?.canScrollHorizontally(-1) ?: false
}
