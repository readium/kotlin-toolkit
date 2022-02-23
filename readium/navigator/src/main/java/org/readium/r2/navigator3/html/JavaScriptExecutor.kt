package org.readium.r2.navigator3.html

import android.webkit.WebView
import androidx.annotation.UiThread
import com.google.common.annotations.Beta
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import timber.log.Timber

internal class JavaScriptExecutor(
    private val webView: WebView,
) {
    @UiThread
    private fun executeJavascript(
        script: String
    ): ListenableFuture<String> {

        val future = SettableFuture.create<String>()
        Timber.v("evaluating $script")
        this.webView.evaluateJavascript(script) {
            try {
                Timber.v("evaluated $script ⇒ $it")
            } finally {
                future.set(it)
            }
        }
        return future
    }

    @UiThread
   fun scrollRight(): ListenableFuture<Boolean> {
        val future = this.executeJavascript("readium.scrollRight();")
        return Futures.transform(
            future,
            {
                when (future.get()) {
                    "false" -> false
                    else -> true
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    @UiThread
    fun scrollLeft(): ListenableFuture<Boolean> {
        val future = this.executeJavascript("readium.scrollLeft();")
        return Futures.transform(
            future,
            {
                when (future.get()) {
                    "false" -> false
                    else -> true
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    @UiThread
    fun scrollToEnd(): ListenableFuture<String> {
        return this.executeJavascript("readium.scrollToEnd();")
    }

    @UiThread
    fun setFontFamily(value: String): ListenableFuture<*> {
        return Futures.allAsList(
            this.setUserProperty("fontFamily", value),
            this.setUserProperty("fontOverride", "readium-font-on")
        )
    }

    @UiThread
    fun setFontSize(value: Double): ListenableFuture<String> {
        val percent = (value * 100.0).toString() + "%"
        return this.setUserProperty("fontSize", percent)
    }

    fun setTheme(value: Theme): ListenableFuture<String> =
        when (value) {
            Theme.DEFAULT ->
                this.setUserProperty("appearance", "readium-default-on")
            Theme.NIGHT ->
                this.setUserProperty("appearance", "readium-night-on")
            Theme.SEPIA ->
                this.setUserProperty("appearance", "readium-sepia-on")
        }

    @UiThread
    fun scrollToPosition(progress: Double): ListenableFuture<String> {
        return this.executeJavascript("readium.scrollToPosition($progress);")
    }

    @UiThread
    fun broadcastReadingPosition(): ListenableFuture<*> {
        return this.executeJavascript("readium.broadcastReadingPosition();")
    }

    @UiThread
    fun setScrollMode(mode: ScrollingMode): ListenableFuture<*> {
        return this.setUserProperty(
            name = "scroll",
            value = when (mode) {
                ScrollingMode.SCROLLING_MODE_PAGINATED -> "readium-scroll-off"
                ScrollingMode.SCROLLING_MODE_CONTINUOUS -> "readium-scroll-on"
            }
        )
    }

    @UiThread
    fun scrollToId(id: String): ListenableFuture<*> {
        return this.executeJavascript("readium.scrollToId(\"$id\");")
    }

    @UiThread
    fun setPublisherCSS(
        css: PublisherCSS
    ): ListenableFuture<*> {
        return when (css) {
            PublisherCSS.PUBLISHER_DEFAULT_CSS_ENABLED ->
                Futures.allAsList(
                    this.setUserProperty("advancedSettings", ""),
                    this.setUserProperty("fontOverride", "")
                )
            PublisherCSS.PUBLISHER_DEFAULT_CSS_DISABLED ->
                Futures.allAsList(
                    this.setUserProperty("advancedSettings", "readium-advanced-on"),
                    this.setUserProperty("fontOverride", "readium-font-on")
                )
        }
    }

    @UiThread
    private fun setUserProperty(
        name: String,
        value: String
    ): ListenableFuture<String> {
        return this.executeJavascript("readium.setProperty(\"--USER__${name}\", \"${value}\");")
    }
}

internal enum class Theme {
    DEFAULT,
    NIGHT,
    SEPIA;
}

/**
 * A specification of the scrolling mode.
 */

internal enum class ScrollingMode {

    /**
     * Paginated scrolling mode; book chapters are presented as a series of discrete pages.
     */

    SCROLLING_MODE_PAGINATED,

    /**
     * Continuous scrolling mode; book chapters are presented as a single scrollable region of text.
     */

    SCROLLING_MODE_CONTINUOUS
}

/**
 * A specification of whether or not the publisher default CSS is enabled.
 */

internal enum class PublisherCSS {

    /**
     * Publisher default CSS is enabled.
     */

    PUBLISHER_DEFAULT_CSS_ENABLED,

    /**
     * Publisher default CSS is disabled.
     */

    PUBLISHER_DEFAULT_CSS_DISABLED
}
