package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.readium.r2.navigator.APPEARANCE_REF
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.SCROLL_REF
import android.view.GestureDetector
import android.view.MotionEvent


class R2PageFragment : Fragment() {

    private val TAG = this::class.java.simpleName

    val resourceUrl: String?
        get() = arguments!!.getString("url")

    val bookTitle: String?
        get() = arguments!!.getString("title")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_page, container, false)
        val prefs = activity?.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        // Set text color depending of appearance preference
        (v.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor(
                if (prefs?.getInt(APPEARANCE_REF, 0) ?: 0 > 1) "#ffffff" else "#000000"
        ))

        val scrollMode = prefs?.getBoolean(SCROLL_REF, false)
        when (scrollMode) {
            true -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.GONE
                v.setPadding(0,4,0,4)
            }
            false -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.VISIBLE
                v.setPadding(0,30,0,30)
            }
        }

        (v.findViewById(R.id.book_title) as TextView).text = bookTitle

        val webView: R2WebView = v!!.findViewById(R.id.webView) as R2WebView

        webView.activity = activity as R2EpubActivity

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                try {
                    val progression = (activity as R2EpubActivity).preferences.getString("${(activity as R2EpubActivity).publicationIdentifier}-documentProgression", 0.0.toString()).toDouble()

                    when (progression) {
                        0.0 -> webView.scrollToBeginning()
                        1.0 -> webView.scrollToEnd()
                        else -> webView.scrollToPosition(progression)
                    }
                } catch (e: Exception) {
                    // TODO double check this error, a crash happens when scrolling to fast between resources.....
                    // kotlin.TypeCastException: null cannot be cast to non-null type org.readium.r2.navigator.R2EpubActivity
                }

            }

        }

/*
        webView.setOnTouchListener(object : View.OnTouchListener {

            internal var startX = 0
            internal var startY = 0
            internal var SCROLL_THRESHOLD = 300f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startX = event.x.toInt()
                    startY = event.y.toInt()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    val absX = Math.abs(startX - event.x)
                    val absY = Math.abs(startY - event.y)
                    val angle = Math.toDegrees(Math.atan2((event.y - startY).toDouble(), (event.x - startX).toDouble())).toFloat()
                    if (angle > -45 && angle <= 45) {
//                        LOG.debug("Right to Left swipe performed")
                        if (absX > SCROLL_THRESHOLD) {
                            webView.scrollLeft()
                            return true
                        }
                    }
                    if (angle >= 135 && angle < 180 || angle < -135 && angle > -180) {
//                        LOG.debug("Left to Right swipe performed")
                        if (absX > SCROLL_THRESHOLD) {
                            webView.scrollRight()
                            return true
                        }
                    }
                    if (angle < -45 && angle >= -135) {
//                        LOG.debug("Up to Down swipe performed")
                        if (absY > SCROLL_THRESHOLD) {
                            webView.CenterTapped()
                            return true
                        }
                    }
                    if (angle > 45 && angle <= 135) {
//                        LOG.debug("Down to Up swipe performed")
                        if (absY > SCROLL_THRESHOLD) {
                            webView.CenterTapped()
                            return true
                        }
                    }
                    return true
                }
                if (event.action == MotionEvent.ACTION_MOVE) {
                    if ((activity as R2EpubActivity).userProperties.verticalScroll) {
                        return false
                    }
                    return true
                }
                return true
            }
//            override fun onTouch(v: View, event: MotionEvent): Boolean {
//
//                    if (event.action == MotionEvent.ACTION_MOVE) {
//                    if ((activity as R2EpubActivity).userProperties.isVerticalScrollEnabled) {
//                        return false
//                    }
//                    return true
//                }
//                return false
//            }
        })
*/
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            true
        }
        webView.setGestureDetector(GestureDetector(context, CustomeGestureDetector(webView)))
        webView.loadUrl(resourceUrl)

        return v
    }

    class CustomeGestureDetector(val webView: R2WebView) : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null) return false
            if (e1.pointerCount > 1 || e2.pointerCount > 1)
                return false
            else {
                try { // right to left swipe .. go to next page
                    if (e1.x - e2.x > 100) {
                        webView.scrollRight()
                        return true
                    } //left to right swipe .. go to prev page
                    else if (e2.x - e1.x > 100) {
                        webView.scrollLeft()
                        return true
                    }
                } catch (e: Exception) { // nothing
                }

                return false
            }
        }
    }
    companion object {

        fun newInstance(url: String, title: String): R2PageFragment {

            val args = Bundle()
            args.putString("url", url)
            args.putString("title", title)
            val fragment = R2PageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


