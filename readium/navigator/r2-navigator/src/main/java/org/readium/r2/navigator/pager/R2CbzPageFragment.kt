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
import android.webkit.WebResourceResponse
import kotlinx.android.synthetic.main.cbz_fragment_page.*
import org.jetbrains.anko.find


class R2CbzPageFragment : Fragment() {

    private val TAG = this::class.java.simpleName

    val resourceUrl: String?
        get() = arguments!!.getString("url")

    val bookTitle: String?
        get() = arguments!!.getString("title")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.cbz_fragment_page, container, false)
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
        val imageView = v.findViewById<R2ImageView>(R.id.imageView)
        //imageView.setImage
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

        fun newInstance(url: String, title: String): R2CbzPageFragment {

            val args = Bundle()
            args.putString("url", url)
            args.putString("title", title)
            val fragment = R2CbzPageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


