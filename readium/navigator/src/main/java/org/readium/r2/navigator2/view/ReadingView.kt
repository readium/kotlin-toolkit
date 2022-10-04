package org.readium.r2.navigator2.view

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import org.readium.r2.navigator.R
import org.readium.r2.navigator2.view.layout.EffectiveReadingProgression

internal class ReadingView(context: Context, attributes: AttributeSet? = null)
    : FrameLayout(context, attributes)  {

    var singleTapListener: ((PointF) -> Unit)? =
        null

    var scrollListener: (() -> Unit)? = null

    private val recyclerView: RecyclerView =
        inflate(context, R.layout.navigator2_reading_view, this)
            .findViewById(R.id.r2_reading_view_recycler_view)

    private val layoutManager: NavigatorLayoutManager =
        NavigatorLayoutManager(context)

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

    init {
        recyclerView.layoutManager = layoutManager
        recyclerView.addOnScrollListener(onScrollListener)
    }

    fun setReadingProgression(progression: EffectiveReadingProgression) {
      layoutManager.setReadingProgression(progression)
    }

    fun setContinuous(continuous: Boolean) {
        pagerSnapHelper.attachToRecyclerView(if (continuous) null else recyclerView)
    }

    fun <VH: ViewHolder> setAdapter(adapter: Adapter<VH>) {
       recyclerView.adapter = adapter
    }

    suspend fun scrollToPosition(position: Int) {
        layoutManager.scrollTo(position)
    }

    fun findViewByPosition(position: Int): View? {
        return layoutManager.findViewByPosition(position)
    }

    fun findFirstVisiblePosition(): Int {
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        check(firstVisible != -1)
        return firstVisible
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(ev)
    }
}