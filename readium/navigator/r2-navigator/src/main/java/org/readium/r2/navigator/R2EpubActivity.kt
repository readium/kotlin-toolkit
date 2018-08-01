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
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_page_epub.view.*
import org.jetbrains.anko.contentView
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRMMModel


open class R2EpubActivity : AppCompatActivity() {

    lateinit var preferences: SharedPreferences
    lateinit var resourcePager: R2ViewPager
    lateinit var resources: ArrayList<String>

    protected lateinit var publicationPath: String
    protected lateinit var publication: Publication
    protected lateinit var epubName: String
    lateinit var publicationIdentifier: String

    lateinit var userSettings: UserSettings
    protected var drmModel: DRMMModel? = null
    protected var menuDrm: MenuItem? = null
    protected var menuToc: MenuItem? = null

    var pagerPosition = 0
    var relaodPagerPositions = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)
        resources = ArrayList()

        Handler().postDelayed({
            if (intent.getSerializableExtra("drmModel") != null) {
                drmModel = intent.getSerializableExtra("drmModel") as DRMMModel
                drmModel?.let {
                    runOnUiThread {
                        menuDrm?.isVisible = true
                    }
                } ?: run {
                    runOnUiThread {
                        menuDrm?.isVisible = false
                    }
                }
            }
        }, 100)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        epubName = intent.getStringExtra("epubName")
        publicationIdentifier = publication.metadata.identifier

        title = publication.metadata.title

        val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()

        for (spine in publication.spine) {
            val uri = "$BASE_URL:$port" + "/" + epubName + spine.href
            resources.add(uri)
        }

        val index = preferences.getInt("$publicationIdentifier-document", 0)

        val adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title, Publication.TYPE.EPUB, publicationPath)
        relaodPagerPositions = true
        resourcePager.adapter = adapter

        userSettings = UserSettings(preferences, this)
        userSettings.resourcePager = resourcePager

        if (index == 0) {
            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.currentItem = resources.size - 1
            } else {
                // The view has LTR layout
            }
        } else {
            resourcePager.currentItem = index
        }

        val appearancePref = preferences.getInt("appearance", 0)
        val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
        val textColors = mutableListOf("#000000", "#000000", "#ffffff")
        resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        (resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor(textColors[appearancePref]))
        toggleActionBar()
    }

    override fun onPause() {
        super.onPause()
        storeProgression(resourcePager.webView.progression)
    }

    /**
     * storeProgression : save in the preference the last progression
     */
    fun storeProgression(progression:Double) {
        val publicationIdentifier = publication.metadata.identifier
        val documentIndex = resourcePager.currentItem
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
        preferences.edit().putString("$publicationIdentifier-documentProgression", progression.toString()).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {

                pagerPosition = 0
                relaodPagerPositions = true

                val adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title, Publication.TYPE.EPUB, publicationPath)
                resourcePager.adapter = adapter

                // href is the link to the page in the toc
                var href: String = data.getStringExtra("toc_item_uri")

                // Fetching the last progression saved ( default : 0.0 )
                val progression = data.getDoubleExtra("item_progression", 0.0)

                // Set the progression fetched
                preferences.edit().putString("$publicationIdentifier-documentProgression", progression.toString()).apply()

                if (href.indexOf("#") > 0) {
                    href = href.substring(0, href.indexOf("#"))
                }
                // Search corresponding href in the spine
                for (i in 0 until publication.spine.size) {
                    if (publication.spine[i].href == href) {
                        resourcePager.currentItem = i
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


    fun nextResource() {
        runOnUiThread {
            pagerPosition = 0
            resourcePager.webView.progression = 0.0

            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            }
        }
    }

    fun previousResource() {
        runOnUiThread {
            pagerPosition = 0
            resourcePager.webView.progression = 1.0

            if (ViewCompat.getLayoutDirection(this.contentView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem - 1
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
}

