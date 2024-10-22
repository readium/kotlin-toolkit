package org.readium.navigator.web.webapi

import android.webkit.WebView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import timber.log.Timber

internal interface GesturesListener {

    fun onTap(offset: DpOffset)
    fun onLinkActivated(href: AbsoluteUrl)
}

internal class GesturesApi(
    private val listener: GesturesListener
) {

    companion object {

        val path: RelativeUrl =
            RelativeUrl("readium/navigators/web/fixed-injectable-script.js")!!
    }

    fun registerOnWebView(webView: WebView) {
        webView.addJavascriptInterface(this, "gestures")
    }

    @android.webkit.JavascriptInterface
    fun onTap(eventJson: String) {
        Timber.d("onTap start $eventJson")
        val tapEvent = Json.decodeFromString<JsonTapEvent>(eventJson)
        listener.onTap(DpOffset(tapEvent.x.dp, tapEvent.y.dp))
    }

    @android.webkit.JavascriptInterface
    fun onLinkActivated(href: String) {
        val url = AbsoluteUrl(href) ?: return
        listener.onLinkActivated(url)
    }
}

@Serializable
private data class JsonTapEvent(
    val x: Float,
    val y: Float
)
