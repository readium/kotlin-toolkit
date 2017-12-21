package org.readium.r2.navigator.pager

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity


class R2PageFragment : Fragment() {

    val someIdentifier: String?
        get() = arguments.getString("url")


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater!!.inflate(R.layout.fragment_page, container, false)
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
                    (activity as R2EpubActivity).cssOperator.applyAllCSS(view as R2WebView)
                } catch (e: Exception) {
                    //TODO double check this error, a scrash happens when scrolling to fast bewteen resources.....
                    // kotlin.TypeCastException: null cannot be cast to non-null type org.readium.r2.navigator.R2EpubActivity
                }
            }

        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
            }
        }


        webView.setOnScrollStoppedListener(object : R2WebView.OnScrollStoppedListener {

            override fun onScrollStopped() {

                Log.d("TAG", "stopped")
                webView.snap()

            }
        })


        webView.setOnTouchListener(object : View.OnTouchListener {

            internal var startX = 0
            internal var startY = 0
            internal var SCROLL_THRESHOLD = 10f



            override fun onTouch(v: View, event: MotionEvent): Boolean {




                if (event.action == MotionEvent.ACTION_DOWN) {

                    startX = event.x.toInt()
                    startY = event.y.toInt()


                } else if (event.action == MotionEvent.ACTION_UP) {

                    webView.startScrollerTask();

                    val viewWidth = webView.getWidth()
                    val viewHeight = webView.getHeight()
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    val absX = Math.abs(startX - event.x)
                    val absY = Math.abs(startY - event.y)

                    val relativeDistanceX = (event.x - startX) / viewWidth
                    val relativeDistanceY = (event.y - startY) / viewHeight
                    val slope = (event.y - startY) / (event.x - startX)


                    Log.d("relativeDistanceX", relativeDistanceX.toString())
                    Log.d("relativeDistanceY", relativeDistanceY.toString())
                    Log.d("slope", slope.toString())



                }

                return false
            }
        })

        


        webView.loadUrl(someIdentifier)


        return v
    }

    companion object {

        fun newInstance(url: String): R2PageFragment {

            val args = Bundle()
            args.putString("url", url)
            val fragment = R2PageFragment()
            fragment.arguments = args
            return fragment
        }
    }
}


