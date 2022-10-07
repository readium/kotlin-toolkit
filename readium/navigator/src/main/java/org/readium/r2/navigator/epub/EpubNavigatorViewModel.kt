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
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.epub.extensions.javascriptForGroup
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.settings.ColumnCount
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.COLUMN_COUNT_REF
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Href
import kotlin.reflect.KClass

internal enum class DualPage {
    AUTO, OFF, ON
}

@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class)
internal class EpubNavigatorViewModel(
    application: Application,
    val publication: Publication,
    val config: EpubNavigatorFragment.Configuration,
    baseUrl: String?,
    private val server: WebViewServer?,
) : AndroidViewModel(application) {

    val useLegacySettings: Boolean = (server == null)

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
        /** Refreshes all the resources in the view pager. */
        object InvalidateViewPager : Event()
        data class RunScript(val command: RunScriptCommand) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> get() = _events.receiveAsFlow()

    private val _settings = MutableStateFlow<EpubSettings>(
        when (publication.metadata.presentation.layout) {
            EpubLayout.FIXED -> EpubSettings.FixedLayout().update(
                preferences = config.preferences,
                defaults = config.defaultPreferences
            )
            EpubLayout.REFLOWABLE, null -> EpubSettings.Reflowable().update(
                metadata = publication.metadata,
                fontFamilies = config.fontFamilies.map { it.fontFamily },
                namedColors = emptyMap(),
                preferences = config.preferences,
                defaults = config.defaultPreferences
            )
        }
    )
    val settings: StateFlow<EpubSettings> = _settings.asStateFlow()

    private val css = MutableStateFlow(
        ReadiumCss(
            rsProperties = config.readiumCssRsProperties,
            fontFamilies = config.fontFamilies,
            assetsBaseHref = WebViewServer.assetsBaseHref
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

    // Serving resources

    val baseUrl: String =
        baseUrl?.let { it.removeSuffix("/") + "/" }
            ?: publication.linkWithRel("self")?.href
            ?: WebViewServer.publicationBaseHref

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
        val href = url.toString()
        val link = internalLinkFromUrl(href)
        if (link != null) {
            _events.send(Event.GoTo(link))
        } else {
            _events.send(Event.OpenExternalLink(url))
        }
    }

    /**
     * Gets the publication [Link] targeted by the given [url].
     */
    fun internalLinkFromUrl(url: String): Link? {
        if (!url.startsWith(baseUrl)) return null

        val href = url.removePrefix(baseUrl).addPrefix("/")
        return publication.linkWithHref(href)
            // Query parameters must be kept as they might be relevant for the fetcher.
            ?.copy(href = href)
    }

    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? =
        server?.shouldInterceptRequest(request, css.value)

    // Settings

    fun submitPreferences(preferences: Preferences) = viewModelScope.launch {
        val oldReflowSettings = (settings.value as? EpubSettings.Reflowable)
        val oldFixedSettings = (settings.value as? EpubSettings.FixedLayout)
        val oldReadingProgression = readingProgression

        val newSettings = _settings.updateAndGet { settings ->
            when (settings) {
                is EpubSettings.FixedLayout -> settings.update(
                    preferences = preferences,
                    defaults = config.defaultPreferences
                )

                is EpubSettings.Reflowable -> settings.update(
                    metadata = publication.metadata,
                    fontFamilies = config.fontFamilies.map { it.fontFamily },
                    namedColors = emptyMap(),
                    preferences = preferences,
                    defaults = config.defaultPreferences
                )
            }
        }
        val newReflowSettings = (newSettings as? EpubSettings.Reflowable)
        val newFixedSettings = (newSettings as? EpubSettings.FixedLayout)

        css.update { it.update(newSettings) }

        val needsInvalidation: Boolean = (
            oldReadingProgression != readingProgression ||
            oldReflowSettings?.verticalText?.value != newReflowSettings?.verticalText?.value ||
            oldFixedSettings?.spread?.value != newFixedSettings?.spread?.value
        )

        if (needsInvalidation) {
            _events.send(Event.InvalidateViewPager)
        }
    }

    /**
     * Effective reading progression.
     */
    val readingProgression: ReadingProgression get() =
        if (useLegacySettings) {
            publication.metadata.effectiveReadingProgression
        } else {
            settings.value.readingProgression.value
        }

    /**
     * Indicates whether the dual page mode is enabled.
     */
    val dualPageMode: DualPage get() =
        if (useLegacySettings) {
            @Suppress("DEPRECATION")
            when (preferences.getInt(COLUMN_COUNT_REF, 0)) {
                1 -> DualPage.OFF
                2 -> DualPage.ON
                else -> DualPage.AUTO
            }
        } else {
            when (val settings = settings.value) {
                is EpubSettings.FixedLayout -> when (settings.spread.value) {
                    Presentation.Spread.AUTO -> DualPage.AUTO
                    Presentation.Spread.BOTH -> DualPage.ON
                    Presentation.Spread.NONE -> DualPage.OFF
                    Presentation.Spread.LANDSCAPE -> DualPage.AUTO
                }
                is EpubSettings.Reflowable -> when (settings.columnCount?.value) {
                    ColumnCount.ONE, null -> DualPage.OFF
                    ColumnCount.TWO -> DualPage.ON
                    ColumnCount.AUTO -> DualPage.AUTO
                }
            }
        }

    /**
     * Indicates whether the navigator is scrollable instead of paginated.
     */
    val isScrollEnabled: Boolean get() =
        if (useLegacySettings) {
            @Suppress("DEPRECATION")
            preferences.getBoolean(SCROLL_REF, false)
        } else {
            (settings.value as? EpubSettings.Reflowable)?.scroll?.value ?: true
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
            EpubNavigatorViewModel(application, publication, config,
                baseUrl = baseUrl,
                server = if (baseUrl != null) null
                    else WebViewServer(application, publication, servedAssets = config.servedAssets)
            )
        }
    }
}