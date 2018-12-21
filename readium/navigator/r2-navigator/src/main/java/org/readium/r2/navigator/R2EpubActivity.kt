/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import org.json.JSONObject
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.pager.PageCallback
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.*
import java.net.URI


open class R2EpubActivity : AppCompatActivity(), PageCallback {

    lateinit var preferences: SharedPreferences
    lateinit var resourcePager: R2ViewPager
    lateinit var resourcesSingle: ArrayList<Pair<Int, String>>
    lateinit var resourcesDouble: ArrayList<Triple<Int, String, String>>

    private lateinit var publicationPath: String
    protected lateinit var epubName: String
    lateinit var publication: Publication
    lateinit var publicationIdentifier: String

    var pagerPosition = 0

    private var currentPagerPosition: Int = 0
    lateinit var adapter:R2PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)
        resourcesSingle = ArrayList()
        resourcesDouble = ArrayList()

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        epubName = intent.getStringExtra("epubName")
        publicationIdentifier = publication.metadata.identifier

        title = publication.metadata.title

        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()

        // TODO needs work, currently showing two resources for fxl, needs to understand which two resources, left & right, or only right etc.
        var doublePageIndex = 0
        var doublePageLeft: String = ""
        var doublePageRight: String = ""
        var resourceIndexDouble = 0
        var resourceIndexSingle = 0

        for (spineItem in publication.spine) {
            var uri: String
            if (URI(publicationPath).isAbsolute) {
                if (URI(spineItem.href).isAbsolute) {
                    uri = spineItem.href!!
                } else {
                    uri = publicationPath + spineItem.href
                }
            } else {
                uri = "$BASE_URL:$port" + "/" + epubName + spineItem.href
            }
            resourcesSingle.add(Pair(resourceIndexSingle, uri))
            resourceIndexSingle++

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
            when (preferences.getInt(COLUMN_COUNT_REF, 0)) {
                1 -> {
                    adapter = R2PagerAdapter(supportFragmentManager, resourcesSingle, publication.metadata.title, Publication.TYPE.FXL, publicationPath)
                }
                2 -> {
                    adapter = R2PagerAdapter(supportFragmentManager, resourcesDouble, publication.metadata.title, Publication.TYPE.FXL, publicationPath)
                }
                else -> {
                    // TODO based on device
                    // TODO decide if 1 page or 2 page
                    adapter = R2PagerAdapter(supportFragmentManager, resourcesSingle, publication.metadata.title, Publication.TYPE.FXL, publicationPath)
                }
            }
        }
        resourcePager.adapter = adapter

        resourcePager.direction = publication.metadata.direction

        val index = preferences.getInt("$publicationIdentifier-document", 0)
        resourcePager.currentItem = index
        currentPagerPosition = index


        resourcePager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
                // Do nothing
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // Do nothing
            }

            override fun onPageSelected(position: Int) {
                resourcePager.disableTouchEvents = true
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
                storeDocumentIndex()
                currentPagerPosition = position; // Update current position
            }

        });

        storeDocumentIndex()

    }
    
    /**
     * storeProgression() : save in the preference the last progression in the spine item
     */
    fun storeProgression(locations: Locations) {
        storeDocumentIndex()
        val publicationIdentifier = publication.metadata.identifier
        preferences.edit().putString("$publicationIdentifier-documentLocations", locations.toJSON().toString()).apply()
    }

    /**
     * storeDocumentIndex() : save in the preference the last spine item
     */
    fun storeDocumentIndex() {
        val documentIndex = resourcePager.currentItem
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {

                pagerPosition = 0

                val locator = data.getSerializableExtra("locator") as Locator

                // Set the progression fetched
                storeProgression(locator.locations)

                // href is the link to the page in the toc
                var href = locator.href

                if (href.indexOf("#") > 0) {
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
                                    locator.locations.id?.let {
                                        var anchor = it
                                        if (anchor.startsWith("#")) {
                                        } else {
                                            anchor = "#" + anchor
                                        }
                                        val goto = resource.second +  anchor
                                        currentFragent?.webView?.loadUrl(goto)
                                    }?:run {
                                        currentFragent?.webView?.loadUrl(resource.second)
                                    }
                                } else {
                                    resourcePager.currentItem = resource.first
                                }
                                storeDocumentIndex()
                                break
                            }
                        } else {
                            resource as Triple<Int, String, String>
                            if (resource.second.endsWith(href) || resource.third.endsWith(href)) {
                                resourcePager.currentItem = resource.first
                                storeDocumentIndex()
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

                if (supportActionBar!!.isShowing) {
                    resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            or View.SYSTEM_UI_FLAG_IMMERSIVE)
                }
            }
        }
    }


    fun nextResource(smoothScroll: Boolean) {
        runOnUiThread {
            pagerPosition = 0
            if (resourcePager.currentItem < resourcePager.adapter!!.count - 1 ) {

                resourcePager.setCurrentItem(resourcePager.currentItem + 1, smoothScroll)

                val currentFragent = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

                if (layoutDirectionIsRTL() || publication.metadata.direction == PageProgressionDirection.rtl.name) {
                    // The view has RTL layout
                    currentFragent?.webView?.let {
                        currentFragent.webView.progression = 1.0
                        currentFragent.webView.setCurrentItem(currentFragent.webView.numPages - 1,false)
                    }
                } else {
                    // The view has LTR layout
                    currentFragent?.webView?.let {
                        currentFragent.webView.progression = 0.0
                        currentFragent.webView.setCurrentItem(0,false)
                    }
                }
                storeDocumentIndex()
            }
        }
    }

    fun previousResource(smoothScroll: Boolean) {
        runOnUiThread {
            pagerPosition = 0
            if (resourcePager.currentItem > 0) {

                resourcePager.setCurrentItem(resourcePager.currentItem - 1, smoothScroll)

                val currentFragent = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

                if (layoutDirectionIsRTL() || publication.metadata.direction == PageProgressionDirection.rtl.name) {
                    // The view has RTL layout
                    currentFragent?.webView?.let {
                        currentFragent.webView.progression = 0.0
                        currentFragent.webView.setCurrentItem(0,false)
                    }
                } else {
                    // The view has LTR layout
                    currentFragent?.webView?.let {
                        currentFragent.webView.progression = 1.0
                        currentFragent.webView.setCurrentItem(currentFragent.webView.numPages - 1,false)
                    }
                }
                storeDocumentIndex()
            }
        }
    }


    fun toggleActionBar() {
        runOnUiThread {
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

    override fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {
        //optional
    }

    override fun onPageEnded(end: Boolean) {
        //optional
    }

}

