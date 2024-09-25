package org.readium.navigator.web.webapi

import android.graphics.PointF
import android.webkit.WebView
import androidx.compose.ui.unit.Density
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.r2.shared.util.RelativeUrl

internal interface GesturesListener {

    fun onTap(point: PointF)
}

internal class GesturesApi(
    private val density: Density,
    private val listener: GesturesListener
) {

    companion object {

        val path: RelativeUrl =
            RelativeUrl("readium/navigators/web/prepaginated-injectable-script.js")!!
    }

    fun registerOnWebView(webView: WebView) {
        webView.addJavascriptInterface(this, "gestures")
    }

    @android.webkit.JavascriptInterface
    fun onTap(eventJson: String) {
        val tapEvent = Json.decodeFromString<JsonTapEvent>(eventJson)
        val point = with(density) { PointF(tapEvent.x.toDp().value, tapEvent.y.toDp().value) }
        listener.onTap(point)
    }
}

@Serializable
private data class JsonTapEvent(
    val x: Float,
    val y: Float
)
