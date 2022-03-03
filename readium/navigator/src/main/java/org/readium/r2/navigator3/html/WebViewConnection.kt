package org.readium.r2.navigator3.html

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import org.readium.r2.shared.publication.Publication
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * A connection to a web view.
 */

internal class WebViewConnection private constructor(
    private val jsExecutor: JavaScriptExecutor,
    private val webView: WebView,
    private val requestQueue: ExecutorService,
    private val uiExecutor: UIExecutor,
    private val resourceProvider: WebResourceProvider
) {

    fun openURL(
        location: String
    ): ListenableFuture<*> {
        val id = UUID.randomUUID()
        val future = SettableFuture.create<Unit>()

        /*
         * Execute a request to the web view on the UI thread, and wait for that request to
         * complete on the background thread we're using for web view connections. Because
         * the background thread executor is a single thread, this has the effect of serializing
         * and queueing requests made to the web view. We can effectively wait on the [Future]
         * that will be set by the web view on completion without having to block the UI thread
         * waiting for the request to complete.
         */

        this.requestQueue.execute {
            Timber.v("[$id]: openURL $location")
            this.uiExecutor.execute {
                this.webView.webViewClient = WebViewClient(location, future, resourceProvider)
                this.webView.loadUrl(location)
            }
            this.waitOrFail(id, future)
        }
        return future
    }

    /**
     * Wait up to a minute. If nothing is happening after a minute, something is seriously
     * broken and we should propagate an error via the [Future].
     */

    private fun <T> waitOrFail(
        id: UUID,
        future: SettableFuture<T>
    ) {
        try {
            Timber.v("[$id]: waiting for request to complete")
            future.get(1L, TimeUnit.MINUTES)
            Timber.v("[$id]: request completed")
        } catch (e: Exception) {
            Timber.e(e, "[$id]: timed out waiting for the web view to complete: ")
            future.setException(e)
        }
    }

    fun executeJS(
        function: (JavaScriptExecutor).() -> ListenableFuture<*>
    ): ListenableFuture<Any> {
        val id = UUID.randomUUID()
        val future = SettableFuture.create<Any>()

        /*
         * Execute a request to the web view on the UI thread, and wait for that request to
         * complete on the background thread we're using for web view connections. Because
         * the background thread executor is a single thread, this has the effect of serializing
         * and queueing requests made to the web view. We can effectively wait on the [Future]
         * that will be set by the web view on completion without having to block the UI thread
         * waiting for the request to complete.
         */

        this.requestQueue.execute {
            Timber.v("[$id] executeJS")

            this.uiExecutor.execute {
                val jsFuture = function.invoke(this.jsExecutor)
                jsFuture.addListener(
                    {
                        try {
                            future.set(jsFuture.get())
                        } catch (e: Throwable) {
                            future.setException(e)
                        }
                    },
                    MoreExecutors.directExecutor()
                )
            }

            this.waitOrFail(id, future)
        }
        return future
    }

    fun close() {
        this.webView.removeJavascriptInterface("Android")
        this.uiExecutor.dispose()
        this.requestQueue.shutdown()
    }

    companion object {

        @SuppressLint("ClickableViewAccessibility", "JavascriptInterface")
        fun create(
            webView: WebView,
            publication: Publication,
            jsExecutor: JavaScriptExecutor,
            jsReceiver: JavaScriptReceiver
        ): WebViewConnection {

            val threadFactory = ThreadFactory { runnable ->
                val thread = Thread(runnable)
                thread.name = "org.readium.navigator.html.WebViewConnection[${thread.id}]"
                return@ThreadFactory thread
            }

            val requestQueue = Executors.newFixedThreadPool(1, threadFactory)
            val webChromeClient = WebChromeClient()
            webView.webChromeClient = webChromeClient
            @SuppressLint("SetJavaScriptEnabled")
            webView.settings.javaScriptEnabled = true
            webView.isVerticalScrollBarEnabled = false
            webView.isHorizontalScrollBarEnabled = false

            /*
             * Disable manual scrolling on the web view. Scrolling is controlled via the javascript API.
             */

            webView.setOnTouchListener { v, event ->
                event.action == MotionEvent.ACTION_MOVE
            }

            webView.addJavascriptInterface(jsReceiver, "Android")

            val resourceProvider = WebResourceProvider(
                publication,
                HtmlInjector(publication)
            )

            return WebViewConnection(
                jsExecutor = jsExecutor,
                webView = webView,
                requestQueue = requestQueue,
                uiExecutor = UIExecutor(),
                resourceProvider = resourceProvider
            )
        }
    }
}
