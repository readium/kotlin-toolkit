/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
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
import org.readium.r2.shared.COLUMN_COUNT_REF
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.Href

internal enum class DualPage {
    AUTO, OFF, ON
}

@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class, DelicateReadiumApi::class)
internal class EpubNavigatorViewModel(
    application: Application,
    val publication: Publication,
    val config: EpubNavigatorFragment.Configuration,
    initialPreferences: EpubPreferences,
    val layout: EpubLayout,
    val listener: VisualNavigator.Listener?,
    private val defaults: EpubDefaults,
    baseUrl: String?,
    private val server: WebViewServer?,
) : AndroidViewModel(application) {

    val useLegacySettings: Boolean = (server == null)

    val preferences: SharedPreferences =
        application.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

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
        data class OpenInternalLink(val target: Link) : Event()
        data class OpenExternalLink(val url: Uri) : Event()
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

    val presentation: StateFlow<VisualNavigator.Presentation> = _settings
        .mapStateIn(viewModelScope) { settings ->
            SimplePresentation(
                readingProgression = settings.readingProgression,
                scroll = settings.scroll,
                axis = if (settings.scroll && !settings.verticalText) Axis.VERTICAL
                else Axis.HORIZONTAL
            )
        }

    private val googleFonts: List<FontFamily> =
        if (useLegacySettings)
            listOf(
                FontFamily.LITERATA, FontFamily.PT_SERIF, FontFamily.ROBOTO,
                FontFamily.SOURCE_SANS_PRO, FontFamily.VOLLKORN
            )
        else
            emptyList()

    private val css = MutableStateFlow(
        ReadiumCss(
            rsProperties = config.readiumCssRsProperties,
            fontFamilyDeclarations = config.fontFamilyDeclarations,
            googleFonts = googleFonts,
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
            if (listener?.shouldJumpToLink(link) == true) {
                _events.send(Event.OpenInternalLink(link))
            }
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
    val readingProgression: PublicationReadingProgression get() =
        if (useLegacySettings) {
            publication.metadata.effectiveReadingProgression
        } else when (settings.value.readingProgression) {
            ReadingProgression.LTR -> PublicationReadingProgression.LTR
            ReadingProgression.RTL -> PublicationReadingProgression.RTL
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
        }

    /**
     * Indicates whether the navigator is scrollable instead of paginated.
     */
    val isScrollEnabled: StateFlow<Boolean> get() =
        if (useLegacySettings) {
            @Suppress("DEPRECATION")
            val scroll = preferences.getBoolean(SCROLL_REF, false)
            MutableStateFlow(scroll)
        } else {
            settings.mapStateIn(viewModelScope) {
                if (layout == EpubLayout.REFLOWABLE) it.scroll else false
            }
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

        fun createFactory(
            application: Application,
            publication: Publication,
            baseUrl: String?,
            layout: EpubLayout,
            listener: VisualNavigator.Listener?,
            defaults: EpubDefaults,
            config: EpubNavigatorFragment.Configuration,
            initialPreferences: EpubPreferences
        ) = createViewModelFactory {
            EpubNavigatorViewModel(
                application, publication, config, initialPreferences, layout, listener,
                defaults = defaults,
                baseUrl = baseUrl,
                server = if (baseUrl != null) null
                else WebViewServer(
                    application, publication,
                    servedAssets = config.servedAssets,
                    disableSelectionWhenProtected = config.disableSelectionWhenProtected
                )
            )
        }
    }
}

private val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")
private val FontFamily.Companion.PT_SERIF: FontFamily get() = FontFamily("PT Serif")
private val FontFamily.Companion.ROBOTO: FontFamily get() = FontFamily("Roboto")
private val FontFamily.Companion.SOURCE_SANS_PRO: FontFamily get() = FontFamily("Source Sans Pro")
private val FontFamily.Companion.VOLLKORN: FontFamily get() = FontFamily("Vollkorn")
