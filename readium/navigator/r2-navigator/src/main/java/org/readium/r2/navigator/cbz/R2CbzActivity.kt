/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.cbz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.navigator.IR2Activity
import org.readium.r2.navigator.ReadingProgression
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Link
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication
import kotlin.coroutines.CoroutineContext


open class R2CbzActivity : AppCompatActivity(), CoroutineScope, IR2Activity {
    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override lateinit var preferences: SharedPreferences
    override lateinit var resourcePager: R2ViewPager
    override lateinit var publicationPath: String
    override lateinit var publication: Publication
    override lateinit var publicationFileName: String
    override lateinit var publicationIdentifier: String

    var resources = arrayListOf<String>()
    lateinit var adapter: R2PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        publicationFileName = intent.getStringExtra("publicationFileName")
        publicationIdentifier = publication.metadata.identifier!!
        title = publication.metadata.title

        for (link in publication.images) {
            resources.add(link.href.toString())
        }

        val index = preferences.getInt("$publicationIdentifier-document", 0)

        adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title, Publication.TYPE.CBZ, publicationPath)

        resourcePager.adapter = adapter

        if (index == 0) {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resources.size - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = index
            }
        } else {
            resourcePager.currentItem = index
        }

    }

    override fun onPause() {
        super.onPause()
        storeDocumentIndex()
    }

    override fun nextResource(v: View?) {
        launch {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            }
            storeDocumentIndex()
        }
    }

    override fun previousResource(v: View?) {
        launch {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            }
            storeDocumentIndex()
        }
    }

    /**
     * storeProgression() : save in the preference the last progression in the spine item
     */
    fun storeProgression(locations: Locations?) {
        storeDocumentIndex()
        val publicationIdentifier = publication.metadata.identifier
        preferences.edit().putString("$publicationIdentifier-documentLocations", locations?.toJSON().toString()).apply()
    }

    /**
     * storeDocumentIndex() : save in the preference the last spine item
     */
    private fun storeDocumentIndex() {
        val documentIndex = resourcePager.currentItem
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
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

    override fun toggleActionBar(v: View?) {
        toggleActionBar()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {

                val locator = data.getSerializableExtra("locator") as Locator

                // Set the progression fetched
                storeProgression(locator.locations)

                fun setCurrent(resources: ArrayList<*>) {
                    for (index in 0 until resources.count()) {
                        val resource = resources[index] as String
                        if (resource.endsWith(locator.href!!)) {
                            resourcePager.currentItem = index
                            break
                        }
                    }
                }

                resourcePager.adapter = adapter

                setCurrent(resources)

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

}

