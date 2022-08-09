/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.graphics.PointF
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.epub.extensions.javascriptForGroup
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import kotlin.reflect.KClass

@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class)
internal class EpubNavigatorViewModel(
    val publication: Publication,
    val config: EpubNavigatorFragment.Configuration,
) : ViewModel() {

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
        fun createFactory(publication: Publication, config: EpubNavigatorFragment.Configuration) = createViewModelFactory {
            EpubNavigatorViewModel(publication, config)
        }
    }
}