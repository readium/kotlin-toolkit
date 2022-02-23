package org.readium.r2.navigator3.html

import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.google.common.util.concurrent.SettableFuture
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

internal class WebViewClient(
    private val requestLocation: String,
    private val future: SettableFuture<Unit>,
    private val resourceProvider: WebResourceProvider
) : android.webkit.WebViewClient() {

    private val errors =
        ConcurrentHashMap<String, String>()

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return resourceProvider.processRequest(view.context, request)
            ?: super.shouldInterceptRequest(view, request)
    }

    override fun onLoadResource(
        view: WebView,
        url: String
    ) {
        Timber.v("onLoadResource: $url")
        super.onLoadResource(view, url)
    }

    override fun onPageFinished(
        view: WebView,
        url: String
    ) {
        Timber.v("onPageFinished: $url")

        if (this.requestLocation == url) {
            try {
                if (this.errors.isEmpty()) {
                    Timber.v("onPageFinished: $url succeeded")
                    this.future.set(Unit)
                    return
                } else {
                    Timber.e("onPageFinished: $url failed with ${errors.size} errors")
                    this.future.setException(
                        WebViewLoadException("Failed to load $requestLocation", this.errors.toMap())
                    )
                    return
                }
            } finally {
                Timber.v("onPageFinished: completed future")
            }
        }

        super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (Build.VERSION.SDK_INT >= 23) {
            Timber.e("onReceivedError: ${request.url}: ${error.errorCode} ${error.description}")
            this.errors[request.url.toString()] = "${error.errorCode} ${error.description}"
        }

        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        Timber.e("onReceivedHttpError: ${request.url}: ${errorResponse.statusCode} ${errorResponse.reasonPhrase}")

        this.errors[request.url.toString()] = "${errorResponse.statusCode} ${errorResponse.reasonPhrase}"
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        Timber.e("onReceivedError: $failingUrl: $errorCode $description")

        this.errors[failingUrl] = "$errorCode $description"
        super.onReceivedError(view, errorCode, description, failingUrl)
    }
}
