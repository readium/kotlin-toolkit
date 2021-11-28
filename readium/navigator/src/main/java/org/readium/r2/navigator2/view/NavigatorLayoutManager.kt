package org.readium.r2.navigator2.view

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_LTR
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import org.readium.r2.navigator2.view.layout.EffectiveReadingProgression
import kotlin.coroutines.Continuation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class NavigatorLayoutManager(
    private val context: Context,
    //private val getAdapterForView: (View) -> NavigatorAdapter.ViewHolder
    ) : LinearLayoutManager(context) {

    private var scrollState: Int = SCROLL_STATE_IDLE
    private var scrollContinuation: Continuation<Unit>? = null
    private var scrollTargetPosition: Int = NO_POSITION

    fun setReadingProgression(progression: EffectiveReadingProgression) {
        orientation = when {
            progression.isHorizontal -> HORIZONTAL
            else -> VERTICAL
        }
        reverseLayout = when (progression) {
            EffectiveReadingProgression.TTB -> false
            EffectiveReadingProgression.BTT -> true
            EffectiveReadingProgression.LTR -> layoutDirection == LAYOUT_DIRECTION_RTL
            EffectiveReadingProgression.RTL -> layoutDirection == LAYOUT_DIRECTION_LTR
        }
        requestLayout()
    }

    suspend fun scrollTo(position: Int) {
        if (scrollState == SCROLL_STATE_DRAGGING) {
            throw CancellationException()
        }

        resumeCurrentScrollWithCancellation()

        scrollTargetPosition = position
        scrollToPositionWithOffset(position, 0)

        suspendCoroutine<Unit> { continuation ->
            scrollContinuation = continuation
        }
    }

    suspend fun smoothScrollTo(position: Int) {
        if (scrollState == SCROLL_STATE_DRAGGING) {
            throw CancellationException()
        }

        resumeCurrentScrollWithCancellation()

        scrollTargetPosition = position
        val smoothScroller = NavigatorSmoothScroller(context, position)
        startSmoothScroll(smoothScroller)

        suspendCoroutine<Unit> { continuation ->
            scrollContinuation = continuation
        }
    }

    private fun resumeCurrentScroll() {
        val continuation = scrollContinuation
        scrollContinuation = null
        scrollTargetPosition = NO_POSITION
        continuation?.resume(Unit)
    }

    private fun resumeCurrentScrollWithCancellation() {
        val continuation = scrollContinuation
        scrollContinuation = null
        scrollTargetPosition = NO_POSITION
        continuation?.resumeWithException(CancellationException())
    }

    override fun onLayoutCompleted(state: State?) {
        super.onLayoutCompleted(state)

        if (scrollTargetPosition != NO_POSITION && findViewByPosition(scrollTargetPosition) != null) {
           resumeCurrentScroll()
        }
    }

    override fun onScrollStateChanged(state: Int) {
        when (state) {
            SCROLL_STATE_IDLE -> {
                // Nothing to do
            }
            SCROLL_STATE_DRAGGING -> {
                resumeCurrentScrollWithCancellation()
            }
            SCROLL_STATE_SETTLING -> {
                // Nothing to do
            }
        }

        super.onScrollStateChanged(state)

        scrollState = state
    }

    override fun onRequestChildFocus(
        parent: RecyclerView,
        state: State,
        child: View,
        focused: View?
    ): Boolean {
        /*
         * Suppress the default scroll behaviour when a new child is focused.
         * This is necessary to prevent the RecyclerView to scroll to the top.
         */
        return true
    }
}