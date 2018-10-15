/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.navigator.CreateSyntheticPageList
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Locations
import org.readium.r2.shared.LocatorText

/**
 * R2EpubActivity : Extension of the R2EpubActivity() from navigator
 *
 * That Activity manage everything related to the menu
 *      ( Table of content, User Settings, Drm, Bookmarks )
 *
 */
class R2EpubActivity : R2EpubActivity() {

    // List of bookmarks on activity_outline_container.xml
    private var menuBmk: MenuItem? = null

    // Provide access to the Bookmarks & Positions Databases
    private lateinit var bookmarksDB: BookmarksDatabase
    private lateinit var positionsDB: PositionsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarksDB = BookmarksDatabase(this)
        positionsDB = PositionsDatabase(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(org.readium.r2.testapp.R.menu.menu_epub, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)
        menuDrm?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val bookId = intent.getLongExtra("bookId", -1)
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.toc), 0, 0, Gravity.END)
                return true
            }
            R.id.drm -> {
                startActivityForResult(intentFor<DRMManagementActivity>("drmModel" to drmModel), 1)
                return true
            }
            R.id.bookmark -> {
                val bookId = intent.getLongExtra("bookId", -1)
                val resourceIndex = resourcePager.currentItem.toLong()
                val resourceHref = publication.spine[resourcePager.currentItem].href!!
                val resourceTitle = publication.spine[resourcePager.currentItem].title?: ""
                val progression = preferences.getString("$publicationIdentifier-documentProgression", 0.toString()).toDouble()
                val currentPage = positionsDB.positions.getCurrentPage(publicationIdentifier, resourceHref, progression)

                val bookmark = Bookmark(
                        bookId,
                        publicationIdentifier,
                        resourceIndex,
                        resourceHref,
                        resourceTitle,
                        Locations(progression = progression, position = currentPage),
                        LocatorText()
                )
                
                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    runOnUiThread {
                        toast("Bookmark added at page $currentPage")
                    }
                } ?:run {
                    runOnUiThread {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }

            else -> return false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getBooleanExtra("returned", false)) {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    override fun onResume() {
        super.onResume()

        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_book))

        Handler().postDelayed({
            if (publication.pageList.isEmpty() && !(positionsDB.positions.has(publicationIdentifier))) {

                val syntheticPageList = CreateSyntheticPageList()

                /*
                 * Creation of the page list (retrieving resource's URLs first, then execute async task
                 * that runs through resource content to count pages of 1024 characters each)
                 */
                val resourcesHref = mutableListOf<String>()

                for (spineItem in publication.spine) {
                    resourcesHref.add(spineItem.href!!)
                }
                val list = syntheticPageList.execute(Triple("$BASE_URL:$port/", epubName, resourcesHref)).get()

                /*
                 * Storing the generated page list in the DB
                 */
                positionsDB.positions.storeSyntheticPageList(publicationIdentifier, list)
            }
            progress.dismiss()
        }, 200)
    }


}