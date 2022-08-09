/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.app.Application
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.PatternMatcher
import android.os.PatternMatcher.PATTERN_SIMPLE_GLOB
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.epub.extensions.javascriptForGroup
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.fetcher.fallback
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.http.HttpHeaders
import org.readium.r2.shared.util.http.HttpRange
import org.readium.r2.shared.util.mediatype.MediaType
import kotlin.reflect.KClass

@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class)
internal class EpubNavigatorViewModel(
    application: Application,
    val publication: Publication,
    baseUrl: String?,
    val config: EpubNavigatorFragment.Configuration,
) : AndroidViewModel(application) {

    private val baseUrl: String =
        baseUrl?.let { it.removeSuffix("/") + "/" }
            ?: publication.linkWithRel("self")?.href
            ?: "https://readium/publication/"

    val preferences = application.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

    // Make a copy to prevent new decoration templates from being registered after initializing
    // the navigator.
    private val decorationTemplates: HtmlDecorationTemplates = config.decorationTemplates.copy()

    data class RunScriptCommand(val script: String, val scope: Scope) {
        sealed class Scope {
            object CurrentResource : Scope()
            object LoadedResources : Scope()
            data class Resource(val href: String) : Scope()
            data class WebView(val webView: R2BasicWebView) : Scope()
        }
    }

    sealed class Event {
        data class GoTo(val target: Link) : Event()
        data class OpenExternalLink(val url: Uri) : Event()
        data class RunScript(val command: RunScriptCommand) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> get() = _events.receiveAsFlow()

    private val _settings = MutableStateFlow<EpubSettings>(
        when (publication.metadata.presentation.layout) {
            EpubLayout.FIXED -> EpubSettings.FixedLayout()
            EpubLayout.REFLOWABLE, null -> EpubSettings.Reflowable(fontFamilies = config.fontFamilies.map { it.fontFamily })
        }
            .update(
                metadata = publication.metadata,
                preferences = config.preferences,
                defaults = config.defaultPreferences
            )
    )
    val settings: StateFlow<EpubSettings> = _settings.asStateFlow()

    private val assetsBaseHref = "https://readium/assets/"

    private val css = MutableStateFlow(
        ReadiumCss(
            rsProperties = config.readiumCssRsProperties,
            fontFamilies = config.fontFamilies,
            assetsBaseHref = assetsBaseHref
        ).update(settings.value)
    )

    init {
        initReadiumCss()
    }

    /**
     * Requests the web views to be updated when the Readium CSS properties change.
     */
    private fun initReadiumCss() {
        var previousCss = css.value
        css
            .onEach { css ->
                val properties = mutableMapOf<String, String?>()
                if (previousCss.rsProperties != css.rsProperties) {
                    properties += css.rsProperties.toCssProperties()
                }
                if (previousCss.userProperties != css.userProperties) {
                    properties += css.userProperties.toCssProperties()
                }
                if (properties.isNotEmpty()) {
                    _events.send(Event.RunScript(
                        RunScriptCommand(
                            script = "readium.setCSSProperties(${JSONObject(properties.toMap())});",
                            scope = RunScriptCommand.Scope.LoadedResources
                        )
                    ))
                }

                previousCss = css
            }
            .launchIn(viewModelScope)
    }

    fun onResourceLoaded(link: Link?, webView: R2BasicWebView): RunScriptCommand {
        val templates = decorationTemplates.toJSON().toString()
            .replace("\\n", " ")
        var script = "readium.registerDecorationTemplates($templates);\n"

        if (link != null) {
            for ((group, decorations) in decorations) {
                val changes = decorations
                    .filter { it.locator.href == link.href }
                    .map { DecorationChange.Added(it) }

                val groupScript = changes.javascriptForGroup(group, decorationTemplates) ?: continue
                script += "$groupScript\n"
            }
        }

        return RunScriptCommand(script, scope = RunScriptCommand.Scope.WebView(webView))
    }

    // Server

    /**
     * Generates the URL to the given publication link.
     */
    fun urlTo(link: Link): String =
        with(link) {
            // Already an absolute URL?
            if (Uri.parse(href).scheme != null) {
                href
            } else {
                Href(
                    href = href.removePrefix("/"),
                    baseHref = baseUrl
                ).percentEncodedString
            }
        }

    /**
     * Intercepts and handles web view navigation to [url].
     */
    fun navigateToUrl(url: Uri) = viewModelScope.launch {
        var href = url.toString()
        if (href.startsWith(baseUrl)) {
            href = href.removePrefix(baseUrl).addPrefix("/")
            val link = publication.linkWithHref(href)
                // Query parameters must be kept as they might be relevant for the fetcher.
                ?.copy(href = href)
                ?.let { _events.send(Event.GoTo(it)) }

        } else {
            _events.send(Event.OpenExternalLink(url))
        }
    }

    /**
     * Serves the requests of the navigator web views.
     *
     * https://readium/publication/ serves the publication resources through its fetcher.
     * https://readium/assets/ serves the application assets.
     */
    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        if (request.url.host != "readium") return null
        val path = request.url.path ?: return null

        return when {
            path.startsWith("/publication/") -> {
                servePublicationResource(
                    href = path.removePrefix("/publication"),
                    range = HttpHeaders(request.requestHeaders).range
                )
            }
            path.startsWith("/assets/") && isServedAsset(path.removePrefix("/assets/")) -> {
                assetsLoader.shouldInterceptRequest(request.url)
            }
            else -> null
        }
    }

    /**
     * Returns a new [Resource] to serve the given [href] in the publication.
     *
     * If the [Resource] is an HTML document, injects the required JavaScript and CSS files.
     */
    private fun servePublicationResource(href: String, range: HttpRange?): WebResourceResponse {
        val link = publication.linkWithHref(href)
            // Query parameters must be kept as they might be relevant for the fetcher.
            ?.copy(href = href)
            ?: Link(href = href)

        var resource = publication.get(link)
            .fallback(notFoundResource(link))
        if (link.mediaType.isHtml) {
            resource = resource.injectHtml(publication, css.value, baseHref = assetsBaseHref)
        }

        val headers = mutableMapOf(
            "Accept-Ranges" to "bytes",
        )

        if (range == null) {
            return WebResourceResponse(link.type, null, 200, "OK", headers, ResourceInputStream(resource))

        } else { // Byte range request
            val stream = ResourceInputStream(resource)
            val length = stream.available()
            val longRange = range.toLongRange(length.toLong())
            headers["Content-Range"] = "bytes ${longRange.first}-${longRange.last}/$length"
            // Content-Length will automatically be filled by the WebView using the Content-Range header.
//            headers["Content-Length"] = (longRange.last - longRange.first + 1).toString()
            return WebResourceResponse(link.type, null, 206, "Partial Content", headers, stream)
        }
    }

    private fun notFoundResource(link: Link): Resource =
        StringResource(link.copy(type = MediaType.XHTML.toString())) {
            withContext(Dispatchers.IO) {
                getApplication<Application>().assets
                    .open("readium/404.xhtml").bufferedReader()
                    .use { it.readText() }
                    .replace("\${href}", link.href)
            }
        }

    private fun isServedAsset(path: String): Boolean =
        servedAssetPatterns.any { it.match(path) }

    private val servedAssetPatterns: List<PatternMatcher> =
        config.servedAssets.map { PatternMatcher(it, PATTERN_SIMPLE_GLOB) }

    private val assetsLoader by lazy {
        WebViewAssetLoader.Builder()
            .setDomain("readium")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(application))
            .build()
    }

    // Settings

    fun applyPreferences(preferences: Preferences) {
        val settings = _settings.updateAndGet {
            it.update(
                metadata = publication.metadata,
                preferences = preferences,
                defaults = config.defaultPreferences
            )
        }

        css.update { it.update(settings) }
    }

    /**
     * Indicates whether the navigator is scrollable instead of paginated.
     */
    val isOverflowScrolled: Boolean get() =
        if (config.useLegacySettings) {
            preferences.getBoolean(SCROLL_REF, false)
        } else {
            (settings.value as? EpubSettings.Reflowable)?.overflow?.value == Presentation.Overflow.SCROLLED
        }

    // Selection

    fun clearSelection(): RunScriptCommand =
        RunScriptCommand(
            "window.getSelection().removeAllRanges();",
            scope = RunScriptCommand.Scope.CurrentResource
        )

    // Decorations

    /** Current decorations, indexed by the group name. */
    private val decorations: MutableMap<String, List<Decoration>> = mutableMapOf()

    fun <T : Decoration.Style> supportsDecorationStyle(style: KClass<T>): Boolean =
        decorationTemplates.styles.containsKey(style)

    suspend fun applyDecorations(decorations: List<Decoration>, group: String): List<RunScriptCommand> {
        val source = this.decorations[group] ?: emptyList()
        val target = decorations.toList()
        this.decorations[group] = target

        val cmds = mutableListOf<RunScriptCommand>()

        if (target.isEmpty()) {
            cmds.add(RunScriptCommand(
                // The updates command are using `requestAnimationFrame()`, so we need it for
                // `clear()` as well otherwise we might recreate a highlight after it has been
                // cleared.
                "requestAnimationFrame(function () { readium.getDecorations('$group').clear(); });",
                scope = RunScriptCommand.Scope.LoadedResources
            ))
        } else {
            for ((href, changes) in source.changesByHref(target)) {
                val script = changes.javascriptForGroup(group, decorationTemplates) ?: continue
                cmds.add(RunScriptCommand(script, scope = RunScriptCommand.Scope.Resource(href)))
            }
        }

        return cmds
    }

    /** Decoration group listeners, indexed by the group name. */
    private val decorationListeners: MutableMap<String, List<DecorableNavigator.Listener>> = mutableMapOf()

    fun addDecorationListener(group: String, listener: DecorableNavigator.Listener) {
        val listeners = decorationListeners[group] ?: emptyList()
        decorationListeners[group] = listeners + listener
    }

    fun removeDecorationListener(listener: DecorableNavigator.Listener) {
        for ((group, listeners) in decorationListeners) {
            decorationListeners[group] = listeners.filter { it != listener }
        }
    }

    fun onDecorationActivated(id: DecorationId, group: String, rect: RectF, point: PointF): Boolean {
        val listeners = decorationListeners[group]
            ?: return false

        val decoration = decorations[group]
            ?.firstOrNull { it.id == id }
            ?: return false

        val event = DecorableNavigator.OnActivatedEvent(
            decoration = decoration, group = group, rect = rect, point = point
        )
        for (listener in listeners) {
            if (listener.onDecorationActivated(event)) {
                return true
            }
        }

        return false
    }

    companion object {
        fun createFactory(application: Application, publication: Publication, baseUrl: String?, config: EpubNavigatorFragment.Configuration) = createViewModelFactory {
            EpubNavigatorViewModel(application, publication, baseUrl = baseUrl, config)
        }
    }
}