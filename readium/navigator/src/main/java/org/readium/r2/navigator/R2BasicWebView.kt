/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.view.*
import android.view.Gravity
import android.view.LayoutInflater
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.readium.r2.navigator.extensions.optRectF
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Href
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@OptIn(ExperimentalDecorator::class)
open class R2BasicWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {

    interface Listener {
        val readingProgression: ReadingProgression
        fun onResourceLoaded(link: Link?, webView: R2BasicWebView, url: String?) {}
        fun onPageLoaded()
        fun onPageChanged(pageIndex: Int, totalPages: Int, url: String)
        fun onPageEnded(end: Boolean)
        fun onScroll()
        fun onTap(point: PointF): Boolean
        fun onDragStart(event: DragEvent): Boolean
        fun onDragMove(event: DragEvent): Boolean
        fun onDragEnd(event: DragEvent): Boolean
        fun onDecorationActivated(id: DecorationId, group: String, rect: RectF, point: PointF): Boolean = false
        fun onProgressionChanged()
        fun onHighlightActivated(id: String)
        fun onHighlightAnnotationMarkActivated(id: String)
        fun goForward(animated: Boolean = false, completion: () -> Unit = {}): Boolean
        fun goBackward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

        /**
         * Returns the custom [ActionMode.Callback] to be used with the text selection menu.
         */
        val selectionActionModeCallback: ActionMode.Callback? get() = null

        /**
         * Offers an opportunity to override a request loaded by the given web view.
         */
        fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean = false
    }

    lateinit var listener: Listener
    internal var preferences: SharedPreferences? = null

    var resourceUrl: String? = null

    internal val scrollModeFlow = MutableStateFlow(false)

    val scrollMode: Boolean get() = scrollModeFlow.value

    var callback: OnOverScrolledCallback? = null

    private val uiScope = CoroutineScope(Dispatchers.Main)

    init {
        setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    /** Computes the current progression in the resource. */
    val progression: Double get() =
        if (scrollMode) {
            val y = scrollY.toDouble()
            val contentHeight = computeVerticalScrollRange()

            var progression = 0.0
            if (contentHeight > 0) {
                progression = (y / contentHeight).coerceIn(0.0, 1.0)
            }

            progression

        } else {
            var x = scrollX.toDouble()
            val pageWidth = computeHorizontalScrollExtent()
            val contentWidth = computeHorizontalScrollRange()

            val isRtl = (listener.readingProgression == ReadingProgression.RTL)

            // For RTL, we need to add the equivalent of one page to the x position, otherwise the
            // progression will be one page off.
            if (isRtl) {
                x += pageWidth
            }

            var progression = 0.0
            if (contentWidth > 0) {
                progression = (x / contentWidth).coerceIn(0.0, 1.0)
            }
            // For RTL, we need to reverse the progression because the web view is always scrolling
            // from left to right, no matter the reading direction.
            if (isRtl) {
                progression = 1 - progression
            }

            progression
        }

    interface OnOverScrolledCallback {
        fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean)
    }

    fun setOnOverScrolledCallback(callback: OnOverScrolledCallback) {
        this.callback = callback
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        if (callback != null) {
            callback?.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        listener.onProgressionChanged()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addJavascriptInterface(this, "Android")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Prevent the web view from leaking when detached
        // See https://github.com/readium/r2-navigator-kotlin/issues/52
        removeJavascriptInterface("Android")
    }

    @android.webkit.JavascriptInterface
    open fun scrollRight(animated: Boolean = false) {
        uiScope.launch {
            listener.onScroll()

            fun goRight() {
                if (listener.readingProgression == ReadingProgression.RTL) {
                    listener.goBackward(animated = animated)
                } else {
                    listener.goForward(animated = animated)
                }
            }

            if (scrollMode || !this@R2BasicWebView.canScrollHorizontally(1)) {
                goRight()
            } else {
                runJavaScript("readium.scrollRight();") { success ->
                    if (!success.toBoolean()) {
                        goRight()
                    }
                }
            }
        }
    }

    @android.webkit.JavascriptInterface
    open fun scrollLeft(animated: Boolean = false) {
        uiScope.launch {
            listener.onScroll()

            fun goLeft() {
                if (listener.readingProgression == ReadingProgression.RTL) {
                    listener.goForward(animated = animated)
                } else {
                    listener.goBackward(animated = animated)
                }
            }

            if (scrollMode || !this@R2BasicWebView.canScrollHorizontally(-1)) {
                goLeft()
            } else {
                runJavaScript("readium.scrollLeft();") { success ->
                    if (!success.toBoolean()) {
                        goLeft()
                    }
                }
            }
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

        // The script prevented the default behavior.
        if (event.defaultPrevented) {
            return false
        }

        // FIXME: Let the app handle edge taps and footnotes.

        // We ignore taps on interactive element, unless it's an element we handle ourselves such as
        // pop-up footnotes.
        if (event.interactiveElement != null) {
            return handleFootnote(event.targetElement)
        }

        // Skips to previous/next pages if the tap is on the content edges.
        val clientWidth = computeHorizontalScrollExtent()
        val thresholdRange = 0.0..(0.2 * clientWidth)

        // FIXME: Call listener.onTap if scrollLeft|Right fails
        return when {
            thresholdRange.contains(event.point.x) -> {
                scrollLeft(false)
                true
            }
            thresholdRange.contains(clientWidth - event.point.x) -> {
                scrollRight(false)
                true
            }
            else ->
                runBlocking(uiScope.coroutineContext) { listener.onTap(event.point) }
        }
    }

    /**
     * Called from the JS code when a tap on a decoration is detected.
     */
    @android.webkit.JavascriptInterface
    fun onDecorationActivated(eventJson: String): Boolean {
        val obj = tryOrLog { JSONObject(eventJson) }
        val id = obj?.optNullableString("id")
        val group = obj?.optNullableString("group")
        val rect = obj?.optRectF("rect")
        val click = TapEvent.fromJSONObject(obj?.optJSONObject("click"))
        if (id == null || group == null || rect == null || click == null) {
            Timber.e("Invalid JSON for onDecorationActivated: $eventJson")
            return false
        }

        return listener.onDecorationActivated(id, group, rect, click.point)
    }

    /** Produced by gestures.js */
    private data class TapEvent(
        val defaultPrevented: Boolean,
        val point: PointF,
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
                    point = PointF(x, y),
                    targetElement = obj.optString("targetElement"),
                    interactiveElement = obj.optNullableString("interactiveElement")
                )
            }

            fun fromJSON(json: String): TapEvent? =
                fromJSONObject(tryOrNull { JSONObject(json) })
        }
    }

    private fun handleFootnote(html: String): Boolean {
        val resourceUrl = resourceUrl ?: return false

        val href = tryOrNull { Jsoup.parse(html) }
            ?.select("a[epub:type=noteref]")?.first()
            ?.attr("href")
            ?: return false

        val id = href.substringAfter("#", missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
            ?: return false

        val absoluteUrl = Href(href, baseHref = resourceUrl).percentEncodedString
            .substringBefore("#")

        val aside = tryOrNull { Jsoup.connect(absoluteUrl).get() }
            ?.select("#$id")
            ?.first()?.html()
            ?: return false

        val safe = Jsoup.clean(aside, Whitelist.relaxed())

        // Initialize a new instance of LayoutInflater service
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate the custom layout/view
        val customView = inflater.inflate(R.layout.popup_footnote, null)

        // Initialize a new instance of popup window
        val mPopupWindow = PopupWindow(
            customView,
            ListPopupWindow.WRAP_CONTENT,
            ListPopupWindow.WRAP_CONTENT
        )
        mPopupWindow.isOutsideTouchable = true
        mPopupWindow.isFocusable = true

        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPopupWindow.elevation = 5.0f
        }

        val textView = customView.findViewById(R.id.footnote) as TextView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.text = Html.fromHtml(safe, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            textView.text = Html.fromHtml(safe)
        }

        // Get a reference for the custom view close button
        val closeButton = customView.findViewById(R.id.ib_close) as ImageButton

        // Set a click listener for the popup window close button
        closeButton.setOnClickListener {
            // Dismiss the popup window
            mPopupWindow.dismiss()
        }

        // Finally, show the popup window at the center location of root relative layout
        mPopupWindow.showAtLocation(this, Gravity.CENTER, 0, 0)

        return true
    }

    @android.webkit.JavascriptInterface
    fun onDragStart(eventJson: String): Boolean {
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

        return runBlocking(uiScope.coroutineContext) { listener.onDragStart(event) }
    }

    @android.webkit.JavascriptInterface
    fun onDragMove(eventJson: String): Boolean {
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

        return runBlocking(uiScope.coroutineContext) { listener.onDragMove(event) }
    }

    @android.webkit.JavascriptInterface
    fun onDragEnd(eventJson: String): Boolean {
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

        return runBlocking(uiScope.coroutineContext) { listener.onDragEnd(event) }
    }

    /** Produced by gestures.js */
    data class DragEvent(
        val defaultPrevented: Boolean,
        val startPoint: PointF,
        val currentPoint: PointF,
        val offset: PointF,
        val interactiveElement: String?
    ) {
        internal val isValid: Boolean get() =
            !defaultPrevented && (interactiveElement == null)

        companion object {
            fun fromJSONObject(obj: JSONObject?): DragEvent? {
                obj ?: return null

                val x = obj.optDouble("x").toFloat()
                val y = obj.optDouble("y").toFloat()

                return DragEvent(
                    defaultPrevented = obj.optBoolean("defaultPrevented"),
                    startPoint = PointF(
                        obj.optDouble("startX").toFloat(),
                        obj.optDouble("startY").toFloat()
                    ),
                    currentPoint = PointF(
                        obj.optDouble("currentX").toFloat(),
                        obj.optDouble("currentY").toFloat()
                    ),
                    offset = PointF(
                        obj.optDouble("offsetX").toFloat(),
                        obj.optDouble("offsetY").toFloat()
                    ),
                    interactiveElement = obj.optNullableString("interactiveElement")
                )
            }

            fun fromJSON(json: String): DragEvent? =
                fromJSONObject(tryOrNull { JSONObject(json) })
        }
    }

    @android.webkit.JavascriptInterface
    fun getViewportWidth(): Int = width

    @android.webkit.JavascriptInterface
    fun logError(message: String, filename: String, line: Int) {
        Timber.e("JavaScript error: $filename:$line $message")
    }

    @android.webkit.JavascriptInterface
    fun log(message: String) {
        Timber.d("JavaScript: $message")
    }

    @android.webkit.JavascriptInterface
    fun highlightActivated(id: String) {
        uiScope.launch {
            listener.onHighlightActivated(id)
        }
    }

    @android.webkit.JavascriptInterface
    fun highlightAnnotationMarkActivated(id: String) {
        uiScope.launch {
            listener.onHighlightAnnotationMarkActivated(id)
        }
    }


    fun Boolean.toInt() = if (this) 1 else 0

    fun scrollToStart() {
        runJavaScript("readium.scrollToStart();")
    }

    fun scrollToEnd() {
        runJavaScript("readium.scrollToEnd();")
    }

    suspend fun scrollToId(htmlId: String): Boolean =
        runJavaScriptSuspend("readium.scrollToId(\"$htmlId\");").toBoolean()

    fun scrollToPosition(progression: Double) {
        runJavaScript("readium.scrollToPosition(\"$progression\");")
    }

    suspend fun scrollToText(text: Locator.Text): Boolean {
        val json = text.toJSON().toString()
        return runJavaScriptSuspend("readium.scrollToText($json);").toBoolean()
    }

    fun setScrollMode(scrollMode: Boolean) {
        runJavaScript("setScrollMode($scrollMode)")
        scrollModeFlow.value = scrollMode
    }

    fun setProperty(key: String, value: String) {
        runJavaScript("readium.setProperty(\"$key\", \"$value\");")
    }

    fun removeProperty(key: String) {
        runJavaScript("readium.removeProperty(\"$key\");")
    }

    fun getCurrentSelectionInfo(callback: (String) -> Unit) {
        runJavaScript("getCurrentSelectionInfo();", callback)
    }

    fun getCurrentSelectionRect(callback: (String) -> Unit) {
        runJavaScript("getSelectionRect();", callback)
    }

    fun createHighlight(locator: String?, color: String?, callback: (String) -> Unit) {
        uiScope.launch {
            runJavaScript("createHighlight($locator, $color, true);", callback)
        }
    }

    fun destroyHighlight(id: String) {
        uiScope.launch {
            runJavaScript("destroyHighlight(\"$id\");")
        }
    }

    fun createAnnotation(id: String) {
        uiScope.launch {
            runJavaScript("createAnnotation(\"$id\");")
        }
    }

    fun rectangleForHighlightWithID(id: String, callback: (String) -> Unit) {
        uiScope.launch {
            runJavaScript("rectangleForHighlightWithID(\"$id\");", callback)
        }
    }

    fun runJavaScript(javascript: String, callback: ((String) -> Unit)? = null) {
        if (BuildConfig.DEBUG) {
            val filename = URLUtil.guessFileName(url, null, null)
            Timber.d("runJavaScript in ${filename}: $javascript")
        }

        this.evaluateJavascript(javascript) { result ->
            if (callback != null) callback(result)
        }
    }

    internal suspend fun runJavaScriptSuspend(javascript: String): String = suspendCoroutine { cont ->
        runJavaScript(javascript) { result ->
            cont.resume(result)
        }
    }

    internal fun shouldOverrideUrlLoading(request: WebResourceRequest): Boolean {
        if (resourceUrl == request.url?.toString()) return false

        return listener.shouldOverrideUrlLoading(this, request)
    }

    // Text selection ActionMode overrides
    //
    // Since Android 12, overriding Activity.onActionModeStarted doesn't seem to work to customize
    // the text selection menu. As an alternative, we can provide a custom ActionMode.Callback to be
    // used by the web view.

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        val customCallback = listener.selectionActionModeCallback
            ?: return super.startActionMode(callback)

        val parent = parent ?: return null
        return parent.startActionModeForChild(this, customCallback)
    }

    /**
     * A wrapper for the app-provided custom [ActionMode.Callback] which clears the selection when
     * activating one of the menu items.
     */
    inner class CallbackWrapper(val callback: ActionMode.Callback) : ActionMode.Callback by callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            uiScope.launch { clearFocus() }
            return callback.onActionItemClicked(mode, item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        val customCallback = listener.selectionActionModeCallback
            ?: return super.startActionMode(callback, type)

        val parent = parent ?: return null
        val wrapper = Callback2Wrapper(customCallback, callback2 = callback as? ActionMode.Callback2)
        return parent.startActionModeForChild(this, wrapper, type)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    inner class Callback2Wrapper(val callback: ActionMode.Callback, val callback2: ActionMode.Callback2?) : ActionMode.Callback by callback, ActionMode.Callback2() {
        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) =
            callback2?.onGetContentRect(mode, view, outRect)
                ?: super.onGetContentRect(mode, view, outRect)
    }
}
