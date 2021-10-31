package org.readium.r2.navigator2.view

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.readium.r2.navigator.R
import org.readium.r2.navigator2.view.layout.EffectiveReadingProgression
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ReadingView(context: Context, attributes: AttributeSet? = null)
    : FrameLayout(context, attributes)  {

    var singleTapListener: ((PointF) -> Unit)? =
        null

    var scrollListener: (() -> Unit)? = null

    private val recyclerView: RecyclerView =
        inflate(context, R.layout.navigator2_reading_view, this)
            .findViewById(R.id.r2_reading_view_recycler_view)

    private val layoutManager: LinearLayoutManager =
        LinearLayoutManager(context)

    private val pagerSnapHelper: PagerSnapHelper =
        PagerSnapHelper()

    private val gestureListener: GestureDetector.OnGestureListener =
        object: GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                singleTapListener?.let { it(PointF(e.x, e.y)) }
                return true
            }
        }

    private val gestureDetector: GestureDetectorCompat =
        GestureDetectorCompat(context, gestureListener)

    private var scrollFinishedCallbacks: MutableList<Pair<Int, () -> Unit>> =
        mutableListOf()


    private val onScrollListener: OnScrollListener =
        object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // Do nothing
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                scrollListener?.let { it() }
            }
        }

    private val onLayoutChangeListener: OnLayoutChangeListener =
        OnLayoutChangeListener { p0, p1, p2, p3, p4, p5, p6, p7, p8 ->
            val newPendingCallbacks: MutableList<Pair<Int, () -> Unit>> = mutableListOf()
            for ((position, callback) in scrollFinishedCallbacks) {
                if (recyclerView.findViewHolderForAdapterPosition(position) == null) {
                    newPendingCallbacks.add(position to callback)
                } else {
                    callback.invoke()
                }
            }
            scrollFinishedCallbacks = newPendingCallbacks
        }

    init {
        recyclerView.layoutManager = layoutManager
        recyclerView.layoutDirection = LAYOUT_DIRECTION_LTR
        //recyclerView.addOnChildAttachStateChangeListener(childAttachedListener)
        recyclerView.addOnScrollListener(onScrollListener)
        recyclerView.addOnLayoutChangeListener(onLayoutChangeListener)
    }

    fun setReadingProgression(progression: EffectiveReadingProgression) {
        this.layoutManager.reverseLayout = when (progression) {
            EffectiveReadingProgression.LTR, EffectiveReadingProgression.TTB -> false
            EffectiveReadingProgression.RTL, EffectiveReadingProgression.BTT -> true
        }

        this.layoutManager.requestLayout()
    }

    fun setContinuous(continuous: Boolean) {
        this.layoutManager.orientation = if (continuous) VERTICAL else HORIZONTAL
        this.pagerSnapHelper.attachToRecyclerView(if (continuous) null else recyclerView)
    }

    fun <VH: ViewHolder> setAdapter(adapter: Adapter<VH>) {
        this.recyclerView.adapter = adapter
    }

    suspend fun scrollToPosition(position: Int) {
        // If a previous call is pending, queue this new one.
        if (scrollFinishedCallbacks.isNotEmpty()) {
            suspendCoroutine<Unit> { continuation ->
                this.scrollFinishedCallbacks.add(position to {
                    continuation.resume(Unit)
                })
            }
        }

        this.recyclerView.scrollToPosition(position)
        // Return immediately if the item is already loaded, suspend otherwise.
        if (this.recyclerView.findViewHolderForAdapterPosition(position) == null) {
            suspendCoroutine<Unit> { continuation ->
                this.scrollFinishedCallbacks.add(position to {
                    continuation.resume(Unit)
                })
            }
        } else if (this.layoutManager.findFirstVisibleItemPosition() < position ) {

        }
    }

    fun findViewByPosition(position: Int): View? {
        return this.layoutManager.findViewByPosition(position)
    }

    fun findFirstVisiblePosition(): Int {
        val firstVisible = this.layoutManager.findFirstVisibleItemPosition()
        check(firstVisible != -1)
        return firstVisible
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (scrollFinishedCallbacks.isNotEmpty()) {
            // Prevent any scroll while the previous one  has not completed
            true
        } else {
            gestureDetector.onTouchEvent(ev)
        }
    }
}