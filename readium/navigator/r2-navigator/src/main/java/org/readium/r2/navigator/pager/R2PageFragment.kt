package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.UserSettings.Appearance
import org.readium.r2.navigator.UserSettings.Scroll


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

        val appearance_pref = prefs?.getString("appearance", Appearance.Default.toString()) ?: Appearance.Default.toString()
        when (appearance_pref) {
            Appearance.Default.toString() -> {
                (v.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#000000"))
            }
            Appearance.Sepia.toString() -> {
                (v.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#000000"))
            }
            Appearance.Night.toString() -> {
                (v.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#ffffff"))
            }
        }

        val scroll_mode = prefs?.getString("scroll", Scroll.Off.toString())
        when (scroll_mode) {
            Scroll.On.toString() -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.GONE
                v.setPadding(0,4,0,4)
            }
            Scroll.Off.toString() -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.VISIBLE
                v.setPadding(0,30,0,30)
            }
        }

        (v.findViewById(R.id.book_title) as TextView).setText(bookTitle)

        val webView: R2WebView = v!!.findViewById<R2WebView>(R.id.webView) as R2WebView

        webView.activity = activity as R2EpubActivity

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
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
                    (activity as R2EpubActivity).userSettings.applyAllCSS(view as R2WebView)
                } catch (e: Exception) {
                    //TODO double check this error, a crash happens when scrolling to fast bewteen resources.....
                    // kotlin.TypeCastException: null cannot be cast to non-null type org.readium.r2.navigator.R2EpubActivity
                }

            }

        }

        webView.setOnTouchListener(object : View.OnTouchListener {

            override fun onTouch(v: View, event: MotionEvent): Boolean {

                    if (event.action == MotionEvent.ACTION_MOVE) {
                    if ((activity as R2EpubActivity).userSettings.isVerticalScrollEnabled) {
                        return false
                    }
                    return true
                }
                return false
            }
        })

        webView.loadUrl(resourceUrl)

        return v
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


