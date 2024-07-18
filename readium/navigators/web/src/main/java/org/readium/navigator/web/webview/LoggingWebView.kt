package org.readium.navigator.web.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.r2.shared.InternalReadiumApi
import timber.log.Timber

@OptIn(InternalReadiumApi::class)
internal class LoggingWebView(context: Context) : WebView(context) {

    lateinit var nestedScrollDispatcher: NestedScrollDispatcher

    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /*init {
        val gestureDetector = GestureDetector()
        addJavascriptInterface(gestureDetector, "Gestures")
    }*/

    data class State(
        val downPosition: Offset,
        val lastMoveDest: Offset? = null,
        val cumulatedDelta: Offset = Offset.Zero
    )

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var state: State? = null

    private val disallowInterceptTouchEvent: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    init {
        disallowInterceptTouchEvent
            .onEach {
                Timber.d(
                    if (it) {
                        "disallowIntercept"
                    } else {
                        "allowIntercept"
                    }
                )
                requestDisallowInterceptTouchEvent(it)
            }
            .launchIn(coroutineScope)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        Timber.d("onInterceptTouchEvent $ev")
        val res = super.onInterceptTouchEvent(ev)
        // Timber.d("onInterceptTouchEvent $res $ev")
        return res
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Timber.d("onTouchEvent $event")
        Timber.d("onTouchEvent ${disallowInterceptTouchEvent.value} $event")

        val eventOffset = Offset(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                disallowInterceptTouchEvent.value = false
                state = State(downPosition = eventOffset)
            }
            MotionEvent.ACTION_MOVE -> {
                val currentMoveDelta = eventOffset - (state!!.lastMoveDest ?: state!!.downPosition)
                val absCurrentMoveDelta = abs(currentMoveDelta)
                val cumulatedDelta = state!!.cumulatedDelta + abs(eventOffset)
                state = state!!.copy(lastMoveDest = eventOffset, cumulatedDelta = cumulatedDelta)

                val horizontalSign = -sign(currentMoveDelta.x).toInt()
                val verticalSign = sign(currentMoveDelta.y).toInt()

                Timber.d("nestedScrollDispatcher.dispatchPreScroll $currentMoveDelta")
                val preconsumed =
                    nestedScrollDispatcher.dispatchPreScroll(
                        currentMoveDelta,
                        NestedScrollSource.Drag
                    )

                val remaining = currentMoveDelta - preconsumed
                val consumed = Offset(remaining.x.toInt().toFloat(), remaining.y.toInt().toFloat())

                scrollBy(consumed.x.toInt(), consumed.y.toInt())

                Timber.d(
                    "nestedScrollDispatcher.dispatchPostScroll ${preconsumed + consumed} ${remaining - consumed}"
                )
                nestedScrollDispatcher.dispatchPostScroll(
                    preconsumed + consumed,
                    remaining - consumed,
                    NestedScrollSource.Drag
                )

                val shouldScrollHorizontally =
                    state!!.isHorizontallyScrolling() && canScrollHorizontally(horizontalSign)

                val shouldScrollVertically =
                    state!!.isVerticallyScrolling() && canScrollVertically(verticalSign) &&
                        absCurrentMoveDelta.x < 2 * absCurrentMoveDelta.y

                disallowInterceptTouchEvent.value = shouldScrollHorizontally || shouldScrollVertically
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                disallowInterceptTouchEvent.value = false
                state = null
            }
        }

        val res = super.onTouchEvent(event)
        // Timber.d("onTouchEvent $res $event")
        return true
    }

    private fun State.isVerticallyScrolling() =
        cumulatedDelta.y > touchSlop

    private fun State.isHorizontallyScrolling() =
        cumulatedDelta.x > touchSlop

    private fun abs(offset: Offset): Offset {
        val absX = abs(offset.x)
        val absY = abs(offset.y)
        return Offset(absX, absY)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        Timber.d("dispatchTouchEvent $ev")
        val res = super.dispatchTouchEvent(ev)
        // Timber.d("dispatchTouchEvent $res $ev")
        return res
    }
}
