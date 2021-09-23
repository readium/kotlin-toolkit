/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.databinding.ActivityR2ViewpagerBinding
import org.readium.r2.navigator.epub.EpubNavigatorViewModel.RunScriptCommand
import org.readium.r2.navigator.extensions.htmlId
import org.readium.r2.navigator.extensions.optRectF
import org.readium.r2.navigator.extensions.positionsByResource
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2PagerAdapter.PageResource
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.COLUMN_COUNT_REF
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positions
import kotlin.math.ceil
import kotlin.reflect.KClass

/**
 * Navigator for EPUB publications.
 *
 * To use this [Fragment], create a factory with `EpubNavigatorFragment.createFactory()`.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalDecorator::class)
class EpubNavigatorFragment private constructor(
    override val publication: Publication,
    private val baseUrl: String,
    private val initialLocator: Locator?,
    internal val listener: Listener?,
    internal val paginationListener: PaginationListener?,
    internal val config: Configuration,
): Fragment(), CoroutineScope by MainScope(), VisualNavigator, SelectableNavigator, DecorableNavigator, R2BasicWebView.Listener {

    data class Configuration(
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
    )

    interface PaginationListener {
        fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {}
        fun onPageLoaded() {}
    }

    interface Listener: VisualNavigator.Listener

    init {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection."}
    }

    private val viewModel: EpubNavigatorViewModel by viewModels {
        // Make a copy to prevent new decoration templates from being registered after initializing
        // the navigator.
        EpubNavigatorViewModel.createFactory(config.decorationTemplates.copy())
    }

    internal lateinit var positions: List<Locator>
    lateinit var resourcePager: R2ViewPager

    private lateinit var resourcesSingle: List<PageResource>
    private lateinit var resourcesDouble: List<PageResource>

    lateinit var preferences: SharedPreferences
    internal lateinit var publicationIdentifier: String

    internal var currentPagerPosition: Int = 0
    internal lateinit var adapter: R2PagerAdapter
    private lateinit var currentActivity: FragmentActivity

    internal var navigatorDelegate: NavigatorDelegate? = null

    private val r2Activity: R2EpubActivity? get() = activity as? R2EpubActivity

    private var _binding: ActivityR2ViewpagerBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preferences = context.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentActivity = requireActivity()
        _binding = ActivityR2ViewpagerBinding.inflate(inflater, container, false)
        val view = binding.root

        positions = runBlocking { publication.positions() }
        publicationIdentifier = publication.metadata.identifier ?: publication.metadata.title

        resourcePager = binding.resourcePager
        resourcePager.type = Publication.TYPE.EPUB

        if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE) {
            resourcesSingle = publication.readingOrder.map { link ->
                PageResource.EpubReflowable(
                    link = link,
                    url = link.withBaseUrl(baseUrl).href
                )
            }

            adapter = R2PagerAdapter(childFragmentManager, resourcesSingle)
            resourcePager.type = Publication.TYPE.EPUB

        } else {
            val resourcesSingle = mutableListOf<PageResource>()
            val resourcesDouble = mutableListOf<PageResource>()

            // TODO needs work, currently showing two resources for fxl, needs to understand which two resources, left & right, or only right etc.
            var doublePageLeft = ""
            var doublePageRight = ""

            for ((index, link) in publication.readingOrder.withIndex()) {
                val url = link.withBaseUrl(baseUrl).href
                resourcesSingle.add(PageResource.EpubFxl(url))

                // add first page to the right,
                if (index == 0) {
                    resourcesDouble.add(PageResource.EpubFxl("", url))
                } else {
                    // add double pages, left & right
                    if (doublePageLeft == "") {
                        doublePageLeft = url
                    } else {
                        doublePageRight = url
                        resourcesDouble.add(PageResource.EpubFxl(doublePageLeft, doublePageRight))
                        doublePageLeft = ""
                    }
                }
            }
            // add last page if there is only a left page remaining
            if (doublePageLeft != "") {
                resourcesDouble.add(PageResource.EpubFxl(doublePageLeft, ""))
            }

            this.resourcesSingle = resourcesSingle
            this.resourcesDouble = resourcesDouble

            resourcePager.type = Publication.TYPE.FXL
            adapter = when (preferences.getInt(COLUMN_COUNT_REF, 0)) {
                1 -> {
                    R2PagerAdapter(childFragmentManager, resourcesSingle)
                }
                2 -> {
                    R2PagerAdapter(childFragmentManager, resourcesDouble)
                }
                else -> {
                    // TODO based on device
                    // TODO decide if 1 page or 2 page
                    R2PagerAdapter(childFragmentManager, resourcesSingle)
                }
            }
        }

        resourcePager.adapter = adapter
        resourcePager.direction = publication.metadata.effectiveReadingProgression

        if (publication.cssStyle == ReadingProgression.RTL.value) {
            resourcePager.direction = ReadingProgression.RTL
        }

        resourcePager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {

            override fun onPageSelected(position: Int) {
//                if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE) {
//                    resourcePager.disableTouchEvents = true
//                }
                if (preferences.getBoolean(SCROLL_REF, false)) {
                    if (currentPagerPosition < position) {
                        // handle swipe LEFT
                        currentFragment?.webView?.scrollToStart()
                    } else if (currentPagerPosition > position) {
                        // handle swipe RIGHT
                        currentFragment?.webView?.scrollToEnd()
                    }
                } else {
                    if (currentPagerPosition < position) {
                        // handle swipe LEFT
                        currentFragment?.webView?.setCurrentItem(0, false)
                    } else if (currentPagerPosition > position) {
                        // handle swipe RIGHT
                        currentFragment?.webView?.apply {
                            setCurrentItem(numPages - 1, false)
                        }
                    }
                }
                currentPagerPosition = position // Update current position

                notifyCurrentLocation()
            }

        })

        // Restore the last locator before a configuration change (e.g. screen rotation), or the
        // initial locator when given.
        val locator = savedInstanceState?.getParcelable("locator") ?: initialLocator
        if (locator != null) {
            go(locator)
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("locator", currentLocator.value)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        notifyCurrentLocation()
    }

    /**
     * Locator waiting to be loaded in the navigator.
     */
    internal var pendingLocator: Locator? = null

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        pendingLocator = locator

        val href = locator.href
            // Remove anchor
            .substringBefore("#")

        fun setCurrent(resources: List<PageResource>) {
            val page = resources.withIndex().firstOrNull { (_, res) ->
                when (res) {
                    is PageResource.EpubReflowable -> res.link.href == href
                    is PageResource.EpubFxl -> res.url1.endsWith(href) || res.url2?.endsWith(href) == true
                    else -> false
                }
            } ?: return
            val (index, resource) = page

            if (resourcePager.currentItem != index) {
                resourcePager.currentItem = index
            } else if (resource is PageResource.EpubReflowable) {
                var url = resource.url
                locator.locations.htmlId?.let { htmlId ->
                    url += htmlId.addPrefix("#")
                }
                currentFragment?.webView?.loadUrl(url)
            }
        }

        resourcePager.adapter = adapter

        if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE) {
            setCurrent(resourcesSingle)
        } else {

            when (preferences.getInt(COLUMN_COUNT_REF, 0)) {
                1 -> {
                    setCurrent(resourcesSingle)
                }
                2 -> {
                    setCurrent(resourcesDouble)
                }
                else -> {
                    // TODO based on device
                    // TODO decide if 1 page or 2 page
                    setCurrent(resourcesSingle)
                }
            }
        }

        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        return go(link.toLocator(), animated, completion)
    }

    private fun run(commands: List<RunScriptCommand>) {
        commands.forEach { run(it) }
    }

    private fun run(command: RunScriptCommand) {
        when (command.scope) {
            RunScriptCommand.Scope.CurrentResource -> {
                currentFragment?.webView
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
        val webView = currentFragment?.webView ?: return null
        val json =
            webView.runJavaScriptSuspend("readium.getCurrentSelection();")
                .takeIf { it != "null"}
                ?.let { tryOrLog { JSONObject(it) } }
                ?: return null

        return Selection(
            locator = currentLocator.value.copy(
                text = Locator.Text.fromJSON(json.optJSONObject("text"))
            ),
            rect = json.optRectF("rect")
        )
    }

    override fun clearSelection() {
        run(viewModel.clearSelection())
    }

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

    override fun onResourceLoaded(link: Link?, webView: R2BasicWebView, url: String?) {
        run(viewModel.onResourceLoaded(link, webView))
    }

    override fun onPageLoaded() {
        r2Activity?.onPageLoaded()
        paginationListener?.onPageLoaded()
    }

    override fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {
        r2Activity?.onPageChanged(pageIndex = pageIndex, totalPages = totalPages, url = url)
        if(paginationListener != null) {
            // Find current locator
            val resource = publication.readingOrder[resourcePager.currentItem]
            val progression = currentFragment?.webView?.progression?.coerceIn(0.0, 1.0) ?: 0.0
            val positions = publication.positionsByResource[resource.href]?.takeIf { it.isNotEmpty() }
                    ?: return

            val positionIndex = ceil(progression * (positions.size - 1)).toInt()
            if (!positions.indices.contains(positionIndex)) {
                return
            }

            val locator = positions[positionIndex].copyWithLocations(progression = progression)
            // Pageindex is actually the page number so to get a zero based index we subtract one.
            paginationListener.onPageChanged(pageIndex - 1, totalPages, locator)
        }
    }

    override fun onPageEnded(end: Boolean) {
        r2Activity?.onPageEnded(end)
    }

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

    override fun onTap(point: PointF): Boolean {
        return this.listener?.onTap(point) ?: false
    }

    override fun onDecorationActivated(id: DecorationId, group: String, rect: RectF, point: PointF): Boolean {
        currentFragment?.paddingTop?.let { top ->
            rect.top += top
            point.y += top
        }
        return viewModel.onDecorationActivated(id, group, rect, point)
    }

    override fun onProgressionChanged() {
        notifyCurrentLocation()
    }

    override fun onHighlightActivated(id: String) {
        r2Activity?.highlightActivated(id)
    }

    override fun onHighlightAnnotationMarkActivated(id: String) {
        r2Activity?.highlightAnnotationMarkActivated(id)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        launch {
            if (resourcePager.currentItem < resourcePager.adapter!!.count - 1) {

                resourcePager.setCurrentItem(resourcePager.currentItem + 1, animated)

                if (publication.metadata.effectiveReadingProgression == ReadingProgression.RTL) {
                    // The view has RTL layout
                    currentFragment?.webView?.apply {
                        setCurrentItem(numPages - 1, false)
                    }
                } else {
                    // The view has LTR layout
                    currentFragment?.webView?.apply {
                        setCurrentItem(0, false)
                    }
                }
            }
        }
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        launch {
            if (resourcePager.currentItem > 0) {

                resourcePager.setCurrentItem(resourcePager.currentItem - 1, animated)

                if (publication.metadata.effectiveReadingProgression == ReadingProgression.RTL) {
                    // The view has RTL layout
                    currentFragment?.webView?.apply {
                        setCurrentItem(0, false)
                    }
                } else {
                    // The view has LTR layout
                    currentFragment?.webView?.apply {
                        setCurrentItem(numPages - 1, false)
                    }
                }
            }
        }
        return true
    }

    override val selectionActionModeCallback: ActionMode.Callback?
        get() = config.selectionActionModeCallback

    private val r2PagerAdapter: R2PagerAdapter?
        get() = if (::resourcePager.isInitialized) resourcePager.adapter as? R2PagerAdapter
            else null

    private val currentFragment: R2EpubPageFragment? get() =
        r2PagerAdapter?.let { adapter ->
            adapter.mFragments.get(adapter.getItemId(resourcePager.currentItem)) as? R2EpubPageFragment
        }

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
        get() = publication.metadata.effectiveReadingProgression

    override val currentLocator: StateFlow<Locator> get() = _currentLocator
    private val _currentLocator = MutableStateFlow(initialLocator ?: publication.readingOrder.first().toLocator())

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
        debounceLocationNotificationJob = launch {
            delay(100L)

            // The transition has stabilized, so we can ask the web view to refresh its current
            // item to reflect the current scroll position.
            currentFragment?.webView?.updateCurrentItem()

            val resource = publication.readingOrder[resourcePager.currentItem]
            val progression = currentFragment?.webView?.progression?.coerceIn(0.0, 1.0) ?: 0.0
            val positions = publication.positionsByResource[resource.href]?.takeIf { it.isNotEmpty() }
                    ?: return@launch

            val positionIndex = ceil(progression * (positions.size - 1)).toInt()
            if (!positions.indices.contains(positionIndex)) {
                return@launch
            }

            val locator = positions[positionIndex]
                    .copy(title = tableOfContentsTitleByHref[resource.href])
                    .copyWithLocations(progression = progression)

            if (locator == _currentLocator.value) {
                return@launch
            }

            _currentLocator.value = locator
            navigatorDelegate?.locationDidChange(navigator = navigator, locator = locator)
        }
    }

    companion object {

        /**
         * Creates a factory for [EpubNavigatorFragment].
         *
         * @param publication EPUB publication to render in the navigator.
         * @param baseUrl A base URL where this publication is served from.
         * @param initialLocator The first location which should be visible when rendering the
         *        publication. Can be used to restore the last reading location.
         * @param listener Optional listener to implement to observe events, such as user taps.
         * @param config Additional configuration.
         */
        fun createFactory(
            publication: Publication,
            baseUrl: String,
            initialLocator: Locator? = null,
            listener: Listener? = null,
            paginationListener: PaginationListener? = null,
            config: Configuration = Configuration(),
        ): FragmentFactory =
            createFragmentFactory { EpubNavigatorFragment(publication, baseUrl, initialLocator, listener, paginationListener, config) }

    }

}
