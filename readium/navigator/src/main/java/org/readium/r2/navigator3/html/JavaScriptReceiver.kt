package org.readium.r2.navigator3.html

import android.webkit.WebView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.tryOrNull
import timber.log.Timber

internal class JavaScriptReceiver(
    private val viewportSize: IntSize,
    private val onTap: ((Offset) -> Unit)?,
    private val onDoubleTap: ((Offset) -> Unit)?
){
    @android.webkit.JavascriptInterface
    fun getViewportWidth(): Int {
        Timber.d("getViewportWidth ${viewportSize.width}")
        return viewportSize.width
    }

    /** Produced by gestures.js */
    private data class TapEvent(
        val defaultPrevented: Boolean,
        val point: Offset,
        val targetElement: String,
        val interactiveElement: String?
    ) {
        companion object {
            fun fromJSONObject(obj: JSONObject?): TapEvent? {
                obj ?: return null

                val x = obj.optDouble("x").toFloat()
                val y = obj.optDouble("y").toFloat()

                return TapEvent(
                    defaultPrevented = obj.optBoolean("defaultPrevented"),
                    point = Offset(x, y),
                    targetElement = obj.optString("targetElement"),
                    interactiveElement = obj.optNullableString("interactiveElement")
                )
            }

            fun fromJSON(json: String): TapEvent? =
                fromJSONObject(tryOrNull { JSONObject(json) })
        }
    }
    /**
     * Called from the JS code when a tap is detected.
     * If the JS indicates the tap is being handled within the web view, don't take action,
     *
     * Returns whether the web view should prevent the default behavior for this tap.
     */
    @android.webkit.JavascriptInterface
    fun onTap(eventJson: String): Boolean {
        val event = TapEvent.fromJSON(eventJson) ?: return false
        onTap?.invoke(event.point)
        return true
    }
}