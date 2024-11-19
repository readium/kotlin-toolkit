/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.readium.r2.navigator.extensions.optRectF
import org.readium.r2.navigator.input.InputModifier
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.decodeString
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
internal open class R2BasicWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {

    interface Listener {
        val readingProgression: ReadingProgression

        /** Called when the resource content is loaded in the web view. */
        fun onResourceLoaded(webView: R2BasicWebView, link: Link) {}

        /** Called when the target page of the resource is loaded in the web view. */
        fun onPageLoaded(webView: R2BasicWebView, link: Link) {}
        fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {}
        fun onPageEnded(end: Boolean) {}
        fun onTap(point: PointF): Boolean = false
        fun onDragStart(event: DragEvent): Boolean = false
        fun onDragMove(event: DragEvent): Boolean = false
        fun onDragEnd(event: DragEvent): Boolean = false
        fun onKey(event: KeyEvent): Boolean = false
        fun onDecorationActivated(id: DecorationId, group: String, rect: RectF, point: PointF): Boolean = false
        fun onProgressionChanged() {}
        fun goForward(animated: Boolean = false): Boolean = false
        fun goBackward(animated: Boolean = false): Boolean = false

        /**
         * Returns the custom [ActionMode.Callback] to be used with the text selection menu.
         */
        val selectionActionModeCallback: ActionMode.Callback? get() = null

        @InternalReadiumApi
        fun javascriptInterfacesForResource(link: Link): Map<String, Any?> = emptyMap()

        @InternalReadiumApi
        fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean = false

        @InternalReadiumApi
        fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? = null

        @InternalReadiumApi
        fun onFootnoteLinkActivated(url: AbsoluteUrl, context: HyperlinkNavigator.FootnoteContext)

        @InternalReadiumApi
        fun resourceAtUrl(url: Url): Resource? = null

        /**
         * Requests to load the next resource in the reading order.
         *
         * @param jump Indicates whether it's a discontinuous jump from the current locator. Used
         * for scroll mode.
         */
        @InternalReadiumApi
        fun goToNextResource(jump: Boolean, animated: Boolean): Boolean = false

        @InternalReadiumApi
        fun goToPreviousResource(jump: Boolean, animated: Boolean): Boolean = false
    }

    var listener: Listener? = null

    var resourceUrl: AbsoluteUrl? = null

    internal val scrollModeFlow = MutableStateFlow(false)

    /** Indicates that a user text selection is active. */
    internal var isSelecting = false

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

            val isRtl = (listener?.readingProgression == ReadingProgression.RTL)

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
        // Workaround addressing a bug in the Android WebView where the viewport is scrolled while
        // dragging the text selection handles.
        // See https://github.com/readium/kotlin-toolkit/issues/325
        if (isSelecting) {
            return
        }

        if (callback != null) {
            callback?.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        listener?.onProgressionChanged()
    }

    override fun destroy() {
        // Prevent the web view from leaking when detached
        // See https://github.com/readium/r2-navigator-kotlin/issues/52
        removeJavascriptInterface("Android")

        super.destroy()
    }

    open fun scrollRight(animated: Boolean = false) {
        uiScope.launch {
            val listener = listener ?: return@launch

            fun goRight(jump: Boolean) {
                if (listener.readingProgression == ReadingProgression.RTL) {
                    listener.goBackward(animated = animated) // Legacy
                    listener.goToPreviousResource(jump = jump, animated = animated)
                } else {
                    listener.goForward(animated = animated) // Legacy
                    listener.goToNextResource(jump = jump, animated = animated)
                }
            }

            when {
                scrollMode ->
                    goRight(jump = true)

                !this@R2BasicWebView.canScrollHorizontally(1) ->
                    goRight(jump = false)

                else ->
                    runJavaScript("readium.scrollRight();") { success ->
                        if (!success.toBoolean()) {
                            goRight(jump = false)
                        }
                    }
            }
        }
    }

    open fun scrollLeft(animated: Boolean = false) {
        uiScope.launch {
            val listener = listener ?: return@launch

            fun goLeft(jump: Boolean) {
                if (listener.readingProgression == ReadingProgression.RTL) {
                    listener.goForward(animated = animated) // legacy
                    listener.goToNextResource(jump = jump, animated = animated)
                } else {
                    listener.goBackward(animated = animated) // legacy
                    listener.goToPreviousResource(jump = jump, animated = animated)
                }
            }

            when {
                scrollMode ->
                    goLeft(jump = true)

                !this@R2BasicWebView.canScrollHorizontally(-1) ->
                    goLeft(jump = false)

                else ->
                    runJavaScript("readium.scrollLeft();") { success ->
                        if (!success.toBoolean()) {
                            goLeft(jump = false)
                        }
                    }
            }
        }
    }

    /*
     * Returns whether the web view should prevent the default behavior for this tap.
     */
    @android.webkit.JavascriptInterface
    fun onTap(eventJson: String): Boolean {
        // If there's an on-going selection, the tap will dismiss it so we don't forward it.
        if (isSelecting) {
            return false
        }

        val event = TapEvent.fromJSON(eventJson) ?: return false

        // The script prevented the default behavior.
        if (event.defaultPrevented) {
            return false
        }

        // We ignore taps on interactive element, unless it's an element we handle ourselves such as
        // pop-up footnotes.
        if (event.interactiveElement != null) {
            return handleFootnote(event.interactiveElement)
        }

        return runBlocking(uiScope.coroutineContext) { listener?.onTap(event.point) ?: false }
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

        return listener?.onDecorationActivated(id, group, rect, click.point) ?: false
    }

    /** Produced by gestures.js */
    private data class TapEvent(
        val defaultPrevented: Boolean,
        val point: PointF,
        val targetElement: String,
        val interactiveElement: String?,
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
            ?.let { Url(it) }
            ?: return false

        val id = href.fragment ?: return false

        val absoluteUrl = resourceUrl.resolve(href)

        val absoluteUrlWithoutFragment = absoluteUrl.removeFragment()

        val aside = runBlocking {
            tryOrLog {
                listener?.resourceAtUrl(absoluteUrlWithoutFragment)
                    ?.use { res ->
                        res.read()
                            .flatMap { it.decodeString() }
                            .map { Jsoup.parse(it) }
                            .getOrNull()
                    }
                    ?.select("#$id")
                    ?.first()?.html()
            }
        }?.takeIf { it.isNotBlank() }
            ?: return false

        val safe = Jsoup.clean(aside, Safelist.relaxed())
        val context = HyperlinkNavigator.FootnoteContext(
            noteContent = safe
        )

        listener?.onFootnoteLinkActivated(absoluteUrl, context)

        // Consume the event to prevent the Webview from loading the link.
        return true
    }

    @android.webkit.JavascriptInterface
    fun onDragStart(eventJson: String): Boolean {
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

        return runBlocking(uiScope.coroutineContext) { listener?.onDragStart(event) ?: false }
    }

    @android.webkit.JavascriptInterface
    fun onDragMove(eventJson: String): Boolean {
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

        return runBlocking(uiScope.coroutineContext) { listener?.onDragMove(event) ?: false }
    }

    @android.webkit.JavascriptInterface
    fun onDragEnd(eventJson: String): Boolean {
        val event = DragEvent.fromJSON(eventJson)?.takeIf { it.isValid }
            ?: return false

        return runBlocking(uiScope.coroutineContext) { listener?.onDragEnd(event) ?: false }
    }

    @android.webkit.JavascriptInterface
    fun onKey(eventJson: String): Boolean {
        val jsonObject = JSONObject(eventJson)
        val event = KeyEvent(
            type = when (jsonObject.optString("type")) {
                "down" -> KeyEvent.Type.Down
                "up" -> KeyEvent.Type.Up
                else -> return false
            },
            key = Key(jsonObject.optString("code")),
            modifiers = inputModifiers(jsonObject),
            characters = jsonObject.optNullableString("characters")?.takeUnless { it.isBlank() }
        )

        return listener?.onKey(event) ?: false
    }

    @android.webkit.JavascriptInterface
    fun onSelectionStart() {
        isSelecting = true
    }

    @android.webkit.JavascriptInterface
    fun onSelectionEnd() {
        isSelecting = false
    }

    /** Produced by gestures.js */
    data class DragEvent(
        val defaultPrevented: Boolean,
        val startPoint: PointF,
        val currentPoint: PointF,
        val offset: PointF,
        val interactiveElement: String?,
    ) {
        internal val isValid: Boolean get() =
            !defaultPrevented && (interactiveElement == null)

        companion object {
            fun fromJSONObject(obj: JSONObject?): DragEvent? {
                obj ?: return null

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

    suspend fun scrollToLocator(locator: Locator): Boolean {
        val json = locator.toJSON().toString()
        return runJavaScriptSuspend("readium.scrollToLocator($json);").toBoolean()
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

    internal suspend fun findFirstVisibleLocator(): Locator? =
        runJavaScriptSuspend("readium.findFirstVisibleLocator();")
            .let { tryOrNull { JSONObject(it) } }
            ?.let { Locator.fromJSON(it) }

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
            Timber.d("runJavaScript in $filename: $javascript")
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
        return listener?.shouldOverrideUrlLoading(this, request) ?: false
    }

    internal fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? {
        // Prevent favicon.ico to be loaded, this was causing a NullPointerException in NanoHttp
        if (!request.isForMainFrame && request.url.path?.endsWith("/favicon.ico") == true) {
            tryOrLog<Unit> {
                return WebResourceResponse("image/png", null, null)
            }
        }

        return listener?.shouldInterceptRequest(webView, request)
    }

    // Text selection ActionMode overrides
    //
    // Since Android 12, overriding Activity.onActionModeStarted doesn't seem to work to customize
    // the text selection menu. As an alternative, we can provide a custom ActionMode.Callback to be
    // used by the web view.

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        val customCallback = listener?.selectionActionModeCallback
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
        val customCallback = listener?.selectionActionModeCallback
            ?: return super.startActionMode(callback, type)

        val parent = parent ?: return null
        val wrapper = Callback2Wrapper(
            customCallback,
            callback2 = callback as? ActionMode.Callback2
        )
        return parent.startActionModeForChild(this, wrapper, type)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    inner class Callback2Wrapper(
        val callback: ActionMode.Callback,
        val callback2: ActionMode.Callback2?,
    ) : ActionMode.Callback by callback, ActionMode.Callback2() {
        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) =
            callback2?.onGetContentRect(mode, view, outRect)
                ?: super.onGetContentRect(mode, view, outRect)
    }
}

private fun inputModifiers(json: JSONObject): Set<InputModifier> =
    buildSet {
        if (json.optBoolean("alt")) {
            add(InputModifier.Alt)
        }
        if (json.optBoolean("control")) {
            add(InputModifier.Control)
        }
        if (json.optBoolean("shift")) {
            add(InputModifier.Shift)
        }
        if (json.optBoolean("meta")) {
            add(InputModifier.Meta)
        }
    }
