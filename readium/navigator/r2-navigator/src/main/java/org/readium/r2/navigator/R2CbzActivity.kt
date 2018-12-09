/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Publication


class R2CbzActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    lateinit var resourcePager: R2ViewPager
    var resources = arrayListOf<String>()

    private lateinit var publicationPath: String
    private lateinit var publication: Publication
    private lateinit var cbzName: String
    private lateinit var publicationIdentifier: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        cbzName = intent.getStringExtra("cbzName")
        publicationIdentifier = publication.metadata.identifier
        title = publication.metadata.title

        for (link in publication.pageList) {
            resources.add(link.href.toString())
        }

        val index = preferences.getInt("$publicationIdentifier-document", 0)

        val adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title, Publication.TYPE.CBZ, publicationPath)

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

        toggleActionBar()
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        // TODO we could add a thumbnail view here
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return super.onOptionsItemSelected(item)
//    }

    override fun onPause() {
        super.onPause()
        val publicationIdentifier = publication.metadata.identifier
        val documentIndex = resourcePager.currentItem
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
    }

    fun nextResource(v: View? = null) {
        runOnUiThread {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            }
        }
    }

    fun previousResource(v: View? = null) {
        runOnUiThread {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            }

        }
    }

    fun toggleActionBar(v: View? = null) {
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

