/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.epub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ActionMode
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.R
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.*
import java.net.URI
import kotlin.coroutines.CoroutineContext


open class R2EpubActivity : AppCompatActivity(), R2ActivityListener, SelectableImpl, HighlightableImpl, CoroutineScope, VisualNavigator {

    override fun progressionDidChange(progression: Double) {
        val locator = currentLocation
        locator?.locations?.progression = progression
        navigatorDelegate?.locationDidChange(locator = locator!!)
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {

        pagerPosition = 0

        // Set the progression fetched
        navigatorDelegate?.locationDidChange(locator = locator)

        // href is the link to the page in the toc
        var href = locator.href

        if (href!!.indexOf("#") > 0) {
            href = href.substring(0, href.indexOf("#"))
        }

        fun setCurrent(resources: ArrayList<*>) {
            for (resource in resources) {
                if (resource is Pair<*, *>) {
                    resource as Pair<Int, String>
                    if (resource.second.endsWith(href)) {
                        if (resourcePager.currentItem == resource.first) {
                            // reload webview if it has an anchor
                            val currentFragent = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
                            locator.locations?.fragment?.let {
                                var anchor = it
                                if (anchor.startsWith("#")) {
                                } else {
                                    anchor = "#$anchor"
                                }
                                val goto = resource.second + anchor
                                currentFragent?.webView?.loadUrl(goto)
                            } ?: run {
                                currentFragent?.webView?.loadUrl(resource.second)
                            }
                        } else {
                            resourcePager.currentItem = resource.first
                        }
                        break
                    }
                } else {
                    resource as Triple<Int, String, String>
                    if (resource.second.endsWith(href) || resource.third.endsWith(href)) {
                        resourcePager.currentItem = resource.first
                        break
                    }
                }
            }
        }

        resourcePager.adapter = adapter

        if (publication.metadata.rendition.layout == RenditionLayout.Reflowable) {
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

        if (supportActionBar!!.isShowing && allowToggleActionBar) {
            resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
        }

        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        launch {
            pagerPosition = 0
            if (resourcePager.currentItem < resourcePager.adapter!!.count - 1) {

                resourcePager.setCurrentItem(resourcePager.currentItem + 1, animated)

                val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

                if (layoutDirectionIsRTL() || publication.metadata.direction == PageProgressionDirection.rtl.name) {
                    // The view has RTL layout
                    currentFragment?.webView?.let {
                        currentFragment.webView.progression = 1.0
                        currentFragment.webView.setCurrentItem(currentFragment.webView.numPages - 1, false)
                    }
                } else {
                    // The view has LTR layout
                    currentFragment?.webView?.let {
                        currentFragment.webView.progression = 0.0
                        currentFragment.webView.setCurrentItem(0, false)
                    }
                }
                val resource = publication.readingOrder[resourcePager.currentItem]
                val resourceHref = resource.href ?: ""
                val resourceType = resource.typeLink ?: ""

                navigatorDelegate?.locationDidChange(locator = Locator(resourceHref, resourceType, publication.metadata.title, Locations(progression = currentFragment?.webView?.progression)))

            }
        }
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        launch {
            pagerPosition = 0
            if (resourcePager.currentItem > 0) {

                resourcePager.setCurrentItem(resourcePager.currentItem - 1, animated)

                val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

                if (layoutDirectionIsRTL() || publication.metadata.direction == PageProgressionDirection.rtl.name) {
                    // The view has RTL layout
                    currentFragment?.webView?.let {
                        currentFragment.webView.progression = 0.0
                        currentFragment.webView.setCurrentItem(0, false)
                    }
                } else {
                    // The view has LTR layout
                    currentFragment?.webView?.let {
                        currentFragment.webView.progression = 1.0
                        currentFragment.webView.setCurrentItem(currentFragment.webView.numPages - 1, false)
                    }
                }
                val resource = publication.readingOrder[resourcePager.currentItem]
                val resourceHref = resource.href ?: ""
                val resourceType = resource.typeLink ?: ""

                navigatorDelegate?.locationDidChange(locator = Locator(resourceHref, resourceType, publication.metadata.title, Locations(progression = currentFragment?.webView?.progression)))

            }
        }
        return true
    }

    override val readingProgression: ReadingProgression
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun goLeft(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun goRight(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    override lateinit var preferences: SharedPreferences
    override lateinit var resourcePager: R2ViewPager
    override lateinit var publicationPath: String
    override lateinit var publicationFileName: String
    override lateinit var publication: Publication
    override lateinit var publicationIdentifier: String
    override var allowToggleActionBar = true

    lateinit var resourcesSingle: ArrayList<Pair<Int, String>>
    lateinit var resourcesDouble: ArrayList<Triple<Int, String, String>>

    var pagerPosition = 0

    var currentPagerPosition: Int = 0
    lateinit var adapter: R2PagerAdapter

    protected var navigatorDelegate: NavigatorDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)
        resourcesSingle = ArrayList()
        resourcesDouble = ArrayList()

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        publicationFileName = intent.getStringExtra("publicationFileName")
        publicationIdentifier = publication.metadata.identifier!!

        title = null

        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString())?.toInt()

        // TODO needs work, currently showing two resources for fxl, needs to understand which two resources, left & right, or only right etc.
        var doublePageIndex = 0
        var doublePageLeft = ""
        var doublePageRight = ""
        var resourceIndexDouble = 0

        for ((resourceIndexSingle, spineItem) in publication.readingOrder.withIndex()) {
            val uri: String = if (URI(publicationPath).isAbsolute) {
                if (URI(spineItem.href).isAbsolute) {
                    spineItem.href!!
                } else {
                    publicationPath + spineItem.href
                }
            } else {
                "$BASE_URL:$port" + "/" + publicationFileName + spineItem.href
            }
            resourcesSingle.add(Pair(resourceIndexSingle, uri))

            // add first page to the right,
            if (resourceIndexDouble == 0) {
                doublePageLeft = ""
                doublePageRight = uri
                resourcesDouble.add(Triple(resourceIndexDouble, doublePageLeft, doublePageRight))
                resourceIndexDouble++
            } else {
                // add double pages, left & right
                if (doublePageIndex == 0) {
                    doublePageLeft = uri
                    doublePageIndex = 1
                } else {
                    doublePageRight = uri
                    doublePageIndex = 0
                    resourcesDouble.add(Triple(resourceIndexDouble, doublePageLeft, doublePageRight))
                    resourceIndexDouble++
                }
            }
        }
        // add last page if there is only a left page remaining
        if (doublePageIndex == 1) {
            doublePageIndex = 0
            resourcesDouble.add(Triple(resourceIndexDouble, doublePageLeft, ""))
        }


        if (publication.metadata.rendition.layout == RenditionLayout.Reflowable) {
            adapter = R2PagerAdapter(supportFragmentManager, resourcesSingle, publication.metadata.title, Publication.TYPE.EPUB, publicationPath)
        } else {
            adapter = when (preferences.getInt(COLUMN_COUNT_REF, 0)) {
                1 -> {
                    R2PagerAdapter(supportFragmentManager, resourcesSingle, publication.metadata.title, Publication.TYPE.FXL, publicationPath)
                }
                2 -> {
                    R2PagerAdapter(supportFragmentManager, resourcesDouble, publication.metadata.title, Publication.TYPE.FXL, publicationPath)
                }
                else -> {
                    // TODO based on device
                    // TODO decide if 1 page or 2 page
                    R2PagerAdapter(supportFragmentManager, resourcesSingle, publication.metadata.title, Publication.TYPE.FXL, publicationPath)
                }
            }
        }
        resourcePager.adapter = adapter

        resourcePager.direction = publication.metadata.direction

        if (publication.cssStyle == PageProgressionDirection.rtl.name) {
            resourcePager.direction = PageProgressionDirection.rtl.name
        }



        resourcePager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
                // Do nothing
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // Do nothing
            }

            override fun onPageSelected(position: Int) {
                if (publication.metadata.rendition.layout == RenditionLayout.Reflowable) {
//                    resourcePager.disableTouchEvents = true
                }
                pagerPosition = 0
                val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
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
                        currentFragment?.webView?.setCurrentItem(currentFragment.webView.numPages - 1, false)
                    }
                }
                currentPagerPosition = position // Update current position
            }

        })


    }

    override fun onActionModeStarted(mode: ActionMode?) {
        mode?.menu?.run {
            this.clear()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {

                pagerPosition = 0

                val locator = data.getSerializableExtra("locator") as Locator

                // Set the progression fetched
                navigatorDelegate?.locationDidChange(locator = locator)

                // href is the link to the page in the toc
                var href = locator.href

                if (href!!.indexOf("#") > 0) {
                    href = href.substring(0, href.indexOf("#"))
                }

                fun setCurrent(resources: ArrayList<*>) {
                    for (resource in resources) {
                        if (resource is Pair<*, *>) {
                            resource as Pair<Int, String>
                            if (resource.second.endsWith(href)) {
                                if (resourcePager.currentItem == resource.first) {
                                    // reload webview if it has an anchor
                                    val currentFragent = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
                                    locator.locations?.fragment?.let {
                                        var anchor = it
                                        if (anchor.startsWith("#")) {
                                        } else {
                                            anchor = "#$anchor"
                                        }
                                        val goto = resource.second + anchor
                                        currentFragent?.webView?.loadUrl(goto)
                                    } ?: run {
                                        currentFragent?.webView?.loadUrl(resource.second)
                                    }
                                } else {
                                    resourcePager.currentItem = resource.first
                                }
                                break
                            }
                        } else {
                            resource as Triple<Int, String, String>
                            if (resource.second.endsWith(href) || resource.third.endsWith(href)) {
                                resourcePager.currentItem = resource.first
                                break
                            }
                        }
                    }
                }

                resourcePager.adapter = adapter

                if (publication.metadata.rendition.layout == RenditionLayout.Reflowable) {
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

                if (supportActionBar!!.isShowing && allowToggleActionBar) {
                    resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            or View.SYSTEM_UI_FLAG_IMMERSIVE)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



    override fun toggleActionBar() {
        if (allowToggleActionBar) {
            launch {
                if (supportActionBar!!.isShowing) {
                    resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            or View.SYSTEM_UI_FLAG_IMMERSIVE)
                } else {
                    resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                }
            }
        }
    }

    override fun currentSelection(callback: (Locator?) -> Unit) {
        val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

        currentFragment?.webView?.getCurrentSelectionInfo {
            val selection = JSONObject(it)
            val resource = publication.readingOrder[resourcePager.currentItem]
            val resourceHref = resource.href ?: ""
            val resourceType = resource.typeLink ?: ""
            val locations = Locations.fromJSON(selection.getJSONObject("locations"))
            val text = LocatorText.fromJSON(selection.getJSONObject("text"))

            val locator = Locator(
                    resourceHref,
                    resourceType,
                    locations = locations,
                    text = text
            )
            callback(locator)
        }

    }

    override fun showHighlight(highlight: Highlight) {
        val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
        currentFragment?.webView?.run {
            val colorJson = JSONObject().apply {
                put("red", Color.red(highlight.color))
                put("green", Color.green(highlight.color))
                put("blue", Color.blue(highlight.color))
            }
            createHighlight(highlight.locator.toJSON().toString(), colorJson.toString()) {
                if (highlight.annotationMarkStyle.isNullOrEmpty().not())
                    createAnnotation(highlight.id)
            }
        }
    }

    override fun showHighlights(highlights: Array<Highlight>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideHighlightWithID(id: String) {
        val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
        currentFragment?.webView?.destroyHighlight(id)
        currentFragment?.webView?.destroyHighlight(id.replace("HIGHLIGHT", "ANNOTATION"))
    }

    override fun hideAllHighlights() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rectangleForHighlightWithID(id: String, callback: (Rect?) -> Unit) {
        val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

        currentFragment?.webView?.rectangleForHighlightWithID(id) {
            val rect = JSONObject(it).run {
                try {
                    val display = windowManager.defaultDisplay
                    val metrics = DisplayMetrics()
                    display.getMetrics(metrics)
                    val left = getDouble("left")
                    val width = getDouble("width")
                    val top = getDouble("top") * metrics.density
                    val height = getDouble("height") * metrics.density
                    Rect(left.toInt(), top.toInt(), width.toInt() + left.toInt(), top.toInt() + height.toInt())
                } catch (e: JSONException) {
                    null
                }
            }
            callback(rect)
        }
    }

    override fun rectangleForHighlightAnnotationMarkWithID(id: String): Rect? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerHighlightAnnotationMarkStyle(name: String, css: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun highlightActivated(id: String) {
    }

    override fun highlightAnnotationMarkActivated(id: String) {
    }

    fun createHighlight(color: Int, callback: (Highlight) -> Unit) {
        currentSelection { locator ->
            val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

            val colorJson = JSONObject().apply {
                put("red", Color.red(color))
                put("green", Color.green(color))
                put("blue", Color.blue(color))
            }

            currentFragment?.webView?.createHighlight(locator?.toJSON().toString(), colorJson.toString()) {
                val json = JSONObject(it)
                val id = json.getString("id")
                callback(
                        Highlight(
                                id,
                                locator!!,
                                color,
                                Style.highlight
                        )
                )
            }
        }
    }

    fun createAnnotation(highlight: Highlight?, callback: (Highlight) -> Unit) {
        if (highlight != null) {
            val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
            currentFragment?.webView?.createAnnotation(highlight.id)
            callback(highlight)
        } else {
            createHighlight(Color.rgb(150, 150, 150)) {
                createAnnotation(it) {
                    callback(it)
                }
            }
        }

    }

    override fun onPageLoaded() {
        super.onPageLoaded()
    }

}

