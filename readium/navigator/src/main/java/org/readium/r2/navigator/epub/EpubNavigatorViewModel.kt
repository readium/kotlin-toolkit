/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.app.Application
import android.graphics.PointF
import android.graphics.RectF
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlin.reflect.KClass
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.epub.extensions.javascriptForGroup
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.preferences.*
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

internal enum class DualPage {
    AUTO,
    OFF,
    ON,
}

@OptIn(ExperimentalReadiumApi::class, DelicateReadiumApi::class)
internal class EpubNavigatorViewModel(
    application: Application,
    val publication: Publication,
    val config: EpubNavigatorFragment.Configuration,
    initialPreferences: EpubPreferences,
    val layout: EpubLayout,
    val listener: EpubNavigatorFragment.Listener?,
    private val defaults: EpubDefaults,
    private val server: WebViewServer,
) : AndroidViewModel(application) {

    // Make a copy to prevent new decoration templates from being registered after initializing
    // the navigator.
    private val decorationTemplates: HtmlDecorationTemplates = config.decorationTemplates.copy()

    data class RunScriptCommand(val script: String, val scope: Scope) {
        sealed class Scope {
            object CurrentResource : Scope()
            object LoadedResources : Scope()
            data class LoadedResource(val href: Url) : Scope()
            data class WebView(val webView: R2BasicWebView) : Scope()
        }
    }

    sealed class Event {
        data class OpenInternalLink(val target: Link) : Event()

        /** Refreshes all the resources in the view pager. */
        object InvalidateViewPager : Event()
        data class RunScript(val command: RunScriptCommand) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> get() = _events.receiveAsFlow()

    private val settingsPolicy: EpubSettingsResolver =
        EpubSettingsResolver(publication.metadata, defaults)

    private val _settings: MutableStateFlow<EpubSettings> =
        MutableStateFlow(settingsPolicy.settings(initialPreferences))
    val settings: StateFlow<EpubSettings> = _settings.asStateFlow()

    val overflow: StateFlow<OverflowableNavigator.Overflow> = _settings
        .mapStateIn(viewModelScope) { settings ->
            SimpleOverflow(
                readingProgression = settings.readingProgression,
                scroll = if (layout == EpubLayout.REFLOWABLE) {
                    settings.scroll
                } else {
                    false
                },
                axis = if (settings.scroll && !settings.verticalText) {
                    Axis.VERTICAL
                } else {
                    Axis.HORIZONTAL
                }
            )
        }

    private val css = MutableStateFlow(
        ReadiumCss(
            rsProperties = config.readiumCssRsProperties,
            fontFamilyDeclarations = config.fontFamilyDeclarations,
            assetsBaseHref = WebViewServer.assetsBaseHref
        ).update(settings.value, useReadiumCssFontSize = config.useReadiumCssFontSize)
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
                    _events.send(
                        Event.RunScript(
                            RunScriptCommand(
                                script = "readium.setCSSProperties(${JSONObject(properties.toMap())});",
                                scope = RunScriptCommand.Scope.LoadedResources
                            )
                        )
                    )
                }

                previousCss = css
            }
            .launchIn(viewModelScope)
    }

    fun onResourceLoaded(webView: R2BasicWebView, link: Link): List<RunScriptCommand> =
        buildList {
            val scope = RunScriptCommand.Scope.WebView(webView)

            // Applies the Readium CSS properties in case they changed since they were injected
            // in the HTML document.
            val properties = css.value.run {
                rsProperties.toCssProperties() + userProperties.toCssProperties()
            }

            add(
                RunScriptCommand(
                    script = "readium.setCSSProperties(${JSONObject(properties.toMap())});",
                    scope = scope
                )
            )

            // Applies the decorations.
            val templates = decorationTemplates.toJSON().toString()
                .replace("\\n", " ")
            var script = "readium.registerDecorationTemplates($templates);\n"

            for ((group, decorations) in decorations) {
                val changes = decorations
                    .filter { it.locator.href == link.url() }
                    .map { DecorationChange.Added(it) }

                val groupScript = changes.javascriptForGroup(group, decorationTemplates) ?: continue
                script += "$groupScript\n"
            }

            add(RunScriptCommand(script, scope = scope))
        }

    // Serving resources

    val baseUrl: AbsoluteUrl =
        (publication.baseUrl as? AbsoluteUrl)
            ?: WebViewServer.publicationBaseHref

    /**
     * Generates the URL to the given publication link.
     */
    fun urlTo(link: Link): AbsoluteUrl =
        baseUrl.resolve(link.url())

    /**
     * Intercepts and handles web view navigation to [url].
     */
    fun navigateToUrl(
        url: AbsoluteUrl,
        context: HyperlinkNavigator.LinkContext? = null,
    ) = viewModelScope.launch {
        val link = internalLinkFromUrl(url)
        if (link != null) {
            if (listener == null || listener.shouldFollowInternalLink(link, context)) {
                _events.send(Event.OpenInternalLink(link))
            }
        } else {
            listener?.onExternalLinkActivated(url)
        }
    }

    /**
     * Gets the publication [Link] targeted by the given [url].
     */
    fun internalLinkFromUrl(url: Url): Link? {
        val href = (baseUrl.relativize(url) as? RelativeUrl)
            ?: return null

        return publication.linkWithHref(href)
            // Query parameters must be kept as they might be relevant for the container.
            ?.copy(href = Href(href))
    }

    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? =
        server.shouldInterceptRequest(request, css.value)

    fun submitPreferences(preferences: EpubPreferences) = viewModelScope.launch {
        val oldSettings = settings.value

        val newSettings = settingsPolicy.settings(preferences)
        _settings.value = newSettings
        css.update { it.update(newSettings, useReadiumCssFontSize = config.useReadiumCssFontSize) }

        val needsInvalidation: Boolean = (
            oldSettings.readingProgression != newSettings.readingProgression ||
                oldSettings.language != newSettings.language ||
                oldSettings.verticalText != newSettings.verticalText ||
                oldSettings.spread != newSettings.spread ||
                // We need to invalidate the resource pager when changing from scroll mode to
                // paginated, otherwise the horizontal scroll will be broken.
                // See https://github.com/readium/kotlin-toolkit/pull/304
                oldSettings.scroll != newSettings.scroll
            )

        if (needsInvalidation) {
            _events.send(Event.InvalidateViewPager)
        }
    }

    /**
     * Effective reading progression.
     */
    val readingProgression: ReadingProgression get() =
        settings.value.readingProgression

    /**
     * Effective vertical text.
     */
    val verticalText: Boolean get() =
        settings.value.verticalText

    /**
     * Indicates whether the dual page mode is enabled.
     */
    val dualPageMode: DualPage get() =
        when (layout) {
            EpubLayout.FIXED -> when (settings.value.spread) {
                Spread.AUTO -> DualPage.AUTO
                Spread.ALWAYS -> DualPage.ON
                Spread.NEVER -> DualPage.OFF
            }
            EpubLayout.REFLOWABLE -> when (settings.value.columnCount) {
                ColumnCount.ONE -> DualPage.OFF
                ColumnCount.TWO -> DualPage.ON
                ColumnCount.AUTO -> DualPage.AUTO
            }
        }

    /**
     * Indicates whether the navigator is scrollable instead of paginated.
     */
    val isScrollEnabled: StateFlow<Boolean> get() =
        settings.mapStateIn(viewModelScope) {
            if (layout == EpubLayout.REFLOWABLE) it.scroll else false
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
            cmds.add(
                RunScriptCommand(
                    // The updates command are using `requestAnimationFrame()`, so we need it for
                    // `clear()` as well otherwise we might recreate a highlight after it has been
                    // cleared.
                    "requestAnimationFrame(function () { readium.getDecorations('$group').clear(); });",
                    scope = RunScriptCommand.Scope.LoadedResources
                )
            )
        } else {
            for ((href, changes) in source.changesByHref(target)) {
                val script = changes.javascriptForGroup(group, decorationTemplates) ?: continue
                cmds.add(
                    RunScriptCommand(script, scope = RunScriptCommand.Scope.LoadedResource(href))
                )
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
            decoration = decoration,
            group = group,
            rect = rect,
            point = point
        )
        for (listener in listeners) {
            if (listener.onDecorationActivated(event)) {
                return true
            }
        }

        return false
    }

    companion object {

        fun createFactory(
            application: Application,
            publication: Publication,
            layout: EpubLayout,
            listener: EpubNavigatorFragment.Listener?,
            defaults: EpubDefaults,
            config: EpubNavigatorFragment.Configuration,
            initialPreferences: EpubPreferences,
        ) = createViewModelFactory {
            EpubNavigatorViewModel(
                application,
                publication,
                config,
                initialPreferences,
                layout,
                listener,
                defaults = defaults,
                server = WebViewServer(
                    application,
                    publication,
                    servedAssets = config.servedAssets,
                    disableSelectionWhenProtected = config.disableSelectionWhenProtected,
                    onResourceLoadFailed = { url, error ->
                        listener?.onResourceLoadFailed(url, error)
                    }
                )
            )
        }
    }
}
