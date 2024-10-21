package org.readium.navigator.web.webapi

import android.graphics.PointF
import android.webkit.WebView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import timber.log.Timber

internal interface GesturesListener {

    fun onTap(point: PointF)
    fun onLinkActivated(href: AbsoluteUrl)
}

internal class GesturesApi(
    private val density: Density,
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
        val point = with(density) { PointF(tapEvent.x.dp.toPx(), tapEvent.y.dp.toPx()) }
        Timber.d("onTap ${point.x} ${point.y}")
        listener.onTap(point)
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
