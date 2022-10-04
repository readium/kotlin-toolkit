/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.content.SharedPreferences
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.util.LayoutDirection
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.collection.forEach
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.databinding.ActivityR2ViewpagerBinding
import org.readium.r2.navigator.epub.EpubNavigatorViewModel.RunScriptCommand
import org.readium.r2.navigator.epub.css.FontFamilyDeclaration
import org.readium.r2.navigator.epub.css.FontFamilySource.*
import org.readium.r2.navigator.epub.css.RsProperties
import org.readium.r2.navigator.epub.css.from
import org.readium.r2.navigator.extensions.optRectF
import org.readium.r2.navigator.extensions.positionsByResource
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2PagerAdapter.PageResource
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.launchWebBrowser
import org.readium.r2.shared.util.mediatype.MediaType
import kotlin.math.ceil
import kotlin.reflect.KClass

/**
 * Factory for a [JavascriptInterface] which will be injected in the web views.
 *
 * Return `null` if you don't want to inject the interface for the given [resource].
 */
typealias JavascriptInterfaceFactory = (resource: Link) -> Any?

/**
 * Navigator for EPUB publications.
 *
 * To use this [Fragment], create a factory with `EpubNavigatorFragment.createFactory()`.
 */
@OptIn(ExperimentalDecorator::class, ExperimentalReadiumApi::class)
class EpubNavigatorFragment private constructor(
    override val publication: Publication,
    private val baseUrl: String?,
    private val initialLocator: Locator?,
    internal val listener: Listener?,
    internal val paginationListener: PaginationListener?,
    config: Configuration,
) : Fragment(), VisualNavigator, SelectableNavigator, DecorableNavigator, Configurable<EpubSettings> {

    // Make a copy to prevent the user from modifying the configuration after initialization.
    internal val config: Configuration = config.copy(
        servedAssets = config.servedAssets + "readium/.*"
    )

    data class Configuration(
        /**
         * Initial set of setting preferences.
         */
        @ExperimentalReadiumApi
        val preferences: Preferences = Preferences(),

        /**
         * Fallback preferences when missing.
         */
        @ExperimentalReadiumApi
        val defaultPreferences: Preferences = Preferences(),

        /**
         * Patterns for asset paths which will be available to EPUB resources under
         * https://readium/assets/.
         *
         * The patterns can use simple glob wildcards, see:
         * https://developer.android.com/reference/android/os/PatternMatcher#PATTERN_SIMPLE_GLOB
         *
         * Use .* to serve all app assets.
         */
        @ExperimentalReadiumApi
        val servedAssets: List<String> = emptyList(),

        /**
         * Font families available in reflowable resources.
         */
        @ExperimentalReadiumApi
        val fontFamilies: List<FontFamilyDeclaration> = DEFAULT_FONT_FAMILIES,

        /**
         * Readium CSS reading system settings.
         *
         * See https://readium.org/readium-css/docs/CSS19-api.html#reading-system-styles
         */
        @ExperimentalReadiumApi
        val readiumCssRsProperties: RsProperties = RsProperties(),

        /**
         * Supported HTML decoration templates.
         */
        val decorationTemplates: HtmlDecorationTemplates = HtmlDecorationTemplates.defaultTemplates(),

        /**
         * Custom [ActionMode.Callback] to be used when the user selects content.
         *
         * Provide one if you want to customize the selection context menu items.
         */
        var selectionActionModeCallback: ActionMode.Callback? = null,

        /**
         * Whether padding accounting for display cutouts should be applied.
         */
        val shouldApplyInsetsPadding: Boolean? = true,

        internal val javascriptInterfaces: MutableMap<String, JavascriptInterfaceFactory> = mutableMapOf()
    ) {
        /**
         * Registers a new factory for the [JavascriptInterface] named [name].
         *
         * Return `null` in [factory] to prevent adding the Javascript interface for a given
         * resource.
         */
        fun registerJavascriptInterface(name: String, factory: JavascriptInterfaceFactory) {
            javascriptInterfaces[name] = factory
        }

        companion object {
            /**
             * Default font family declarations.
             *
             * Warning: Most of them require an Internet connection (Google Fonts).
             */
            val DEFAULT_FONT_FAMILIES: List<FontFamilyDeclaration> = listOf(
                FontFamily.LITERATA.from(GoogleFonts),
                FontFamily.PT_SERIF.from(GoogleFonts),
                FontFamily.ROBOTO.from(GoogleFonts),
                FontFamily.SOURCE_SANS_PRO.from(GoogleFonts),
                FontFamily.VOLLKORN.from(GoogleFonts),
                FontFamily.ACCESSIBLE_DFA.from(ReadiumCss),
                FontFamily.IA_WRITER_DUOSPACE.from(ReadiumCss),
                FontFamily.OPEN_DYSLEXIC.from(Assets("readium/fonts/OpenDyslexic-Regular.otf")),
            )
        }
    }

    interface PaginationListener {
        fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {}
        fun onPageLoaded() {}
    }

    interface Listener: VisualNavigator.Listener

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection."}
    }

    // Configurable

    override val settings: StateFlow<EpubSettings> get() = viewModel.settings

    override fun submitPreferences(preferences: Preferences) {
        viewModel.submitPreferences(preferences)
    }

    /**
     * Evaluates the given JavaScript on the currently visible HTML resource.
     *
     * Note that this only work with reflowable resources.
     */
    suspend fun evaluateJavascript(script: String): String? {
        val page = currentReflowablePageFragment ?: return null
        page.awaitLoaded()
        val webView = page.webView ?: return null
        return webView.runJavaScriptSuspend(script)
    }

    private val viewModel: EpubNavigatorViewModel by viewModels {
        EpubNavigatorViewModel.createFactory(requireActivity().application, publication, baseUrl = baseUrl, this.config)
    }

    internal lateinit var positionsByReadingOrder: List<List<Locator>>
    internal lateinit var positions: List<Locator>
    lateinit var resourcePager: R2ViewPager

    private lateinit var resourcesSingle: List<PageResource>
    private lateinit var resourcesDouble: List<PageResource>

    @Deprecated("Migrate to the new Settings API (see migration guide)")
    val preferences: SharedPreferences get() = viewModel.preferences

    internal lateinit var publicationIdentifier: String

    internal var currentPagerPosition: Int = 0
    internal lateinit var adapter: R2PagerAdapter
    private lateinit var currentActivity: FragmentActivity

    internal var navigatorDelegate: NavigatorDelegate? = null

    private val r2Activity: R2EpubActivity? get() = activity as? R2EpubActivity

    private var _binding: ActivityR2ViewpagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentActivity = requireActivity()
        _binding = ActivityR2ViewpagerBinding.inflate(inflater, container, false)
        val view = binding.root

        positionsByReadingOrder = runBlocking { publication.positionsByReadingOrder() }
        positions = positionsByReadingOrder.flatten()
        publicationIdentifier = publication.metadata.identifier ?: publication.metadata.title

        resourcePager = binding.resourcePager
        resourcePager.type = Publication.TYPE.EPUB

        when (publication.metadata.presentation.layout) {
            EpubLayout.REFLOWABLE, null -> {
                resourcesSingle = publication.readingOrder.mapIndexed { index, link ->
                    PageResource.EpubReflowable(
                        link = link,
                        url = viewModel.urlTo(link),
                        positionCount = positionsByReadingOrder.getOrNull(index)?.size ?: 0
                    )
                }

                resourcePager.type = Publication.TYPE.EPUB
            }

            EpubLayout.FIXED -> {
                val resourcesSingle = mutableListOf<PageResource>()
                val resourcesDouble = mutableListOf<PageResource>()

                // TODO needs work, currently showing two resources for fxl, needs to understand which two resources, left & right, or only right etc.
                var doublePageLeft: Link? = null
                var doublePageRight: Link? = null

                for ((index, link) in publication.readingOrder.withIndex()) {
                    val url = viewModel.urlTo(link)
                    resourcesSingle.add(PageResource.EpubFxl(leftLink = link, leftUrl = url))

                    // add first page to the right,
                    if (index == 0) {
                        resourcesDouble.add(PageResource.EpubFxl(rightLink = link, rightUrl = url))
                    } else {
                        // add double pages, left & right
                        if (doublePageLeft == null) {
                            doublePageLeft = link
                        } else {
                            doublePageRight = link
                            resourcesDouble.add(
                                PageResource.EpubFxl(
                                    leftLink = doublePageLeft,
                                    leftUrl = viewModel.urlTo(doublePageLeft),
                                    rightLink = doublePageRight,
                                    rightUrl = viewModel.urlTo(doublePageRight),
                                )
                            )
                            doublePageLeft = null
                        }
                    }
                }
                // add last page if there is only a left page remaining
                if (doublePageLeft != null) {
                    resourcesDouble.add(PageResource.EpubFxl(leftLink = doublePageLeft, leftUrl = viewModel.urlTo(doublePageLeft)))
                }

                this.resourcesSingle = resourcesSingle
                this.resourcesDouble = resourcesDouble

                resourcePager.type = Publication.TYPE.FXL
            }
        }

        resetAdapter()

        @Suppress("DEPRECATION")
        if (viewModel.useLegacySettings && publication.cssStyle == ReadingProgression.RTL.value) {
            resourcePager.direction = ReadingProgression.RTL
        }

        resourcePager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {

            override fun onPageSelected(position: Int) {
//                if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE) {
//                    resourcePager.disableTouchEvents = true
//                }
                currentReflowablePageFragment?.webView?.let { webView ->
                    if (viewModel.isScrollEnabled) {
                        if (currentPagerPosition < position) {
                            // handle swipe LEFT
                            webView.scrollToStart()
                        } else if (currentPagerPosition > position) {
                            // handle swipe RIGHT
                            webView.scrollToEnd()
                        }
                    } else {
                        if (currentPagerPosition < position) {
                            // handle swipe LEFT
                            webView.setCurrentItem(0, false)
                        } else if (currentPagerPosition > position) {
                            // handle swipe RIGHT
                            webView.setCurrentItem(webView.numPages - 1, false)
                        }
                    }
                }
                currentPagerPosition = position // Update current position

                notifyCurrentLocation()
            }

        })

        return view
    }

    private fun resetAdapter() {
        adapter = when (publication.metadata.presentation.layout) {
            EpubLayout.REFLOWABLE, null -> {
                R2PagerAdapter(childFragmentManager, resourcesSingle)
            }
            EpubLayout.FIXED -> {
                when (viewModel.dualPageMode) {
                    // FIXME: Properly implement DualPage.AUTO depending on the device orientation.
                    DualPage.OFF, DualPage.AUTO -> {
                        R2PagerAdapter(childFragmentManager, resourcesSingle)
                    }
                    DualPage.ON -> {
                        R2PagerAdapter(childFragmentManager, resourcesDouble)
                    }
                }
            }
        }
        adapter.listener = PagerAdapterListener()
        resourcePager.adapter = adapter
        resourcePager.direction = readingProgression
        resourcePager.layoutDirection = when (readingProgression) {
            ReadingProgression.RTL, ReadingProgression.BTT -> LayoutDirection.RTL
            ReadingProgression.LTR, ReadingProgression.AUTO -> LayoutDirection.LTR
            ReadingProgression.TTB -> LayoutDirection.LTR
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events
                    .onEach(::handleEvent)
                    .launchIn(this)

                var previousSettings = viewModel.settings.value
                viewModel.settings
                    .onEach {
                        onSettingsChange(previousSettings, it)
                        previousSettings = it
                    }
                    .launchIn(this)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            whenStarted {
                // Restore the last locator before a configuration change (e.g. screen rotation), or the
                // initial locator when given.
                val locator = savedInstanceState?.getParcelable("locator") ?: initialLocator
                if (locator != null) {
                    go(locator)
                }
            }
        }
    }

    private fun handleEvent(event: EpubNavigatorViewModel.Event) {
        when (event) {
            is EpubNavigatorViewModel.Event.RunScript -> {
                run(event.command)
            }
            is EpubNavigatorViewModel.Event.GoTo -> {
                go(event.target)
            }
            EpubNavigatorViewModel.Event.InvalidateViewPager -> {
                invalidateResourcePager()
            }
            is EpubNavigatorViewModel.Event.OpenExternalLink -> {
                launchWebBrowser(requireContext(), event.url)
            }
        }
    }

    private fun invalidateResourcePager() {
        val locator = currentLocator.value
        resetAdapter()
        resourcePager.invalidate()
        go(locator)
    }

    private fun onSettingsChange(previous: EpubSettings, new: EpubSettings) {
        if (new !is EpubSettings.Reflowable) return

        if ((previous as? EpubSettings.Reflowable)?.fontSize?.value != new.fontSize.value) {
            r2PagerAdapter?.setFontSize(new.fontSize.value)
        }
    }

    private fun R2PagerAdapter.setFontSize(fontSize: Double) {
        r2PagerAdapter?.mFragments?.forEach { _, fragment ->
            (fragment as? R2EpubPageFragment)?.setFontSize(fontSize)
        }
    }

    private inner class PagerAdapterListener : R2PagerAdapter.Listener {
        override fun onCreatePageFragment(fragment: Fragment) {
            val settings = settings.value as? EpubSettings.Reflowable ?: return
            (fragment as? R2EpubPageFragment)?.setFontSize(settings.fontSize.value)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("locator", currentLocator.value)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        notifyCurrentLocation()
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        listener?.onJumpToLocator(locator)

        val href = locator.href
            // Remove anchor
            .substringBefore("#")

        fun setCurrent(resources: List<PageResource>) {
            val page = resources.withIndex().firstOrNull { (_, res) ->
                when (res) {
                    is PageResource.EpubReflowable -> res.link.href == href
                    is PageResource.EpubFxl -> res.leftUrl?.endsWith(href) == true || res.rightUrl?.endsWith(href) == true
                    else -> false
                }
            } ?: return
            val (index, _) = page

            if (resourcePager.currentItem != index) {
                resourcePager.currentItem = index
            }
            r2PagerAdapter?.loadLocatorAt(index, locator)
        }

        if (publication.metadata.presentation.layout != EpubLayout.FIXED) {
            setCurrent(resourcesSingle)
        } else {

            when (viewModel.dualPageMode) {
                // FIXME: Properly implement DualPage.AUTO depending on the device orientation.
                DualPage.OFF, DualPage.AUTO -> {
                    setCurrent(resourcesSingle)
                }
                DualPage.ON -> {
                    setCurrent(resourcesDouble)
                }
            }
        }

        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    private fun run(commands: List<RunScriptCommand>) {
        commands.forEach { run(it) }
    }

    private fun run(command: RunScriptCommand) {
        when (command.scope) {
            RunScriptCommand.Scope.CurrentResource -> {
                currentReflowablePageFragment?.webView
                    ?.runJavaScript(command.script)
            }
            RunScriptCommand.Scope.LoadedResources -> {
                r2PagerAdapter?.mFragments?.forEach { _, fragment ->
                    (fragment as? R2EpubPageFragment)?.webView
                        ?.runJavaScript(command.script)
                }
            }
            is RunScriptCommand.Scope.Resource -> {
                loadedFragmentForHref(command.scope.href)?.webView
                    ?.runJavaScript(command.script)
            }
            is RunScriptCommand.Scope.WebView -> {
                command.scope.webView.runJavaScript(command.script)
            }
        }
    }

    // SelectableNavigator

    override suspend fun currentSelection(): Selection? {
        val webView = currentReflowablePageFragment?.webView ?: return null
        val json =
            webView.runJavaScriptSuspend("readium.getCurrentSelection();")
                .takeIf { it != "null"}
                ?.let { tryOrLog { JSONObject(it) } }
                ?: return null

        val rect = json.optRectF("rect")
            ?.run { adjustedToViewport() }

        return Selection(
            locator = currentLocator.value.copy(
                text = Locator.Text.fromJSON(json.optJSONObject("text"))
            ),
            rect = rect
        )
    }

    override fun clearSelection() {
        run(viewModel.clearSelection())
    }

    private fun PointF.adjustedToViewport(): PointF =
        currentReflowablePageFragment?.paddingTop?.let { top ->
            PointF(x, y + top)
        } ?: this

    private fun RectF.adjustedToViewport(): RectF =
        currentReflowablePageFragment?.paddingTop?.let { topOffset ->
            RectF(left, top + topOffset, right, bottom)
        } ?: this

    // DecorableNavigator

    override fun <T : Decoration.Style> supportsDecorationStyle(style: KClass<T>): Boolean =
        viewModel.supportsDecorationStyle(style)

    override fun addDecorationListener(group: String, listener: DecorableNavigator.Listener) {
        viewModel.addDecorationListener(group, listener)
    }

    override fun removeDecorationListener(listener: DecorableNavigator.Listener) {
        viewModel.removeDecorationListener(listener)
    }

    override suspend fun applyDecorations(decorations: List<Decoration>, group: String) {
        run(viewModel.applyDecorations(decorations, group))
    }

    // R2BasicWebView.Listener

    internal val webViewListener: R2BasicWebView.Listener = WebViewListener()

    @OptIn(ExperimentalDragGesture::class)
    private inner class WebViewListener : R2BasicWebView.Listener {

        override val readingProgression: ReadingProgression
            get() = viewModel.readingProgression

        override fun onResourceLoaded(link: Link?, webView: R2BasicWebView, url: String?) {
            run(viewModel.onResourceLoaded(link, webView))
        }

        override fun onPageLoaded() {
            r2Activity?.onPageLoaded()
            paginationListener?.onPageLoaded()
            notifyCurrentLocation()
        }

        override fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {
            r2Activity?.onPageChanged(pageIndex = pageIndex, totalPages = totalPages, url = url)
        }

        override fun onPageEnded(end: Boolean) {
            r2Activity?.onPageEnded(end)
        }

        override fun javascriptInterfacesForResource(link: Link): Map<String, Any?> =
            config.javascriptInterfaces.mapValues { (_, factory) -> factory(link) }

        @Suppress("DEPRECATION")
        override fun onScroll() {
            val activity = r2Activity ?: return
            if (activity.supportActionBar?.isShowing == true && activity.allowToggleActionBar) {
                resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
        }

        override fun onTap(point: PointF): Boolean =
            listener?.onTap(point.adjustedToViewport()) ?: false

        override fun onDragStart(event: R2BasicWebView.DragEvent): Boolean =
            listener?.onDragStart(
                startPoint = event.startPoint.adjustedToViewport(),
                offset = event.offset
            ) ?: false

        override fun onDragMove(event: R2BasicWebView.DragEvent): Boolean =
            listener?.onDragMove(
                startPoint = event.startPoint.adjustedToViewport(),
                offset = event.offset
            ) ?: false

        override fun onDragEnd(event: R2BasicWebView.DragEvent): Boolean =
            listener?.onDragEnd(
                startPoint = event.startPoint.adjustedToViewport(),
                offset = event.offset
            ) ?: false

        override fun onDecorationActivated(id: DecorationId, group: String, rect: RectF, point: PointF): Boolean =
            viewModel.onDecorationActivated(
                id = id,
                group = group,
                rect = rect.adjustedToViewport(),
                point = point.adjustedToViewport()
            )

        override fun onProgressionChanged() {
            notifyCurrentLocation()
        }

        override fun onHighlightActivated(id: String) {
            r2Activity?.highlightActivated(id)
        }

        override fun onHighlightAnnotationMarkActivated(id: String) {
            r2Activity?.highlightAnnotationMarkActivated(id)
        }

        override fun goForward(animated: Boolean, completion: () -> Unit): Boolean =
            goToNextResource(animated, completion)

        override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean =
            goToPreviousResource(animated, completion)

        override val selectionActionModeCallback: ActionMode.Callback?
            get() = config.selectionActionModeCallback

        /**
         * Prevents opening external links in the web view and handles internal links.
         */
        override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
            val url = request.url ?: return false
            viewModel.navigateToUrl(url)
            return true
        }

        override fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? =
            viewModel.shouldInterceptRequest(request)

        override fun resourceAtUrl(url: String): Resource? =
            viewModel.internalLinkFromUrl(url)
                ?.let { publication.get(it) }
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        if (publication.metadata.presentation.layout == EpubLayout.FIXED) {
            return goToNextResource(animated, completion)
        }

        val webView = currentReflowablePageFragment?.webView ?: return false

        when (readingProgression) {
            ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
                webView.scrollRight(animated)

            ReadingProgression.RTL, ReadingProgression.BTT ->
                webView.scrollLeft(animated)
        }
        lifecycleScope.launch { completion() }
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        if (publication.metadata.presentation.layout == EpubLayout.FIXED) {
            return goToPreviousResource(animated, completion)
        }

        val webView = currentReflowablePageFragment?.webView ?: return false

        when (readingProgression) {
            ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
                webView.scrollLeft(animated)

            ReadingProgression.RTL, ReadingProgression.BTT ->
                webView.scrollRight(animated)
        }
        lifecycleScope.launch { completion() }
        return true
    }

    private fun goToNextResource(animated: Boolean, completion: () -> Unit): Boolean {
        val adapter = resourcePager.adapter ?: return false
        if (resourcePager.currentItem >= adapter.count - 1) {
            return false
        }

        resourcePager.setCurrentItem(resourcePager.currentItem + 1, animated)

        currentReflowablePageFragment?.webView?.let { webView ->
            if (readingProgression == ReadingProgression.RTL) {
                webView.setCurrentItem(webView.numPages - 1, false)
            } else {
                webView.setCurrentItem(0, false)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch { completion() }
        return true
    }

    private fun goToPreviousResource(animated: Boolean, completion: () -> Unit): Boolean {
        if (resourcePager.currentItem <= 0) {
            return false
        }

        resourcePager.setCurrentItem(resourcePager.currentItem - 1, animated)

        currentReflowablePageFragment?.webView?.let { webView ->
            if (readingProgression == ReadingProgression.RTL) {
                webView.setCurrentItem(0, false)
            } else {
                webView.setCurrentItem(webView.numPages - 1, false)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch { completion() }
        return true
    }

    private val r2PagerAdapter: R2PagerAdapter?
        get() = if (::resourcePager.isInitialized) resourcePager.adapter as? R2PagerAdapter
            else null

    private val currentReflowablePageFragment: R2EpubPageFragment? get() =
        currentFragment as? R2EpubPageFragment

    private val currentFragment: Fragment? get() =
        fragmentAt(resourcePager.currentItem)

    private fun fragmentAt(index: Int): Fragment? =
        r2PagerAdapter?.mFragments?.get(adapter.getItemId(index))

    /**
     * Returns the reflowable page fragment matching the given href, if it is already loaded in the
     * view pager.
     */
    private fun loadedFragmentForHref(href: String): R2EpubPageFragment? {
        val adapter = r2PagerAdapter ?: return null
        adapter.mFragments.forEach { _, fragment ->
            val pageFragment = fragment as? R2EpubPageFragment ?: return@forEach
            val link = pageFragment.link ?: return@forEach
            if (link.href == href) {
                return pageFragment
            }
        }
        return null
    }

    override val readingProgression: ReadingProgression
        get() = viewModel.readingProgression

    override val currentLocator: StateFlow<Locator> get() = _currentLocator
    private val _currentLocator = MutableStateFlow(initialLocator
        ?: requireNotNull(publication.locatorFromLink(publication.readingOrder.first()))
    )

    /**
     * Returns the [Locator] to the first HTML element that begins on the current screen.
     */
    @ExperimentalReadiumApi
    override suspend fun firstVisibleElementLocator(): Locator? {
        if (!::resourcePager.isInitialized) return null

        val resource = publication.readingOrder[resourcePager.currentItem]
        return currentReflowablePageFragment?.webView?.findFirstVisibleLocator()
            ?.copy(
                href = resource.href,
                type = resource.type ?: MediaType.XHTML.toString()
            )
    }

    /**
     * While scrolling we receive a lot of new current locations, so we use a coroutine job to
     * debounce the notification.
     */
    private var debounceLocationNotificationJob: Job? = null

    /**
     * Mapping between reading order hrefs and the table of contents title.
     */
    private val tableOfContentsTitleByHref: Map<String, String> by lazy {
        fun fulfill(linkList: List<Link>): MutableMap<String, String> {
            var result: MutableMap<String, String> = mutableMapOf()

            for (link in linkList) {
                val title = link.title?: ""

                if (title.isNotEmpty()) {
                    result[link.href] = title
                }

                val subResult = fulfill(link.children)

                result = (subResult + result) as MutableMap<String, String>
            }

            return result
        }

        fulfill(publication.tableOfContents).toMap()
    }

    private fun notifyCurrentLocation() {
        val navigator = this
        debounceLocationNotificationJob?.cancel()
        debounceLocationNotificationJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(100L)

            if (currentReflowablePageFragment?.isLoaded?.value == false) {
                return@launch
            }

            val reflowableWebView = currentReflowablePageFragment?.webView
            val progression = reflowableWebView?.run {
                // The transition has stabilized, so we can ask the web view to refresh its current
                // item to reflect the current scroll position.
                updateCurrentItem()
                progression.coerceIn(0.0, 1.0)
            } ?: 0.0

            val link = when (val pageResource = adapter.getResource(resourcePager.currentItem)) {
                is PageResource.EpubFxl -> checkNotNull(pageResource.leftLink ?: pageResource.rightLink)
                is PageResource.EpubReflowable -> pageResource.link
                else -> throw IllegalStateException("Expected EpubFxl or EpubReflowable page resources")
            }
            val positionLocator = publication.positionsByResource[link.href]?.let { positions ->
                val index = ceil(progression * (positions.size - 1)).toInt()
                positions.getOrNull(index)
            }

            val currentLocator = Locator(
                href = link.href,
                type = link.type ?: MediaType.XHTML.toString(),
                title = tableOfContentsTitleByHref[link.href] ?: positionLocator?.title ?: link.title,
                locations = (positionLocator?.locations ?: Locator.Locations()).copy(
                    progression = progression
                ),
                text = positionLocator?.text ?: Locator.Text()
            )

            _currentLocator.value = currentLocator

            // Deprecated notifications
            @Suppress("DEPRECATION")
            navigatorDelegate?.locationDidChange(navigator = navigator, locator = currentLocator)
            reflowableWebView?.let {
                paginationListener?.onPageChanged(
                    pageIndex = it.mCurItem,
                    totalPages = it.numPages,
                    locator = currentLocator
                )
            }
        }
    }

    companion object {

        /**
         * Creates a factory for [EpubNavigatorFragment].
         *
         * @param publication EPUB publication to render in the navigator.
         * @param baseUrl A base URL where this publication is served from. This is optional, only
         * if you use a local HTTP server.
         * @param initialLocator The first location which should be visible when rendering the
         * publication. Can be used to restore the last reading location.
         * @param listener Optional listener to implement to observe events, such as user taps.
         * @param config Additional configuration.
         */
        fun createFactory(
            publication: Publication,
            baseUrl: String? = null,
            initialLocator: Locator? = null,
            listener: Listener? = null,
            paginationListener: PaginationListener? = null,
            config: Configuration = Configuration(),
        ): FragmentFactory =
            createFragmentFactory { EpubNavigatorFragment(publication, baseUrl, initialLocator, listener, paginationListener, config) }

        /**
         * Returns a URL to the application asset at [path], served in the web views.
         */
        fun assetUrl(path: String): String =
            WebViewServer.assetUrl(path)
    }

}
